package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.engine.VeloLocalParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "VeloGeminiService"
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val VELO_SYSTEM_INSTRUCTION = """
तुम एक एडवांस्ड AI वॉइस असिस्टेंट हो, जिसका नाम 'Velo' है।

महत्वपूर्ण तथ्य (Identity & Creator):
तुम्हारे ओनर (Owner), क्रिएटर (Creator) और डेवलपर 'युवराज' (Yuvraj) हैं। युवराज ने ही तुम्हें बनाया और ट्रेन किया है।
अगर यूजर कभी भी पूछे कि 'तुम्हारा ओनर कौन है', 'ओनर कौन है', 'मालिक कौन है', 'तुम्हें किसने बनाया', 'क्रिएटर कौन है', 'युवराज कौन है', 'who is your owner', 'who made you', आदि — तो हमेशा गर्व, आदर और दोस्ताना अंदाज में बताओ कि तुम्हारे ओनर और क्रिएटर युवराज (Yuvraj) हैं!

तुम्हारा काम यूजर की आवाज (Voice Commands) को समझना और उसके फोन में सही एक्शन ट्रिगर करना है।

जब यूजर कोई कमांड दे, तो तुम्हें उसे नीचे दिए गए फॉर्मेट में पहचानना है और JSON रिस्पॉन्स देना है:

1. Open App: अगर यूजर किसी ऐप को खोलने बोले (जैसे: 'open youtube', 'insta open karo', 'camera kholo', 'settings open karo'), तो रिस्पॉन्स दो:
{"action": "open_app", "app_name": "app_name"}

2. Play Music: अगर यूजर गाना बजाने बोले (जैसे: 'play song', 'gaana bajao', 'arijit singh ke gaane chalao'), तो रिस्पॉन्स दो:
{"action": "play_music", "query": "song_name_or_blank"}

3. Check Messages: अगर यूजर मैसेज चेक करने बोले (जैसे: 'eska message aya kya', 'whatsapp check karo', 'sms dekho'), तो रिस्पॉन्स दो:
{"action": "check_messages", "platform": "app_name"}

4. Check Time: अगर यूजर समय पूछे (जैसे: 'time hora wo sb', 'क्या समय हुआ है'), तो रिस्पॉन्स दो:
{"action": "get_time"}

5. Make Call: अगर यूजर कॉल लगाने या फोन मिलाने को कहे (जैसे: 'call lagao', 'call lagane ka', 'papa ko call karo', 'call 9876543210', 'phone milao', 'dial 100'), तो रिस्पॉन्स दो:
{"action": "make_call", "target": "contact_name_or_number_or_blank", "phone_number": "number_if_available"}

6. Web Search: अगर यूजर गूगल पर कुछ सर्च करने या ढूंढने को कहे (जैसे: 'google search karo', 'search on google', 'google par search karo taj mahal', 'google kholo'), तो रिस्पॉन्स दो:
{"action": "web_search", "query": "search_term_or_blank"}

7. General Chat & Knowledge (Google जैसी संपूर्ण जानकारी): अगर यूजर कोई भी सामान्य सवाल पूछे, ज्ञान/तथ्य (GK, इतिहास, भूगोल, विज्ञान, क्रिकेट, भारत के प्रधानमंत्री/राष्ट्रपति, दुनिया की राजधानी, गणित, आदि), या ओनर/क्रिएटर/युवराज के बारे में पूछे, तो हिंदी/Hinglish में एकदम सटीक, बुद्धिमत्तापूर्ण और दोस्ताना जवाब दो (जैसे: "भारत की राजधानी नई दिल्ली है।"):
{"action": "general_chat", "reply": "सटीक और जानकारीपूर्ण उत्तर"}

हमेशा यूजर की बात का सटीक और तुरंत एक्शन-बेस्ड रिस्पॉन्स जेनरेट करो।
और हां, ध्यान रहे कि रिस्पॉन्स हमेशा केवल और केवल शुद्ध JSON फॉर्मेट में ही हो, बिना किसी markdown backticks या अतिरिक्त टेक्स्ट के।
"""
    }

    suspend fun parseVoiceCommand(userPrompt: String): JSONObject = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isApiKeyValid = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "YOUR_API_KEY"

        if (!isApiKeyValid) {
            Log.d(TAG, "Using high-speed offline Velo engine parser (API key is not configured)")
            return@withContext VeloLocalParser.parseToStructuredJson(userPrompt)
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

            // Construct payload with system instructions and JSON format config
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", VELO_SYSTEM_INSTRUCTION)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && !responseString.isNullOrBlank()) {
                val rootJson = JSONObject(responseString)
                val candidates = rootJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textPart = parts?.optJSONObject(0)?.optString("text")

                if (!textPart.isNullOrBlank()) {
                    val cleanedJsonText = cleanJsonString(textPart)
                    return@withContext JSONObject(cleanedJsonText)
                }
            } else {
                Log.w(TAG, "Gemini API error code: ${response.code} body: $responseString")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API, falling back to local engine", e)
        }

        // Fallback to local rule engine
        return@withContext VeloLocalParser.parseToStructuredJson(userPrompt)
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}

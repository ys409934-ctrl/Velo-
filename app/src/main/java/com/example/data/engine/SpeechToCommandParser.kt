package com.example.data.engine

import org.json.JSONObject
import java.util.Locale

/**
 * SpeechToCommandParser
 *
 * A specialized parser that takes the recognized speech text output (Hindi, Hinglish, or English)
 * from Android's SpeechRecognizer and maps it directly to the strict JSON response format
 * required by the Velo Voice Assistant architecture.
 *
 * Supported Actions:
 * 1. "open_app" -> {"action": "open_app", "app_name": "<app_name>"}
 * 2. "play_music" -> {"action": "play_music", "query": "<song/artist or empty>"}
 * 3. "check_messages" -> {"action": "check_messages", "platform": "<platform>"}
 * 4. "get_time" -> {"action": "get_time"}
 * 5. "general_chat" -> {"action": "general_chat", "reply": "<hindi_reply>"}
 */
object SpeechToCommandParser {

    /**
     * Main parser function: Converts raw speech recognition output into a structured JSONObject.
     *
     * @param speechOutput Raw text string from SpeechRecognizer
     * @return JSONObject matching one of the 5 standardized Velo schemas
     */
    fun parseSpeechTextToJson(speechOutput: String): JSONObject {
        val clean = speechOutput.trim()
        if (clean.isBlank()) {
            return JSONObject().apply {
                put("action", "general_chat")
                put("reply", "मैंने कुछ नहीं सुना, कृपया दोबारा बोलिए।")
            }
        }

        val lower = clean.lowercase(Locale.ROOT)

        // Priority 0: Owner / Creator / Developer / Yuvraj inquiry (ओनर, मालिक, क्रिएटर, डेवलपर, युवराज)
        if (isCreatorOrOwnerQuery(lower)) {
            val reply = getCreatorReply(lower)
            return JSONObject().apply {
                put("action", "general_chat")
                put("reply", reply)
            }
        }

        // Priority 1: Make Call (कॉल लगाना / फोन मिलाना / डायलर)
        val callInfo = extractCallInfo(lower, clean)
        if (callInfo != null) {
            return JSONObject().apply {
                put("action", "make_call")
                put("target", callInfo.target)
                if (callInfo.phoneNumber != null) {
                    put("phone_number", callInfo.phoneNumber)
                }
            }
        }

        // Priority 2: Open Application (ऐप खोलना / चालू करना)
        val appName = extractAppName(lower)
        if (appName != null) {
            return JSONObject().apply {
                put("action", "open_app")
                put("app_name", appName)
            }
        }

        // Priority 3: Web Search on Google (गूगल पर सर्च करना / Google Search)
        val searchInfo = extractWebSearchQuery(lower, clean)
        if (searchInfo != null) {
            return JSONObject().apply {
                put("action", "web_search")
                put("query", searchInfo)
            }
        }

        // Priority 4: Time Inquiry (समय देखना)
        if (isTimeQuery(lower)) {
            return JSONObject().apply {
                put("action", "get_time")
            }
        }

        // Priority 5: Check Messages (मैसेज / संदेश चेक करना)
        val messagePlatform = extractMessagePlatform(lower)
        if (messagePlatform != null) {
            return JSONObject().apply {
                put("action", "check_messages")
                put("platform", messagePlatform)
            }
        }

        // Priority 6: Play Music / Song (गाना बजाना / संगीत सुनना)
        val musicQuery = extractMusicQuery(lower, clean)
        if (musicQuery != null) {
            return JSONObject().apply {
                put("action", "play_music")
                put("query", musicQuery)
            }
        }

        // Priority 7: Conversational / General Chat & GK Knowledge (बातचीत, ज्ञान एवं अन्य सवाल)
        val chatReply = generateHindiChatReply(lower, clean)
        return JSONObject().apply {
            put("action", "general_chat")
            put("reply", chatReply)
        }
    }

    // -------------------------------------------------------------
    // Helper Parsers with comprehensive Hindi / Hinglish patterns
    // -------------------------------------------------------------

    private fun isTimeQuery(q: String): Boolean {
        val timeKeywords = listOf(
            // Hindi Devanagari
            "समय", "टाइम", "कितने बजे", "क्या समय", "समय बताओ", "समय क्या", "वक़्त", "वक्त",
            // Hinglish
            "time", "samay", "kitne baje", "kitna baje", "kya time", "time kya",
            "time batao", "time hora", "ghadi", "clock"
        )

        val hasTimeWord = timeKeywords.any { q.contains(it) }
        if (!hasTimeWord) return false

        val queryIndicators = listOf(
            "kya", "batao", "hua", "hora", "ho raha", "h", "hai",
            "what", "tell", "current", "check", "baje",
            "क्या", "बताओ", "हुआ", "है"
        )
        return queryIndicators.any { q.contains(it) } || q.contains("what time") || q.contains("tell me time")
    }

    private fun extractMessagePlatform(q: String): String? {
        val messageTriggers = listOf(
            // Hindi Devanagari
            "संदेश", "मैसेज", "मेसेज", "किसका मैसेज", "कोई मैसेज आया", "मैसेज चेक",
            // Hinglish / English
            "message", "messages", "msg", "sandesh", "aya kya", "aaya kya", "aaya hai kya",
            "check karo", "dekhna", "dekho", "padho", "kiska message"
        )

        val isMessageRelated = messageTriggers.any { q.contains(it) }
        if (!isMessageRelated) return null

        return when {
            q.contains("whatsapp") || q.contains("व्हाट्सएप") || q.contains("व्हाट्सऐप") || q.contains("वाट्सएप") -> "whatsapp"
            q.contains("instagram") || q.contains("insta") || q.contains("इन्स्टाग्राम") || q.contains("इंस्टा") -> "instagram"
            q.contains("telegram") || q.contains("टेलीग्राम") -> "telegram"
            q.contains("gmail") || q.contains("email") || q.contains("ईमेल") || q.contains("जीमेल") -> "gmail"
            else -> "messages"
        }
    }

    private fun extractMusicQuery(lower: String, original: String): String? {
        val musicKeywords = listOf(
            // Hindi Devanagari
            "गाना", "गाने", "गीत", "संगीत", "बजाओ", "सुनाओ", "चलाओ",
            // Hinglish / English
            "play", "song", "songs", "music", "gaana", "gana", "bajao", "chalao", "sunao"
        )

        val hasMusicTrigger = musicKeywords.any { lower.contains(it) }
        if (!hasMusicTrigger) return null

        // If the user says "open spotify" / "spotify kholo", it's an app opening intent
        if (lower.startsWith("open ") || lower.contains(" kholo") || lower.contains("खोलो") || lower.contains(" open karo")) {
            val apps = listOf("spotify", "youtube music", "wynk", "gaana app")
            if (apps.any { lower.contains(it) }) return null
        }

        var result = original

        // Remove command verbs in English, Hinglish, and Hindi
        val stripPatterns = listOf(
            "(?i)^play\\s+(song|music)?\\s*",
            "(?i)^(ek|koi|mera|favourite)?\\s*(accha|naya|romantic|sad)?\\s*(gaana|gana|song|music)\\s*(bajao|chalao|sunao|play karo)?",
            "(?i)(gaana|gana|song|music)\\s*(bajao|chalao|sunao|play karo)",
            "(?i)\\s*(bajao|chalao|sunao|play karo)$",
            "(?i)^(chalao|bajao|sunao)\\s+",
            // Hindi Devanagari stripping
            "^(कृपया\\s+)?(एक\\s+)?(गाना|गीत|संगीत)\\s*(बजाओ|सुनाओ|चलाओ)?",
            "(गाना|गीत|संगीत)\\s*(बजाओ|सुनाओ|चलाओ)",
            "\\s*(बजाओ|सुनाओ|चलाओ)$",
            "^(बजाओ|चलाओ|सुनाओ)\\s+"
        )

        for (pattern in stripPatterns) {
            result = result.replace(Regex(pattern), "").trim()
        }

        // Clean up common prefixes
        result = result.replace(Regex("(?i)^(ek|koi|mera|favourite|favorite)\\s+"), "").trim()
        result = result.replace(Regex("^(एक|कोई|मेरा)\\s+"), "").trim()

        val emptyTerms = listOf("song", "music", "gaana", "gana", "गीत", "गाना", "संगीत")
        return if (result.isBlank() || emptyTerms.any { it.equals(result, ignoreCase = true) }) {
            ""
        } else {
            result
        }
    }

    private fun extractAppName(lower: String): String? {
        // App open verbs: open, launch, kholo, chalao, start, on karo, खोलो, चलाओ, ओपन करो, शुरू करो
        val appMap = mapOf(
            // Hindi & English synonyms -> Canonical App Identifier
            "youtube" to "youtube",
            "yt" to "youtube",
            "यू ट्यूब" to "youtube",
            "यूट्यूब" to "youtube",
            "व्हाट्सएप" to "whatsapp",
            "व्हाट्सऐप" to "whatsapp",
            "whatsapp" to "whatsapp",
            "वाट्सएप" to "whatsapp",
            "instagram" to "instagram",
            "insta" to "instagram",
            "इंस्टाग्राम" to "instagram",
            "इंस्टा" to "instagram",
            "camera" to "camera",
            "कैमरा" to "camera",
            "कैमरे" to "camera",
            "photos" to "gallery",
            "gallery" to "gallery",
            "गैलरी" to "gallery",
            "chrome" to "chrome",
            "browser" to "chrome",
            "गूगल" to "chrome",
            "क्रोम" to "chrome",
            "spotify" to "spotify",
            "स्पॉटिफाई" to "spotify",
            "settings" to "settings",
            "सेटिंग" to "settings",
            "सेटिंग्स" to "settings",
            "calculator" to "calculator",
            "कैलकुलेटर" to "calculator",
            "maps" to "maps",
            "मैप्स" to "maps",
            "नक्शा" to "maps",
            "contacts" to "contacts",
            "कॉन्टैक्ट्स" to "contacts",
            "फोन बुक" to "contacts",
            "clock" to "clock",
            "घड़ी" to "clock",
            "telegram" to "telegram",
            "टेलीग्राम" to "telegram"
        )

        val openVerbs = listOf(
            "open", "launch", "start", "kholo", "chalao", "on karo", "chalu karo", "chalu",
            "खोलो", "चलाओ", "ओपन करो", "ओपन", "शुरू करो", "चालू करो", "चालू", "दिखाओ"
        )

        // Check explicit match: e.g. "youtube kholo", "open instagram", "कैमरा चालू करो"
        for ((trigger, canonical) in appMap) {
            if (lower.contains(trigger)) {
                val hasVerb = openVerbs.any { lower.contains(it) }
                if (hasVerb || lower.startsWith("open ") || lower.endsWith(" kholo") || lower.endsWith(" खोलो")) {
                    return canonical
                }
            }
        }

        // Regex patterns for generic app names
        val regexPatterns = listOf(
            Regex("(?i)^open\\s+([a-zA-Z0-9_\\s]+)$"),
            Regex("(?i)^launch\\s+([a-zA-Z0-9_\\s]+)$"),
            Regex("(?i)^([a-zA-Z0-9_\\s]+)\\s+(open\\s*karo|kholo|chalao|start\\s*karo|on\\s*karo)$"),
            Regex("([\\u0900-\\u097F\\w\\s]+)\\s*(खोलो|चलाओ|ओपन\\s*करो)")
        )

        for (regex in regexPatterns) {
            val match = regex.find(lower.trim())
            if (match != null) {
                val rawName = match.groupValues[1].trim()
                val normalized = normalizeAppName(rawName)
                if (normalized.isNotBlank()) return normalized
            }
        }

        return null
    }

    private fun normalizeAppName(raw: String): String {
        val l = raw.lowercase(Locale.ROOT).trim()
        return when {
            l.contains("insta") || l.contains("इंस्टा") -> "instagram"
            l.contains("yt") || l.contains("youtube") || l.contains("यूट्यूब") -> "youtube"
            l.contains("wa") || l.contains("whatsapp") || l.contains("व्हाट्स") -> "whatsapp"
            l.contains("cam") || l.contains("कैमरा") -> "camera"
            l.contains("calc") || l.contains("कैलकुलेटर") -> "calculator"
            l.contains("setting") || l.contains("सेटिंग") -> "settings"
            l.contains("chrome") || l.contains("browse") || l.contains("क्रोम") -> "chrome"
            l.contains("gallery") || l.contains("गैलरी") || l.contains("फोटो") -> "gallery"
            l.contains("map") || l.contains("मैप") || l.contains("नक्शा") -> "maps"
            else -> l.replace(Regex("[^a-zA-Z0-9\\u0900-\\u097F]"), "")
        }
    }

    private fun isCreatorOrOwnerQuery(q: String): Boolean {
        // Exclude action commands like calling someone
        if (q.contains("call") || q.contains("कॉल") || q.contains("phone lagao") || q.contains("फोन लगाओ") || q.contains("phone karo")) {
            return false
        }

        val keywords = listOf(
            // Owner variants
            "owner", "ओनर", "ownar",
            // Malik / Maalik variants
            "malik", "maalik", "मालिक",
            // Boss
            "boss", "बॉस",
            // Creator / Developer / Maker variants
            "creator", "क्रिएटर", "developer", "डेवलपर", "author", "maker",
            // Creation phrases
            "kisne banaya", "kisne bnaya", "kisne banayi", "kisne banaya hai", "kisne design kiya", "kisne develop kiya",
            "किसने बनाया", "किसने बनाई", "किसने बनाया है", "किसने डिजाइन किया", "किसने डेवलप किया",
            "किसका ऐप", "किसका एप", "किसका असिस्टेंट", "किसका है", "किसका फोन",
            "kiska app", "kiska assistant", "kiska phone", "kiska hai", "kiska h", "kiski app",
            "who made", "who created", "who built", "who coded", "who developed", "who is your owner", "who is owner",
            // Yuvraj directly
            "yuvraj", "युवराज", "yuvraj kon", "yuvraj kaun", "who is yuvraj"
        )
        return keywords.any { q.contains(it) }
    }

    private fun getCreatorReply(lower: String): String {
        return when {
            lower.contains("yuvraj") || lower.contains("युवराज") ->
                "युवराज (Yuvraj) मेरे ओनर, क्रिएटर और डेवलपर हैं! उन्होंने ही मुझे एक स्मार्ट AI वॉयस असिस्टेंट के रूप में डिज़ाइन और तैयार किया है।"

            lower.contains("owner") || lower.contains("ओनर") || lower.contains("malik") || lower.contains("मालिक") || lower.contains("boss") || lower.contains("बॉस") ->
                "मेरे ओनर और क्रिएटर युवराज (Yuvraj) हैं! मुझे युवराज सर द्वारा ही बनाया और डेवलप किया गया है।"

            else ->
                "मुझे युवराज (Yuvraj) ने बनाया और डेवलप किया है! मैं Yuvraj का स्मार्ट AI वॉयस असिस्टेंट Velo हूँ।"
        }
    }

    data class CallTargetInfo(
        val target: String,
        val phoneNumber: String?
    )

    private fun extractCallInfo(lower: String, original: String): CallTargetInfo? {
        val callTriggers = listOf(
            // Hindi Devanagari
            "कॉल", "फोन लगाओ", "फोन करो", "फोन मिलाओ", "फोन मिला दो", "डायल",
            // Hinglish / English
            "call", "phone lagao", "phone karo", "phone lagana", "phone mila do",
            "phone milao", "phone kardo", "dial"
        )

        // Avoid false positives like Truecaller app
        if (lower.contains("truecaller")) return null

        val hasTrigger = callTriggers.any { lower.contains(it) }
        if (!hasTrigger) return null

        // Check for phone number in query (e.g. 9876543210 or 100 or +91...)
        val digitMatch = Regex("(\\+?[0-9]{3,14})").find(original)?.value

        var cleaned = original

        // Strip common call verbs and words
        val stripPatterns = listOf(
            "(?i)^please\\s+",
            "(?i)^kripya\\s+",
            "(?i)^कृपया\\s+",
            "(?i)^(make a call|make call|start call)\\s*",
            "(?i)^(call lagane ka|call lagana|call lagao|call karo|call kardo|call karna hai|call karni hai|call mila do|call milao)\\s*",
            "(?i)\\s*(ko\\s+)?(call lagane ka|call lagao|call lagana|call karo|call kardo|call karna|call karna hai|call mila do|call milao)$",
            "(?i)\\s*(ko\\s+)?(phone lagao|phone karo|phone lagana|phone kardo|phone mila do|phone milao)$",
            "(?i)\\s*(par\\s+)?(call lagao|call karo|call kardo|call lagana)$",
            "(?i)^call\\s+",
            "(?i)^dial\\s+",
            "(?i)\\s*dial\\s*(karo|karna)?$",
            // Hindi Devanagari patterns
            "(?i)^(कॉल लगाने का|कॉल लगाओ|कॉल करो|कॉल करना है|कॉल मिलाओ|कॉल मिला दो|कॉल लगाइए)\\s*",
            "\\s*(को\\s+)?(कॉल लगाने का|कॉल लगाओ|कॉल करो|कॉल करना है|कॉल मिलाओ|कॉल मिला दो|कॉल लगाइए)$",
            "\\s*(को\\s+)?(फोन लगाओ|फोन करो|फोन मिलाओ|फोन मिला दो)$",
            "\\s*(पर\\s+)?(कॉल लगाओ|कॉल करो)$",
            "^कॉल\\s+",
            "^डायल\\s+"
        )

        for (pattern in stripPatterns) {
            cleaned = cleaned.replace(Regex(pattern), "").trim()
        }

        // Clean trailing/leading connectors like "ko", "par", "pe", "को", "पर"
        cleaned = cleaned.replace(Regex("(?i)^(ko|par|pe|to)\\s+"), "").trim()
        cleaned = cleaned.replace(Regex("(?i)\\s+(ko|par|pe|to)$"), "").trim()
        cleaned = cleaned.replace(Regex("^(को|पर|पे)\\s+"), "").trim()
        cleaned = cleaned.replace(Regex("\\s+(को|पर|पे)$"), "").trim()

        val genericWords = listOf(
            "call", "phone", "lagao", "karo", "lagane ka", "dialer", "dial",
            "कॉल", "फोन", "डायल", "लगाने का", "लगाओ", "करो"
        )

        val finalTarget = if (genericWords.any { it.equals(cleaned, ignoreCase = true) } || cleaned.isBlank()) {
            digitMatch ?: ""
        } else {
            cleaned
        }

        return CallTargetInfo(
            target = finalTarget,
            phoneNumber = digitMatch ?: if (finalTarget.all { it.isDigit() || it == '+' } && finalTarget.length >= 3) finalTarget else null
        )
    }

    private fun extractWebSearchQuery(lower: String, original: String): String? {
        val searchTriggers = listOf(
            // English / Hinglish
            "google search", "search google", "search on google", "search karo", "search kar", "search",
            "google par search", "google pe search", "google par dhoondo", "google pe dhoondo",
            // Hindi Devanagari
            "गूगल सर्च", "गूगल पर सर्च", "गूगल पे सर्च", "सर्च करो", "ढूंढो", "गूगल पर ढूंढो", "गूगल पे खोजो", "खोजो"
        )

        // Avoid app opening triggers like "google kholo" / "open chrome"
        if (lower.startsWith("open ") || lower.contains(" kholo") || lower.contains("खोलो")) {
            return null
        }

        val hasTrigger = searchTriggers.any { lower.contains(it) }
        if (!hasTrigger) return null

        var cleaned = original
        val stripPatterns = listOf(
            "(?i)^google\\s+search\\s*(karo|karna)?\\s*",
            "(?i)^search\\s+(on\\s+google|google\\s+par|google\\s+pe)?\\s*",
            "(?i)^google\\s*(par|pe)?\\s*(search|dhoondo|khojo)?\\s*",
            "(?i)\\s*(google\\s+par|google\\s+pe)?\\s*(search\\s*karo|dhoondo|khojo)$",
            "(?i)\\s*(search\\s*karo|search\\s*karna)$",
            "^गूगल\\s+(पर|पे)?\\s*(सर्च|ढूंढो|खोजो)?\\s*",
            "\\s*(गूगल\\s+पर|गूगल\\s+पे)?\\s*(सर्च\\s*करो|ढूंढो|खोजो)$"
        )

        for (pattern in stripPatterns) {
            cleaned = cleaned.replace(Regex(pattern), "").trim()
        }

        cleaned = cleaned.replace(Regex("(?i)^(par|pe|for|about)\\s+"), "").trim()
        cleaned = cleaned.replace(Regex("^(पर|पे|के बारे में)\\s+"), "").trim()

        val emptyTerms = listOf("google", "search", "गूगल", "सर्च")
        return if (cleaned.isBlank() || emptyTerms.any { it.equals(cleaned, ignoreCase = true) }) {
            ""
        } else {
            cleaned
        }
    }

    private fun generateHindiChatReply(lower: String, original: String): String {
        return when {
            // निर्माता / क्रिएटर / ओनर (Creator / Owner / By Yuvraj)
            isCreatorOrOwnerQuery(lower) ->
                getCreatorReply(lower)

            // Google नॉलेज / सामान्य ज्ञान (General Knowledge / Facts)
            lower.contains("capital of india") || lower.contains("bharat ki rajdhani") || lower.contains("भारत की राजधानी") || lower.contains("india ki capital") ->
                "भारत की राजधानी नई दिल्ली (New Delhi) है।"

            lower.contains("prime minister") || lower.contains("pm of india") || lower.contains("bharat ke pradhan mantri") || lower.contains("प्रधानमंत्री") ->
                "भारत के वर्तमान प्रधानमंत्री श्री नरेंद्र मोदी (Narendra Modi) हैं।"

            lower.contains("president of india") || lower.contains("bharat ke rashtrapati") || lower.contains("राष्ट्रपति") ->
                "भारत की वर्तमान राष्ट्रपति श्रीमती द्रौपदी मुर्मू (Droupadi Murmu) हैं।"

            lower.contains("largest country") || lower.contains("sabse bada desh") || lower.contains("सबसे बड़ा देश") ->
                "क्षेत्रफल के हिसाब से दुनिया का सबसे बड़ा देश रूस (Russia) है, और जनसंख्या के हिसाब से भारत (India) शीर्ष पर है।"

            lower.contains("tallest mountain") || lower.contains("mount everest") || lower.contains("sabse uncha pahad") || lower.contains("सबसे ऊंचा पहाड़") ->
                "दुनिया का सबसे ऊँचा पर्वत शिखर माउंट एवरेस्ट (Mount Everest) है, जिसकी ऊँचाई 8,848.86 मीटर है।"

            lower.contains("chandrayaan") || lower.contains("chandrayan") || lower.contains("चंद्रयान") ->
                "चंद्रयान-3 भारत के ISRO का एक ऐतिहासिक मिशन है, जिसने चंद्रमा के दक्षिणी ध्रुव पर सफल सॉफ्ट लैंडिंग करके भारत का नाम इतिहास में दर्ज कराया।"

            lower.contains("earth") || lower.contains("prithvi") || lower.contains("पृथ्वी") ->
                "पृथ्वी (Earth) सौरमंडल का तीसरा ग्रह है और ब्रह्मांड में ज्ञात एकमात्र ऐसा ग्रह है जहाँ जीवन मौजूद है।"

            lower.contains("sun") || lower.contains("suraj") || lower.contains("सूरज") || lower.contains("सूर्य") ->
                "सूर्य (Sun) हमारे सौरमंडल के केंद्र में स्थित एक तारा है, जो पृथ्वी पर ऊर्जा, प्रकाश और जीवन का मुख्य स्रोत है।"

            lower.contains("ai kya hai") || lower.contains("what is ai") || lower.contains("artificial intelligence") || lower.contains("एआई क्या है") ->
                "AI यानी आर्टिफिशियल इंटेलिजेंस (Artificial Intelligence) कंप्यूटर और मशीनों की वह तकनीक है जिससे वे इंसानों की तरह सोच, समझ, सीख और निर्णय ले सकते हैं।"

            // हाल चाल (Greetings / How are you)
            lower.contains("kaise ho") || lower.contains("kya haal") || lower.contains("how are you") ||
                    lower.contains("कैसे हो") || lower.contains("क्या हाल") ->
                "मैं बढ़िया हूँ! आपकी मदद के लिए हमेशा तैयार हूँ। बताइए क्या करूँ?"

            // परिचय (Identity)
            lower.contains("tum kaun ho") || lower.contains("aap kaun ho") || lower.contains("who are you") || lower.contains("naam kya") ||
                    lower.contains("तुम कौन हो") || lower.contains("आप कौन हो") || lower.contains("आप कौन हैं") ||
                    lower.contains("तुम्हारा नाम") || lower.contains("आपका नाम") || lower.contains("नाम क्या है") ->
                "नमस्ते! मेरा नाम Velo है, जिसे Yuvraj द्वारा बनाया गया है। मैं आपका स्मार्ट AI वॉयस असिस्टेंट हूँ।"

            // क्षमताएं (Capabilities)
            lower.contains("kya kar sakte ho") || lower.contains("what can you do") || lower.contains("help") ||
                    lower.contains("क्या कर सकते हो") || lower.contains("मदद") ->
                "मैं आपके कहने पर ऐप्स खोल सकता हूँ, फोन कॉल लगा सकता हूँ, गूगल पर कुछ भी सर्च कर सकता हूँ, गाने बजा सकता हूँ, मैसेज चेक कर सकता हूँ और दुनिया भर की जानकारी दे सकता हूँ। बस बोलिए!"

            // अभिवादन (Hello / Namaste)
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ||
                    lower.contains("namaste") || lower.contains("नमस्ते") || lower.contains("प्रणाम") ->
                "नमस्ते! कहिए, मैं आपकी क्या सेवा करूँ?"

            // सुप्रभात (Good Morning)
            lower.contains("good morning") || lower.contains("suprabhat") || lower.contains("सुप्रभात") ->
                "सुप्रभात! आपका दिन शुभ और शानदार रहे। बताइए क्या काम है?"

            // शुभ रात्रि (Good Night)
            lower.contains("good night") || lower.contains("shubh ratri") || lower.contains("शुभ रात्रि") ->
                "शुभ रात्रि! अच्छी नींद लीजिए। कल मिलते हैं!"

            // धन्यवाद (Thank You)
            lower.contains("shukriya") || lower.contains("thank") || lower.contains("dhanyawad") ||
                    lower.contains("शुक्रिया") || lower.contains("धन्यवाद") ->
                "अरे कोई बात नहीं! आपकी मदद करना मेरा काम है।"

            // चुटकुला (Jokes)
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("चुटकुला") || lower.contains("हंसाओ") ->
                "अध्यापक: अगर एक पेड़ पर 5 पक्षी बैठे हैं और शिकारी एक को गोली मार दे, तो कितने बचेंगे? छात्र: शून्य, क्योंकि बाकी सब गोली की आवाज से उड़ जाएंगे!"

            // मौसम (Weather)
            lower.contains("mausam") || lower.contains("weather") || lower.contains("मौसम") || lower.contains("बारिश") ->
                "आज का मौसम बहुत बढ़िया है! बाहर घूमने या कोई अच्छा गाना सुनने का एकदम सही समय है।"

            else ->
                "नमस्ते! मैं Velo हूँ। आप मुझे कोई भी सवाल पूछ सकते हैं, गूगल सर्च कर सकते हैं, कॉल लगा सकते हैं, या ऐप्स खोलने का आदेश दे सकते हैं।"
        }
    }
}

package com.example.data.engine

import org.json.JSONObject
import java.util.Locale

object VeloLocalParser {

    /**
     * Parses user query into an exact structured JSON response according to the Velo AI Assistant spec.
     */
    fun parseToStructuredJson(query: String): JSONObject {
        val clean = query.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // 1. Check Time commands:
        // Examples: 'time hora wo sb', 'क्या समय हुआ है', 'kya time hua hai', 'time kya hai', 'samay kya hai', 'kitna baje'
        if (isCheckTimeCommand(lower)) {
            return JSONObject().apply {
                put("action", "get_time")
            }
        }

        // 2. Check Messages commands:
        // Examples: 'eska message aya kya', 'whatsapp check karo', 'messages check karo', 'kisi ka message aaya kya'
        val platform = extractMessagePlatform(lower)
        if (platform != null) {
            return JSONObject().apply {
                put("action", "check_messages")
                put("platform", platform)
            }
        }

        // 3. Play Music commands:
        // Examples: 'play song', 'gaana bajao', 'gana bajao', 'gana chalao', 'play kesariya', 'arjit singh ka gana'
        val musicMatch = extractMusicQuery(lower, clean)
        if (musicMatch != null) {
            return JSONObject().apply {
                put("action", "play_music")
                put("query", musicMatch)
            }
        }

        // 4. Open App commands:
        // Examples: 'open youtube', 'insta open karo', 'whatsapp kholo', 'camera chalao', 'open chrome'
        val appName = extractAppName(lower)
        if (appName != null) {
            return JSONObject().apply {
                put("action", "open_app")
                put("app_name", appName)
            }
        }

        // 5. General Chat (Hindi / Hinglish friendly replies):
        val chatReply = generateFriendlyChatReply(lower, clean)
        return JSONObject().apply {
            put("action", "general_chat")
            put("reply", chatReply)
        }
    }

    private fun isCheckTimeCommand(q: String): Boolean {
        val triggers = listOf(
            "time hora", "क्या समय हुआ है", "क्या समय है", "समय क्या है",
            "kya time hua hai", "time kya hua", "time kya hai", "time batao",
            "time kya ho raha", "kitna baje", "kitne baje", "samay kya hai",
            "samay batao", "what is the time", "what time is it", "check time",
            "tell me the time", "current time"
        )
        return triggers.any { q.contains(it) } || (q.contains("time") && (q.contains("batao") || q.contains("hora") || q.contains("kya")))
    }

    private fun extractMessagePlatform(q: String): String? {
        val isMessageQuery = q.contains("message") || q.contains("msg") || q.contains("sandesh") ||
                q.contains("aya kya") || q.contains("aaya kya") || q.contains("aaya hai kya") ||
                q.contains("check karo") || q.contains("check messages")

        if (!isMessageQuery) return null

        return when {
            q.contains("whatsapp") -> "whatsapp"
            q.contains("insta") || q.contains("instagram") -> "instagram"
            q.contains("telegram") -> "telegram"
            q.contains("gmail") || q.contains("email") || q.contains("mail") -> "gmail"
            else -> "messages"
        }
    }

    private fun extractMusicQuery(q: String, original: String): String? {
        val musicKeywords = listOf("play", "gaana", "gana", "song", "music", "bajao", "sunao", "chalao")
        val hasMusicTrigger = musicKeywords.any { q.contains(it) }
        if (!hasMusicTrigger) return null

        // Check if user is asking to open an app (e.g. "open spotify") rather than play music
        if (q.startsWith("open ") || q.endsWith(" open karo") || q.endsWith(" kholo")) {
            return null
        }

        var query = original
        val removePatterns = listOf(
            "(?i)^play\\s+(song|music)?\\s*",
            "(?i)^koi\\s+(accha|naya)?\\s*(gaana|gana)\\s*(bajao|chalao|sunao)?",
            "(?i)(gaana|gana)\\s*(bajao|chalao|sunao)",
            "(?i)(song|music)\\s*(bajao|chalao|play karo)",
            "(?i)\\s*(bajao|chalao|sunao|play karo)$",
            "(?i)^chalao\\s+",
            "(?i)^bajao\\s+"
        )

        for (pattern in removePatterns) {
            query = query.replace(Regex(pattern), "").trim()
        }

        // Clean up common filler words
        query = query.replace(Regex("(?i)^(ek|koi|mera|favourite)\\s+"), "").trim()

        return if (query.isBlank() || query.equals("song", ignoreCase = true) || query.equals("gaana", ignoreCase = true) || query.equals("music", ignoreCase = true)) {
            ""
        } else {
            query
        }
    }

    private fun extractAppName(q: String): String? {
        val openPatterns = listOf(
            Regex("(?i)^open\\s+([a-zA-Z0-9_\\s]+)$"),
            Regex("(?i)^launch\\s+([a-zA-Z0-9_\\s]+)$"),
            Regex("(?i)^start\\s+([a-zA-Z0-9_\\s]+)$"),
            Regex("(?i)^([a-zA-Z0-9_\\s]+)\\s+(open\\s*karo|kholo|chalao|start\\s*karo|on\\s*karo)$")
        )

        for (regex in openPatterns) {
            val match = regex.find(q.trim())
            if (match != null) {
                var app = match.groupValues[1].trim()
                app = normalizeAppName(app)
                if (app.isNotEmpty()) return app
            }
        }

        // Specific quick match for common phrases
        val commonApps = listOf(
            "youtube", "insta", "instagram", "whatsapp", "spotify", "chrome",
            "camera", "settings", "gallery", "maps", "calculator", "telegram",
            "gmail", "facebook", "twitter", "contacts", "clock"
        )
        for (app in commonApps) {
            if (q.contains(app) && (q.contains("kholo") || q.contains("open") || q.contains("chalao") || q.contains("start"))) {
                return normalizeAppName(app)
            }
        }

        return null
    }

    fun normalizeAppName(raw: String): String {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("insta") -> "instagram"
            lower.contains("yt") || lower.contains("you tube") -> "youtube"
            lower.contains("wa") || lower.contains("whatsap") -> "whatsapp"
            lower.contains("cam") -> "camera"
            lower.contains("calc") -> "calculator"
            lower.contains("setting") -> "settings"
            lower.contains("browse") || lower.contains("google") -> "chrome"
            else -> lower.replace(Regex("[^a-zA-Z0-9]"), "")
        }
    }

    private fun generateFriendlyChatReply(lower: String, original: String): String {
        return when {
            lower.contains("kaise ho") || lower.contains("kya haal") || lower.contains("how are you") ->
                "मैं बढ़िया हूँ! आपकी मदद के लिए हमेशा तैयार हूँ। बताइए क्या करूँ?"

            lower.contains("tum kaun ho") || lower.contains("who are you") || lower.contains("naam kya hai") || lower.contains("your name") ->
                "नमस्ते! मेरा नाम Velo है, आपका स्मार्ट AI वॉयस असिस्टेंट। मैं आपके फोन में ऐप्स खोल सकता हूँ, गाने चला सकता हूँ, और बहुत कुछ!"

            lower.contains("kya kar sakte ho") || lower.contains("what can you do") || lower.contains("help") ->
                "मैं आपके कहने पर ऐप्स खोल सकता हूँ, मनपसंद गाने बजा सकता हूँ, मैसेज चेक कर सकता हूँ और समय बता सकता हूँ। बस बोलिए!"

            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("namaste") || lower.contains("नमस्ते") ->
                "नमस्ते! कहिए, मैं आपकी क्या सेवा करूँ?"

            lower.contains("good morning") || lower.contains("suprabhat") || lower.contains("सुप्रभात") ->
                "सुप्रभात! आपका दिन शुभ और शानदार रहे। बताइए क्या काम है?"

            lower.contains("good night") || lower.contains("shubh ratri") ->
                "शुभ रात्रि! अच्छी नींद लीजिए। कल मिलते हैं!"

            lower.contains("shukriya") || lower.contains("thank") || lower.contains("dhanyawad") ->
                "अरे कोई बात नहीं! आपकी मदद करना मेरा काम है।"

            lower.contains("joke") || lower.contains("chutkula") ->
                "अध्यापक: अगर एक पेड़ पर 5 पक्षी बैठे हैं और शिकारी एक को गोली मार दे, तो कितने बचेंगे? छात्र: शून्य, क्योंकि बाकी सब उड़ जाएंगे!"

            else ->
                "नमस्ते! मैं Velo हूँ। आप मुझे कोई भी ऐप खोलने, गाना बजाने, मैसेज या समय चेक करने का आदेश दे सकते हैं।"
        }
    }
}

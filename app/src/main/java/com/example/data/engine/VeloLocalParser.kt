package com.example.data.engine

import org.json.JSONObject

object VeloLocalParser {

    /**
     * Parses user query into an exact structured JSON response according to the Velo AI Assistant spec.
     * Fully delegates to SpeechToCommandParser with deep Hindi and English command recognition.
     */
    fun parseToStructuredJson(query: String): JSONObject {
        return SpeechToCommandParser.parseSpeechTextToJson(query)
    }
}


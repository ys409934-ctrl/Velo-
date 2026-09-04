package com.example.data.model

sealed class VeloAction {
    abstract val actionName: String
    abstract val rawJson: String
    abstract val userQuery: String
    abstract val timestamp: Long
    abstract val executionStatus: String
    abstract val isSuccess: Boolean

    data class OpenApp(
        val appName: String,
        override val rawJson: String,
        override val userQuery: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val executionStatus: String = "",
        override val isSuccess: Boolean = true
    ) : VeloAction() {
        override val actionName: String = "open_app"
    }

    data class PlayMusic(
        val query: String,
        override val rawJson: String,
        override val userQuery: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val executionStatus: String = "",
        override val isSuccess: Boolean = true
    ) : VeloAction() {
        override val actionName: String = "play_music"
    }

    data class CheckMessages(
        val platform: String,
        override val rawJson: String,
        override val userQuery: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val executionStatus: String = "",
        override val isSuccess: Boolean = true
    ) : VeloAction() {
        override val actionName: String = "check_messages"
    }

    data class GetTime(
        val formattedTime: String,
        override val rawJson: String,
        override val userQuery: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val executionStatus: String = "",
        override val isSuccess: Boolean = true
    ) : VeloAction() {
        override val actionName: String = "get_time"
    }

    data class GeneralChat(
        val reply: String,
        override val rawJson: String,
        override val userQuery: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val executionStatus: String = "",
        override val isSuccess: Boolean = true
    ) : VeloAction() {
        override val actionName: String = "general_chat"
    }
}

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

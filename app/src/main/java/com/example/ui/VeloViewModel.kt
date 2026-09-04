package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.executor.ActionExecutor
import com.example.data.model.VeloAction
import com.example.data.model.VoiceState
import com.example.data.network.GeminiService
import com.example.service.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VeloViewModel(application: Application) : AndroidViewModel(application) {

    private val actionExecutor = ActionExecutor(application.applicationContext)
    private val geminiService = GeminiService()

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _currentAction = MutableStateFlow<VeloAction?>(null)
    val currentAction: StateFlow<VeloAction?> = _currentAction.asStateFlow()

    private val _history = MutableStateFlow<List<VeloAction>>(emptyList())
    val history: StateFlow<List<VeloAction>> = _history.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap the mic or enter a command below")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private var voiceAssistantManager: VoiceAssistantManager? = null

    val soundLevel: StateFlow<Float>
        get() = voiceAssistantManager?.soundLevel ?: MutableStateFlow(0f)

    init {
        voiceAssistantManager = VoiceAssistantManager(
            context = application.applicationContext,
            onResultReceived = { recognizedText ->
                _inputText.value = recognizedText
                processVoiceCommand(recognizedText)
            },
            onErrorReceived = { errorMessage ->
                _voiceState.value = VoiceState.ERROR
                _statusMessage.value = errorMessage
            }
        )

        // Monitor speech manager listening/speaking states
        viewModelScope.launch {
            voiceAssistantManager?.isListening?.collect { listening ->
                if (listening) {
                    _voiceState.value = VoiceState.LISTENING
                    _statusMessage.value = "Listening to voice in Hindi / English..."
                } else if (_voiceState.value == VoiceState.LISTENING) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }

        viewModelScope.launch {
            voiceAssistantManager?.isSpeaking?.collect { speaking ->
                if (speaking) {
                    _voiceState.value = VoiceState.SPEAKING
                } else if (_voiceState.value == VoiceState.SPEAKING) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun toggleVoiceListening() {
        if (_voiceState.value == VoiceState.LISTENING) {
            voiceAssistantManager?.stopListening()
            _voiceState.value = VoiceState.IDLE
            _statusMessage.value = "Ready"
        } else {
            voiceAssistantManager?.startListening()
        }
    }

    fun stopSpeaking() {
        voiceAssistantManager?.stopSpeaking()
        _voiceState.value = VoiceState.IDLE
    }

    fun toggleTts() {
        val newState = !_isTtsEnabled.value
        _isTtsEnabled.value = newState
        if (!newState) {
            stopSpeaking()
        }
    }

    fun submitTextCommand() {
        val query = _inputText.value.trim()
        if (query.isNotBlank()) {
            processVoiceCommand(query)
            _inputText.value = ""
        }
    }

    fun processVoiceCommand(command: String) {
        if (command.isBlank()) return

        _voiceState.value = VoiceState.PROCESSING
        _statusMessage.value = "Processing command with Velo AI..."

        viewModelScope.launch {
            try {
                // 1. Get structured JSON from Gemini or local parser
                val jsonResponse = geminiService.parseVoiceCommand(command)
                val rawJsonString = jsonResponse.toString()

                // 2. Map JSON into VeloAction
                val parsedAction = mapJsonToAction(jsonResponse, rawJsonString, command)

                // 3. Execute device action
                val executionResult = actionExecutor.execute(parsedAction)

                // 4. Update action with execution results
                val finalizedAction = when (parsedAction) {
                    is VeloAction.OpenApp -> parsedAction.copy(
                        executionStatus = executionResult.message,
                        isSuccess = executionResult.success
                    )
                    is VeloAction.PlayMusic -> parsedAction.copy(
                        executionStatus = executionResult.message,
                        isSuccess = executionResult.success
                    )
                    is VeloAction.CheckMessages -> parsedAction.copy(
                        executionStatus = executionResult.message,
                        isSuccess = executionResult.success
                    )
                    is VeloAction.GetTime -> parsedAction.copy(
                        executionStatus = executionResult.message,
                        isSuccess = executionResult.success
                    )
                    is VeloAction.GeneralChat -> parsedAction.copy(
                        executionStatus = executionResult.message,
                        isSuccess = executionResult.success
                    )
                }

                _currentAction.value = finalizedAction
                _history.value = listOf(finalizedAction) + _history.value

                _statusMessage.value = executionResult.message

                // 5. Speak response if enabled
                if (_isTtsEnabled.value && executionResult.speechNarration.isNotBlank()) {
                    _voiceState.value = VoiceState.SPEAKING
                    voiceAssistantManager?.speak(executionResult.speechNarration, isMuted = false)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }

            } catch (e: Exception) {
                _voiceState.value = VoiceState.ERROR
                _statusMessage.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    private fun mapJsonToAction(json: JSONObject, rawJson: String, originalQuery: String): VeloAction {
        val actionType = json.optString("action", "general_chat")

        return when (actionType) {
            "open_app" -> {
                val appName = json.optString("app_name", "app")
                VeloAction.OpenApp(
                    appName = appName,
                    rawJson = rawJson,
                    userQuery = originalQuery
                )
            }
            "play_music" -> {
                val query = json.optString("query", "")
                VeloAction.PlayMusic(
                    query = query,
                    rawJson = rawJson,
                    userQuery = originalQuery
                )
            }
            "check_messages" -> {
                val platform = json.optString("platform", "messages")
                VeloAction.CheckMessages(
                    platform = platform,
                    rawJson = rawJson,
                    userQuery = originalQuery
                )
            }
            "get_time" -> {
                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                VeloAction.GetTime(
                    formattedTime = formattedTime,
                    rawJson = rawJson,
                    userQuery = originalQuery
                )
            }
            else -> {
                val reply = json.optString("reply", "नमस्ते! मैं वेलो हूँ, आपकी क्या मदद करूँ?")
                VeloAction.GeneralChat(
                    reply = reply,
                    rawJson = rawJson,
                    userQuery = originalQuery
                )
            }
        }
    }

    fun replaySpeech(action: VeloAction) {
        val textToSpeak = when (action) {
            is VeloAction.OpenApp -> "${action.appName} खोला जा रहा है"
            is VeloAction.PlayMusic -> if (action.query.isNotBlank()) "${action.query} बजाया जा रहा है" else "गाना बजाया जा रहा है"
            is VeloAction.CheckMessages -> "${action.platform} में मैसेज चेक किए जा रहे हैं"
            is VeloAction.GetTime -> "अभी ${action.formattedTime} बजे हैं"
            is VeloAction.GeneralChat -> action.reply
        }
        voiceAssistantManager?.speak(textToSpeak, isMuted = false)
    }

    fun rerunAction(action: VeloAction) {
        val executionResult = actionExecutor.execute(action)
        _statusMessage.value = executionResult.message
        if (_isTtsEnabled.value && executionResult.speechNarration.isNotBlank()) {
            voiceAssistantManager?.speak(executionResult.speechNarration, isMuted = false)
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        _currentAction.value = null
        _statusMessage.value = "History cleared"
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantManager?.destroy()
    }
}

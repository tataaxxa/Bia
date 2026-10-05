package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.SpeechInputManager
import com.example.audio.VoiceProfile
import com.example.data.AssistantResponse
import com.example.data.ChatMessage
import com.example.data.GeminiRepository
import com.example.data.MessageSender
import com.example.device.AndroidActionBridge
import com.example.device.DeviceActionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantVisualState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING_ACTION
}

data class ContactClarificationState(
    val isOpen: Boolean = false,
    val queryName: String = "",
    val question: String = "",
    val contacts: List<String> = emptyList()
)

data class ActionBannerState(
    val isVisible: Boolean = false,
    val actionName: String = "",
    val details: String = "",
    val success: Boolean = true
)

data class ArushiUiState(
    val visualState: AssistantVisualState = AssistantVisualState.IDLE,
    val detectedLanguage: String = "Português (Brasil)",
    val selectedVoice: VoiceProfile = VoiceProfile.JARVIS,
    val messages: List<ChatMessage> = emptyList(),
    val partialSpeech: String = "",
    val currentAudioAmplitude: Float = 0f,
    val isMicrophoneActive: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val clarificationState: ContactClarificationState = ContactClarificationState(),
    val actionBanner: ActionBannerState = ActionBannerState(),
    val showBridgeConsole: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val hasApiKey: Boolean = false,
    val lastRecognizedText: String = ""
)

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    val actionManager = DeviceActionManager(application.applicationContext)
    val audioPlayer = AudioPlayer(application.applicationContext)
    val speechInputManager = SpeechInputManager(application.applicationContext)
    val geminiRepository = GeminiRepository(application.applicationContext, actionManager)

    val actionBridge = AndroidActionBridge(actionManager) { action, msg, success ->
        showActionBanner(action, msg, success)
        addMessage(
            ChatMessage(
                sender = MessageSender.SYSTEM_ACTION,
                text = "Protocolo Executado: $action -> $msg",
                actionName = action,
                actionDetails = msg,
                actionSuccess = success
            )
        )
    }

    private val welcomeGreetingPortuguese = "Olá, senhor! Eu sou o J.A.R.V.I.S., seu assistente de inteligência artificial. Todos os sistemas estão online e configurados em Português do Brasil. Fale comigo em português e eu vou reconhecer sua voz e responder em português. Posso abrir o WhatsApp, YouTube, Prisma 3D, Godot, fazer ligações e executar comandos no seu celular!"

    private val _uiState = MutableStateFlow(
        ArushiUiState(
            hasApiKey = geminiRepository.hasApiKey,
            detectedLanguage = "Português (Brasil)",
            selectedVoice = VoiceProfile.JARVIS,
            messages = listOf(
                ChatMessage(
                    sender = MessageSender.ARUSHI,
                    text = welcomeGreetingPortuguese
                )
            )
        )
    )
    val uiState: StateFlow<ArushiUiState> = _uiState.asStateFlow()

    init {
        // Configure JARVIS voice by default
        audioPlayer.setVoiceProfile(VoiceProfile.JARVIS)
        geminiRepository.activeVoiceName = VoiceProfile.JARVIS.geminiVoiceName

        // Speak initial Portuguese welcome message with JARVIS voice
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            audioPlayer.speakText(welcomeGreetingPortuguese, "pt-BR") {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
            }
        }

        // Collect audio playback status & amplitude for Arc Reactor HUD animation
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                _uiState.update { current ->
                    current.copy(
                        isAudioPlaying = playing,
                        visualState = when {
                            playing -> AssistantVisualState.SPEAKING
                            current.isMicrophoneActive -> AssistantVisualState.LISTENING
                            current.visualState == AssistantVisualState.THINKING -> AssistantVisualState.THINKING
                            else -> AssistantVisualState.IDLE
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            audioPlayer.currentAmplitude.collect { amp ->
                _uiState.update { it.copy(currentAudioAmplitude = amp) }
            }
        }

        // Collect mic input status
        viewModelScope.launch {
            speechInputManager.isListening.collect { listening ->
                _uiState.update { current ->
                    current.copy(
                        isMicrophoneActive = listening,
                        visualState = if (listening) AssistantVisualState.LISTENING else {
                            if (current.isAudioPlaying) AssistantVisualState.SPEAKING else current.visualState
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            speechInputManager.rmsDb.collect { rms ->
                if (_uiState.value.isMicrophoneActive) {
                    _uiState.update { it.copy(currentAudioAmplitude = rms) }
                }
            }
        }

        viewModelScope.launch {
            speechInputManager.partialText.collect { partial ->
                _uiState.update { it.copy(partialSpeech = partial) }
            }
        }

        // Speech recognition result
        speechInputManager.onSpeechResult = { text ->
            onUserSpoke(text)
        }

        speechInputManager.onSpeechError = { errorMsg ->
            showActionBanner("Microfone", errorMsg, false)
        }
    }

    fun interruptArushi() {
        audioPlayer.stop()
        _uiState.update {
            it.copy(
                isAudioPlaying = false,
                visualState = if (it.isMicrophoneActive) AssistantVisualState.LISTENING else AssistantVisualState.IDLE
            )
        }
    }

    fun toggleMicrophone(hasRecordAudioPermission: Boolean) {
        if (!hasRecordAudioPermission) return

        if (_uiState.value.isMicrophoneActive) {
            speechInputManager.stopListening()
        } else {
            // Stop any ongoing assistant speech first (interruption)
            interruptArushi()
            _uiState.update { it.copy(visualState = AssistantVisualState.LISTENING, partialSpeech = "") }
            speechInputManager.startListening("pt-BR")
        }
    }

    fun onUserSpoke(userText: String) {
        if (userText.isBlank()) return

        interruptArushi()

        // Keep language confirmed as Portuguese
        _uiState.update { it.copy(detectedLanguage = "Português (Brasil)") }

        addMessage(
            ChatMessage(
                sender = MessageSender.USER,
                text = userText
            )
        )

        _uiState.update {
            it.copy(
                visualState = AssistantVisualState.THINKING,
                lastRecognizedText = userText,
                partialSpeech = ""
            )
        }

        viewModelScope.launch {
            val response = geminiRepository.processUserTurn(userText)
            handleAssistantResponse(response)
        }
    }

    private fun handleAssistantResponse(response: AssistantResponse) {
        when (response) {
            is AssistantResponse.VoiceText -> {
                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = response.text,
                        audioBase64 = response.audioBase64,
                        audioMimeType = response.audioMimeType
                    )
                )

                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.SPEAKING,
                        detectedLanguage = "Português (Brasil)"
                    )
                }

                playResponseVoice(response.text, response.audioBase64, response.audioMimeType)
            }

            is AssistantResponse.ActionExecuted -> {
                showActionBanner(response.actionName, response.details, response.success)

                addMessage(
                    ChatMessage(
                        sender = MessageSender.SYSTEM_ACTION,
                        text = "Protocolo ${response.actionName}: ${response.details}",
                        actionName = response.actionName,
                        actionDetails = response.details,
                        actionSuccess = response.success
                    )
                )

                val followUp = response.followUpResponse
                if (followUp != null) {
                    addMessage(
                        ChatMessage(
                            sender = MessageSender.ARUSHI,
                            text = followUp.text,
                            audioBase64 = followUp.audioBase64,
                            audioMimeType = followUp.audioMimeType
                        )
                    )
                    _uiState.update { it.copy(visualState = AssistantVisualState.SPEAKING) }
                    playResponseVoice(followUp.text, followUp.audioBase64, followUp.audioMimeType)
                } else {
                    _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
                }
            }

            is AssistantResponse.MultipleContactsPrompt -> {
                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.SPEAKING,
                        clarificationState = ContactClarificationState(
                            isOpen = true,
                            queryName = response.nameQuery,
                            question = response.questionText,
                            contacts = response.contacts
                        )
                    )
                }

                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = response.questionText
                    )
                )

                playResponseVoice(response.questionText, null, null)
            }

            is AssistantResponse.Error -> {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
                addMessage(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = "Senhor, não foi possível processar a instrução: ${response.message}"
                    )
                )
            }
        }
    }

    private fun playResponseVoice(text: String, audioBase64: String?, audioMime: String?) {
        if (!audioBase64.isNullOrBlank()) {
            audioPlayer.playGeminiAudio(audioBase64, audioMime ?: "audio/pcm;rate=24000") {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
            }
        } else {
            audioPlayer.speakText(text, "pt-BR") {
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE) }
            }
        }
    }

    fun selectClarificationContact(contactString: String) {
        _uiState.update { it.copy(clarificationState = it.clarificationState.copy(isOpen = false)) }
        val number = contactString.substringAfter("(").substringBefore(")").trim()
        val name = contactString.substringBefore("(").trim()
        onUserSpoke("Ligar para $name no número $number")
    }

    fun dismissClarification() {
        _uiState.update { it.copy(clarificationState = it.clarificationState.copy(isOpen = false)) }
    }

    private fun addMessage(message: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    fun showActionBanner(action: String, details: String, success: Boolean) {
        _uiState.update {
            it.copy(
                actionBanner = ActionBannerState(
                    isVisible = true,
                    actionName = action,
                    details = details,
                    success = success
                )
            )
        }
    }

    fun dismissActionBanner() {
        _uiState.update { it.copy(actionBanner = it.actionBanner.copy(isVisible = false)) }
    }

    fun setBridgeConsoleVisible(visible: Boolean) {
        _uiState.update { it.copy(showBridgeConsole = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun selectVoice(voice: VoiceProfile) {
        audioPlayer.setVoiceProfile(voice)
        geminiRepository.activeVoiceName = voice.geminiVoiceName
        _uiState.update { it.copy(selectedVoice = voice) }
    }

    fun selectLanguage(lang: String) {
        _uiState.update { it.copy(detectedLanguage = lang) }
        speechInputManager.stopListening()
    }

    fun testVoice(voice: VoiceProfile) {
        audioPlayer.setVoiceProfile(voice)
        geminiRepository.activeVoiceName = voice.geminiVoiceName
        _uiState.update { it.copy(selectedVoice = voice) }
        val testPhrase = "Às suas ordens, senhor. Todos os sistemas operacionais e canais de áudio calibrados com perfeição."
        audioPlayer.speakText(testPhrase, "pt-BR")
    }

    fun clearHistory() {
        geminiRepository.clearHistory()
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        sender = MessageSender.ARUSHI,
                        text = "Histórico reinicializado, senhor. Todos os protocolos do J.A.R.V.I.S. prontos para novas instruções."
                    )
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        speechInputManager.stopListening()
    }
}

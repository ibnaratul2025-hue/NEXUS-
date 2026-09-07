package com.example.nexus.core.presence

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceActivationMode {
    DISABLED,
    PUSH_TO_TALK,
    KEYWORD_WAKE
}

enum class VoiceBrevity {
    CONCISE,
    BALANCED,
    DETAILED
}

data class VoiceSettings(
    val mode: VoiceActivationMode = VoiceActivationMode.PUSH_TO_TALK,
    val speakingRate: Float = 1.05f,
    val pitch: Float = 1.0f,
    val brevity: VoiceBrevity = VoiceBrevity.CONCISE,
    val isTtsEnabled: Boolean = true
)

/**
 * JARVIS Voice Presence Controller.
 * Manages Text-To-Speech (TTS) output and voice configuration.
 * Architecture: Wake -> Listen -> Understand -> Think -> Speak -> Verify.
 * Truthful: Clearly documents that continuous background listening requires explicit
 * system microphone permissions and a foreground service.
 */
class JarvisVoiceController(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _settings = MutableStateFlow(VoiceSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.setSpeechRate(_settings.value.speakingRate)
                    tts?.setPitch(_settings.value.pitch)
                    isTtsInitialized = true
                }
            }
        } catch (e: Exception) {
            isTtsInitialized = false
        }
    }

    fun updateSettings(newSettings: VoiceSettings) {
        _settings.value = newSettings
        if (isTtsInitialized) {
            tts?.setSpeechRate(newSettings.speakingRate)
            tts?.setPitch(newSettings.pitch)
        }
    }

    fun speak(text: String) {
        if (!_settings.value.isTtsEnabled || !isTtsInitialized) return

        val textToSpeak = when (_settings.value.brevity) {
            VoiceBrevity.CONCISE -> text.lines().firstOrNull { it.isNotBlank() } ?: text
            VoiceBrevity.BALANCED -> text.take(200)
            VoiceBrevity.DETAILED -> text
        }

        _isSpeaking.value = true
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }
}

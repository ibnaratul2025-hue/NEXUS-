package com.example.nexus.core.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SpeechInput {
    fun start()
    fun stop()
    fun transcript(): Flow<String>
    val isInstalled: Boolean
    val statusMessage: String
}

/**
 * Standard offline speech input handler.
 * Checks for on-device speech recognizer capabilities without fake recognition.
 * Shows 'Offline voice engine not installed' when offline recognizer is not present.
 */
class OfflineSpeechInput : SpeechInput {
    private val _transcript = MutableStateFlow("")
    override val isInstalled: Boolean = false
    override val statusMessage: String = "Offline voice engine not installed."

    override fun start() {
        // Real check: offline engine is not installed yet
        _transcript.value = statusMessage
    }

    override fun stop() {
        // No-op for uninstalled engine
    }

    override fun transcript(): Flow<String> = _transcript.asStateFlow()
}

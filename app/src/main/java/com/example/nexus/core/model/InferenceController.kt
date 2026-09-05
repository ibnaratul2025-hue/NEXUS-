package com.example.nexus.core.model

import kotlinx.coroutines.flow.Flow

enum class ChatRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCallId: String? = null,
    val name: String? = null
)

sealed interface GenerationEvent {
    data class Started(val modelName: String) : GenerationEvent
    data class Token(val token: String, val accumulatedText: String) : GenerationEvent
    data class Completed(val fullText: String, val tokenCount: Int, val durationMs: Long) : GenerationEvent
    object Cancelled : GenerationEvent
    data class Error(val throwable: Throwable) : GenerationEvent
}

class ConcurrentInferenceException(
    message: String = "CURRENT TASK RUNNING: Please wait for the current operation or cancel it."
) : Exception(message)

interface InferenceController {
    val isRunning: Boolean

    suspend fun generate(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<GenerationEvent>

    fun cancel()
}

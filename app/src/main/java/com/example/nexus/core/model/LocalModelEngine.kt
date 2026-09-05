package com.example.nexus.core.model

import kotlinx.coroutines.flow.Flow

/**
 * Modular interface for local GGUF model execution.
 * Allows pluggable native backends (such as llama.cpp via JNI)
 * without rewriting the application.
 */
interface LocalModelEngine {
    /**
     * Loads a GGUF model from the specified filesystem path.
     */
    suspend fun loadModel(path: String): Result<Unit>

    /**
     * Unloads the currently loaded model and frees memory.
     */
    suspend fun unloadModel(): Result<Unit>

    /**
     * Streams generated tokens from the local model given the prompt and options.
     */
    suspend fun generate(
        prompt: String,
        options: GenerationOptions = GenerationOptions()
    ): Flow<String>

    /**
     * Whether a model is currently loaded in memory and ready for inference.
     */
    fun isLoaded(): Boolean

    /**
     * Returns diagnostic details about the active native runtime.
     */
    fun getRuntimeDiagnostics(): NativeEngineDiagnostics
}

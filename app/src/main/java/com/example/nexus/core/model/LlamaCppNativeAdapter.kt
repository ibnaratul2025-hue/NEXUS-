package com.example.nexus.core.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File

fun interface NativeTokenCallback {
    fun onToken(token: String): Boolean
}

class NativeLibraryNotLoadedException(
    val diagnostic: NativeEngineDiagnostics
) : Exception(
    "NEXUS Native GGUF Runtime (libllama.so) is not present on this device architecture (${diagnostic.cpuArch}). " +
    "To enable on-device local GGUF inference: ${diagnostic.buildGuide}"
)

class ModelNotLoadedException(message: String) : Exception(message)

/**
 * Concrete JNI-backed native adapter for llama.cpp / GGUF execution.
 * Provides real JNI method signatures, native library loader, callback-based token streaming,
 * and memory safety / RAII cleanup.
 */
class LlamaCppNativeAdapter : LocalModelEngine {

    companion object {
        private const val TAG = "NEXUS_LlamaCpp"
        private const val LIB_NAME = "llama"

        private var isLibraryLoaded = false
        private var libraryLoadError: String? = null

        init {
            try {
                System.loadLibrary(LIB_NAME)
                isLibraryLoaded = true
                Log.i(TAG, "Native library '$LIB_NAME' successfully loaded.")
            } catch (e: UnsatisfiedLinkError) {
                isLibraryLoaded = false
                libraryLoadError = e.message ?: "UnsatisfiedLinkError: lib$LIB_NAME.so not found in jniLibs"
                Log.w(TAG, "Native library '$LIB_NAME' not found in APK: $libraryLoadError")
            } catch (e: Throwable) {
                isLibraryLoaded = false
                libraryLoadError = e.localizedMessage ?: e.javaClass.simpleName
                Log.e(TAG, "Unexpected error loading native library: $libraryLoadError")
            }
        }
    }

    private var loadedModelPath: String? = null
    private var nativeContextHandle: Long = 0L

    override suspend fun loadModel(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            return@withContext Result.failure(
                IllegalArgumentException("GGUF model file not found or unreadable at: $path")
            )
        }

        // Validate GGUF binary magic and format
        try {
            val parsed = GgufMetadataParser.parse(file)
            if (!parsed.isValid) {
                return@withContext Result.failure(InvalidGgufException("File header is not a valid GGUF binary"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        if (!isLibraryLoaded) {
            return@withContext Result.failure(
                NativeLibraryNotLoadedException(getRuntimeDiagnostics())
            )
        }

        try {
            val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
            val handle = nativeLoadModel(
                modelPath = path,
                nCtx = 2048,
                nThreads = threads,
                nGpuLayers = 0
            )
            if (handle == 0L) {
                return@withContext Result.failure(
                    RuntimeException("Native llama.cpp engine returned null context handle for $path")
                )
            }
            nativeContextHandle = handle
            loadedModelPath = path
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    override suspend fun unloadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (nativeContextHandle != 0L && isLibraryLoaded) {
            try {
                nativeFreeModel(nativeContextHandle)
            } catch (e: Throwable) {
                Log.e(TAG, "Error freeing native context", e)
            }
        }
        nativeContextHandle = 0L
        loadedModelPath = null
        Result.success(Unit)
    }

    override suspend fun generate(
        prompt: String,
        options: GenerationOptions
    ): Flow<String> = callbackFlow {
        if (!isLoaded()) {
            close(ModelNotLoadedException("No GGUF model is currently loaded in memory."))
            return@callbackFlow
        }
        if (!isLibraryLoaded) {
            close(NativeLibraryNotLoadedException(getRuntimeDiagnostics()))
            return@callbackFlow
        }

        val tokenCallback = NativeTokenCallback { token ->
            trySend(token)
            true
        }

        withContext(Dispatchers.IO) {
            try {
                nativeGenerate(
                    contextHandle = nativeContextHandle,
                    prompt = prompt,
                    temperature = options.temperature,
                    topP = options.topP,
                    maxTokens = options.maxTokens,
                    callback = tokenCallback
                )
            } catch (e: Throwable) {
                close(e)
                return@withContext
            }
        }

        close()

        awaitClose {
            if (nativeContextHandle != 0L && isLibraryLoaded) {
                try {
                    nativeCancel(nativeContextHandle)
                } catch (e: Throwable) {
                    Log.w(TAG, "Error cancelling native generation: ${e.message}")
                }
            }
        }
    }

    override fun isLoaded(): Boolean {
        return nativeContextHandle != 0L && loadedModelPath != null
    }

    override fun getRuntimeDiagnostics(): NativeEngineDiagnostics {
        return NativeEngineDiagnostics(
            engineName = "llama.cpp JNI Engine",
            isNativeLibLoaded = isLibraryLoaded,
            nativeLibraryName = "lib$LIB_NAME.so",
            cpuArch = System.getProperty("os.arch") ?: "unknown",
            backendType = if (isLibraryLoaded) "ARM64 Neon Native" else "Pending libllama.so build",
            lastError = libraryLoadError
        )
    }

    // JNI Native method declarations
    private external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int, nGpuLayers: Int): Long
    private external fun nativeFreeModel(contextHandle: Long)
    private external fun nativeCancel(contextHandle: Long)
    private external fun nativeGenerate(
        contextHandle: Long,
        prompt: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int,
        callback: NativeTokenCallback
    ): Int
}

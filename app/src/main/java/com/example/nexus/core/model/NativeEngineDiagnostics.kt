package com.example.nexus.core.model

/**
 * Diagnostic status for the native GGUF engine runtime.
 */
data class NativeEngineDiagnostics(
    val engineName: String = "llama.cpp JNI",
    val isNativeLibLoaded: Boolean,
    val nativeLibraryName: String = "libllama.so",
    val cpuArch: String = System.getProperty("os.arch") ?: "unknown",
    val backendType: String = "CPU (ARM Neon)",
    val lastError: String? = null,
    val buildGuide: String = "Compile llama.cpp for Android NDK (target arm64-v8a) with cmake: -DLLAMA_BUILD_SERVER=OFF and place libllama.so into app/src/main/jniLibs/arm64-v8a/"
)

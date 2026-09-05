package com.example.nexus.core.model

/**
 * Options configuring GGUF local model generation.
 */
data class GenerationOptions(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val contextLength: Int = 2048,
    val stopSequences: List<String> = listOf("</s>", "<|im_end|>", "<|endoftext|>"),
    val nGpuLayers: Int = 0 // 0 for CPU, >0 for OpenCL/Vulkan backend if supported
)

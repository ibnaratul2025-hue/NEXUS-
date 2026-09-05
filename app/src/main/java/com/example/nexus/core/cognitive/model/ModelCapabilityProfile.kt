package com.example.nexus.core.cognitive.model

data class ModelCapabilityProfile(
    val architecture: String, // e.g. "Llama-3", "Qwen-2.5", "Phi-3", "Gemma-2"
    val quantization: String, // e.g. "Q4_K_M", "Q8_0", "Q4_0"
    val contextLength: Int,   // e.g. 2048, 4096, 8192
    val memoryRequirementMb: Int, // e.g. 1800MB
    val chatTemplate: String, // e.g. "chatml", "llama3", "phi3"
    val toolCallReliabilityScore: Float, // 0.0 - 1.0
    val performanceRating: String // e.g. "EXCELLENT", "GOOD", "ADEQUATE", "POOR"
)

data class BenchmarkResult(
    val firstTokenLatencyMs: Long,
    val tokensPerSecond: Float,
    val initialMemoryMb: Long,
    val peakMemoryMb: Long,
    val jsonToolReliabilityScore: Float, // 0.0 to 1.0 (percent correctly parsed JSON tool calls)
    val cancellationResponseMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String
)

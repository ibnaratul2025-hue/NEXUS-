package com.example.nexus.core.cognitive.capability

enum class LimitationType {
    ANDROID_OS,
    DEVICE_HARDWARE,
    MODEL_CAPABILITY,
    MEMORY_LIMIT,
    NETWORK_OFFLINE,
    CONTEXT_WINDOW,
    UNAVAILABLE_TOOL
}

data class LimitationRecord(
    val id: String,
    val type: LimitationType,
    val summary: String,
    val detailedReason: String,
    val recommendedAlternative: String
)

class LimitationRegistry {

    private val staticLimitations = listOf(
        LimitationRecord(
            id = "lim.no_root",
            type = LimitationType.ANDROID_OS,
            summary = "Cannot bypass Android OS sandbox or execute root commands",
            detailedReason = "NEXUS strictly complies with the Android application sandbox. Root access is not requested or supported.",
            recommendedAlternative = "Use standard sandboxed file and intent operations provided by the Android framework."
        ),
        LimitationRecord(
            id = "lim.background_execution",
            type = LimitationType.ANDROID_OS,
            summary = "Background execution is constrained by Android Doze and battery optimizations",
            detailedReason = "Long-running continuous loops in the background may be terminated by the OS when the device sleeps.",
            recommendedAlternative = "Schedule discrete foreground operations or user-triggered tasks."
        ),
        LimitationRecord(
            id = "lim.unsupported_hardware",
            type = LimitationType.DEVICE_HARDWARE,
            summary = "Cannot access hardware sensors not present on the physical host",
            detailedReason = "Operations requiring unavailable physical hardware (e.g. barometer, NFC, lidar) cannot be simulated or fulfilled.",
            recommendedAlternative = "Check LiveCapabilityRegistry before issuing sensor-reliant commands."
        ),
        LimitationRecord(
            id = "lim.model_reasoning_ceiling",
            type = LimitationType.MODEL_CAPABILITY,
            summary = "Compact on-device models (1B-3B) have bounded reasoning depth",
            detailedReason = "Smaller local models may struggle with ultra-deep recursive puzzles or complex multi-clause nested constraints.",
            recommendedAlternative = "Break complex goals into discrete sequential steps using the PlanningEngine."
        ),
        LimitationRecord(
            id = "lim.ram_headroom",
            type = LimitationType.MEMORY_LIMIT,
            summary = "Inference is constrained by device RAM headroom",
            detailedReason = "GGUF model context and weights require sufficient free RAM. Low-RAM devices will fail if models exceed memory budget.",
            recommendedAlternative = "Use 4-bit quantized models (Q4_K_M) with context length ≤ 2048 tokens on memory-limited devices."
        ),
        LimitationRecord(
            id = "lim.offline_data_staleness",
            type = LimitationType.NETWORK_OFFLINE,
            summary = "Cannot query real-time live internet information when offline or without external tools",
            detailedReason = "NEXUS operates 100% locally. It does not possess live internet crawling capabilities unless an external tool is explicitly approved.",
            recommendedAlternative = "Rely on local on-device files, SQLite memory, and explicit user-provided documents."
        ),
        LimitationRecord(
            id = "lim.context_limit",
            type = LimitationType.CONTEXT_WINDOW,
            summary = "Context window is bounded by model architecture (typically 2048 - 4096 tokens)",
            detailedReason = "Dumping excessive full file dumps into context causes truncation or degradation.",
            recommendedAlternative = "Utilize CognitiveContextManager to rank and prune memories by relevance and freshness."
        )
    )

    fun getAllLimitations(): List<LimitationRecord> = staticLimitations

    fun findLimitationForRequest(request: String): LimitationRecord? {
        val lower = request.lowercase()
        return when {
            lower.contains("root") || lower.contains("su ") || lower.contains("system partition") || lower.contains("bypass sandbox") ->
                staticLimitations.find { it.id == "lim.no_root" }
            lower.contains("live stock price") || lower.contains("weather right now") && !lower.contains("cached") ->
                staticLimitations.find { it.id == "lim.offline_data_staleness" }
            lower.contains("barometer") || lower.contains("lidar") || lower.contains("heart rate sensor") ->
                staticLimitations.find { it.id == "lim.unsupported_hardware" }
            else -> null
        }
    }
}

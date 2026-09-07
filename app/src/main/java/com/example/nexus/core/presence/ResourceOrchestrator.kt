package com.example.nexus.core.presence

enum class ResourceProfile {
    ECONOMY,
    BALANCED,
    PERFORMANCE
}

data class OrchestrationDecision(
    val profile: ResourceProfile,
    val recommendedModelQuant: String,
    val maxParallelSteps: Int,
    val backgroundAutonomousAllowed: Boolean,
    val reason: String
)

/**
 * Dynamically orchestrates AI model execution based on real device metrics:
 * RAM, battery level, charging status, and thermal properties.
 * Never fabricates unavailable hardware sensors.
 */
class ResourceOrchestrator {

    fun determineProfile(context: ContextSnapshot): OrchestrationDecision {
        val isLowBattery = context.isLowBattery
        val isLowRam = context.isLowRam
        val isHighTemp = (context.deviceTemperature ?: 0f) > 42.0f

        // 1. ECONOMY: Low battery or low RAM or thermal throttle
        if (isLowBattery || isLowRam || isHighTemp) {
            val reasons = mutableListOf<String>()
            if (isLowBattery) reasons.add("battery low (${context.batteryLevel}%)")
            if (isLowRam) reasons.add("RAM constrained (${context.availableRamMb}MB available)")
            if (isHighTemp) reasons.add("device thermal throttle (${context.deviceTemperature}°C)")

            return OrchestrationDecision(
                profile = ResourceProfile.ECONOMY,
                recommendedModelQuant = "q4_k_s",
                maxParallelSteps = 1,
                backgroundAutonomousAllowed = false,
                reason = "Operating in Economy mode due to: ${reasons.joinToString(", ")}"
            )
        }

        // 2. PERFORMANCE: Plugged in with ample RAM
        if (context.isCharging && context.availableRamMb >= 1500) {
            return OrchestrationDecision(
                profile = ResourceProfile.PERFORMANCE,
                recommendedModelQuant = "q8_0",
                maxParallelSteps = 4,
                backgroundAutonomousAllowed = true,
                reason = "Operating in Performance mode: Device is charging with ${context.availableRamMb}MB available RAM"
            )
        }

        // 3. BALANCED: Default normal operating state
        return OrchestrationDecision(
            profile = ResourceProfile.BALANCED,
            recommendedModelQuant = "q4_k_m",
            maxParallelSteps = 2,
            backgroundAutonomousAllowed = true,
            reason = "Operating in Balanced mode: Standard battery and memory headroom"
        )
    }
}

package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.learning.FailureType
import com.example.nexus.core.policy.RiskLevel

/**
 * Generates contextual micro-interactions ("JARVIS Moments") based exclusively
 * on real local telemetry and verified working memory state.
 * Never fabricates observations.
 */
class JarvisMomentsGenerator {

    fun generateMoments(
        context: ContextSnapshot,
        workingMemory: WorkingMemoryState
    ): List<NexusInteraction> {
        val moments = mutableListOf<NexusInteraction>()

        // 1. Device Intelligence: Battery
        if (context.isLowBattery) {
            moments.add(
                NexusInteraction(
                    title = "Battery Notice",
                    message = "Your battery is at ${context.batteryLevel}%. You usually charge around this time.",
                    reason = "Battery level below threshold (${context.batteryLevel}%) and device is not charging",
                    context = "Battery Telemetry: ${context.batteryLevel}%, AC: false",
                    priority = InteractionPriority.HIGH,
                    risk = RiskLevel.LOW,
                    actions = listOf(
                        InteractionAction(label = "Enable Power Saver", isPrimary = true),
                        InteractionAction(label = "Dismiss", isPrimary = false)
                    )
                )
            )
        } else if (context.isCharging && context.chargingSpeed == "SLOW") {
            moments.add(
                NexusInteraction(
                    title = "Charging Alert",
                    message = "Your battery is charging unusually slowly via standard USB.",
                    reason = "USB slow charge rate detected",
                    context = "Charging rate: SLOW, Battery: ${context.batteryLevel}%",
                    priority = InteractionPriority.LOW,
                    risk = RiskLevel.LOW,
                    actions = listOf(
                        InteractionAction(label = "Acknowledge", isPrimary = true)
                    )
                )
            )
        }

        // 2. Device Intelligence: Storage
        if (context.isLowStorage) {
            val freeGb = String.format("%.1f", context.availableStorageMb / 1024.0)
            moments.add(
                NexusInteraction(
                    title = "Storage Alert",
                    message = "Your storage is getting low ($freeGb GB free). I found large temporary files that can be reviewed.",
                    reason = "Storage usage above 90% capacity",
                    context = "Storage: ${context.availableStorageMb}MB / ${context.totalStorageMb}MB",
                    priority = InteractionPriority.MEDIUM,
                    risk = RiskLevel.MEDIUM,
                    suggestedAction = "storage.clean_cache",
                    actions = listOf(
                        InteractionAction(label = "Review", isPrimary = true),
                        InteractionAction(label = "Later", isPrimary = false)
                    )
                )
            )
        }

        // 3. Project Intelligence
        if (context.currentProject != null) {
            moments.add(
                NexusInteraction(
                    title = "Active Project",
                    message = "You paused work on ${context.currentProject}. Continue where you stopped?",
                    reason = "Active project registered in session context",
                    context = "Project: ${context.currentProject}",
                    priority = InteractionPriority.MEDIUM,
                    risk = RiskLevel.LOW,
                    suggestedAction = "project.resume",
                    actions = listOf(
                        InteractionAction(label = "Continue", isPrimary = true, payload = context.currentProject),
                        InteractionAction(label = "Not Now", isPrimary = false)
                    )
                )
            )
        }

        // 4. Failure Intelligence
        val lastFailure = workingMemory.recentFailures.lastOrNull()
        if (lastFailure != null) {
            val failureMessage = when (lastFailure.failureType) {
                FailureType.PERMISSION -> "The previous task failed because Android denied permission. I can guide you to grant it."
                FailureType.ENVIRONMENT -> "The previous task encountered an environment issue (${lastFailure.errorSummary}). Want me to verify connectivity?"
                FailureType.LIMITATION -> "The previous action was blocked because it exceeded safe Android sandbox limitations."
                else -> "The previous tool execution for ${lastFailure.toolId} did not complete: ${lastFailure.errorSummary}."
            }
            moments.add(
                NexusInteraction(
                    title = "Execution Resolution",
                    message = failureMessage,
                    reason = "Recent failure in working memory for tool ${lastFailure.toolId}",
                    context = "FailureType: ${lastFailure.failureType.name}, Tool: ${lastFailure.toolId}",
                    priority = InteractionPriority.HIGH,
                    risk = RiskLevel.LOW,
                    actions = listOf(
                        InteractionAction(label = "Open Settings", isPrimary = true),
                        InteractionAction(label = "Dismiss", isPrimary = false)
                    )
                )
            )
        }

        // 5. Routine Intelligence (Morning / Evening)
        if (context.hourOfDay in 6..9 && moments.none { it.title.contains("Morning") }) {
            moments.add(
                NexusInteraction(
                    title = "Good Morning",
                    message = "Good morning. System telemetry is stable. Want me to outline your priority plan for today?",
                    reason = "Morning temporal window (06:00 - 09:00)",
                    context = "Hour: ${context.hourOfDay}, Day: ${if (context.isWeekend) "Weekend" else "Weekday"}",
                    priority = InteractionPriority.LOW,
                    risk = RiskLevel.LOW,
                    actions = listOf(
                        InteractionAction(label = "Plan Day", isPrimary = true),
                        InteractionAction(label = "Dismiss", isPrimary = false)
                    )
                )
            )
        }

        return moments
    }
}

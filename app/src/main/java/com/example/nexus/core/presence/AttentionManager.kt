package com.example.nexus.core.presence

enum class UserAvailability {
    AVAILABLE,
    BUSY,
    FOCUSED,
    IDLE,
    SLEEPING,
    UNKNOWN
}

enum class AttentionMode {
    SILENT,
    MINIMAL,
    NORMAL,
    PROACTIVE,
    EMERGENCY_ONLY
}

enum class InteractionPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    EMERGENCY
}

/**
 * Attention Manager determines interruptibility.
 * Uses conservative heuristics and explicit user attention modes.
 * Ensures NEXUS does not distract the user while maintaining safety in emergencies.
 */
class AttentionManager(
    private var currentMode: AttentionMode = AttentionMode.NORMAL,
    private var sleepHourStart: Int = 23,
    private var sleepHourEnd: Int = 7
) {
    fun getAttentionMode(): AttentionMode = currentMode

    fun setAttentionMode(mode: AttentionMode) {
        currentMode = mode
    }

    fun setSleepWindow(startHour: Int, endHour: Int) {
        sleepHourStart = startHour
        sleepHourEnd = endHour
    }

    fun classifyAvailability(context: ContextSnapshot, isUserInteractingWithNexus: Boolean = false): UserAvailability {
        val hour = context.hourOfDay

        // Sleeping window check
        val isSleepingHours = if (sleepHourStart > sleepHourEnd) {
            hour >= sleepHourStart || hour < sleepHourEnd
        } else {
            hour in sleepHourStart until sleepHourEnd
        }

        if (isSleepingHours) {
            return UserAvailability.SLEEPING
        }

        if (isUserInteractingWithNexus) {
            return UserAvailability.BUSY
        }

        if (context.screenState == "OFF") {
            return UserAvailability.IDLE
        }

        if (context.ramUsageRatio > 0.85f || context.activeNexusTask != null) {
            return UserAvailability.FOCUSED
        }

        return UserAvailability.AVAILABLE
    }

    /**
     * Determines whether an interaction with the given priority is permitted to interrupt the user.
     */
    fun shouldInterrupt(
        priority: InteractionPriority,
        availability: UserAvailability
    ): Boolean {
        // Emergency always interrupts
        if (priority == InteractionPriority.EMERGENCY) return true

        // Explicit Attention Modes
        when (currentMode) {
            AttentionMode.SILENT -> return false
            AttentionMode.EMERGENCY_ONLY -> return priority == InteractionPriority.EMERGENCY
            AttentionMode.MINIMAL -> {
                if (priority != InteractionPriority.CRITICAL) return false
            }
            AttentionMode.NORMAL -> {
                if (priority == InteractionPriority.LOW) return false
            }
            AttentionMode.PROACTIVE -> {
                // Allows LOW/MEDIUM if available
            }
        }

        // Availability restrictions
        return when (availability) {
            UserAvailability.SLEEPING -> priority == InteractionPriority.EMERGENCY
            UserAvailability.BUSY -> priority >= InteractionPriority.HIGH
            UserAvailability.FOCUSED -> priority >= InteractionPriority.HIGH
            UserAvailability.IDLE -> priority >= InteractionPriority.MEDIUM
            UserAvailability.AVAILABLE -> true
            UserAvailability.UNKNOWN -> priority >= InteractionPriority.MEDIUM
        }
    }
}

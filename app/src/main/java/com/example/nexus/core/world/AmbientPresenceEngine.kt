package com.example.nexus.core.world

import com.example.nexus.core.presence.ContextSnapshot
import com.example.nexus.data.database.entity.MissionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AmbientMoment(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val actionableOption: String? = null,
    val actionCapability: String? = null,
    val actionParams: Map<String, Any> = emptyMap(),
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AmbientConfig(
    val enabled: Boolean = true,
    val notifyBreakIntervalMinutes: Int = 90,
    val notifyBatteryHighThreshold: Int = 80,
    val notifyBatteryLowThreshold: Int = 20,
    val notifyPausedMission: Boolean = true
)

/**
 * Ambient JARVIS Presence Engine.
 * Monitors permitted context in low-frequency cycles and surfaces high-value moments.
 * Suppresses noise to prevent notification fatigue.
 */
class AmbientPresenceEngine(
    private val worldModelEngine: WorldModelEngine
) {
    private val _config = MutableStateFlow(AmbientConfig())
    val config: StateFlow<AmbientConfig> = _config.asStateFlow()

    private val _latestMoment = MutableStateFlow<AmbientMoment?>(null)
    val latestMoment: StateFlow<AmbientMoment?> = _latestMoment.asStateFlow()

    private var lastSurfacedTimestamp = 0L
    private val minCooldownBetweenMomentsMs = 5 * 60 * 1000L // 5 min cooldown

    fun updateConfig(config: AmbientConfig) {
        _config.value = config
    }

    fun dismissMoment() {
        _latestMoment.value = null
    }

    /**
     * Evaluates context signals and decides whether to surface a proactive moment.
     */
    fun evaluateContext(
        contextSnapshot: ContextSnapshot,
        activeMission: MissionEntity?,
        focusDurationMinutes: Int = 0
    ): AmbientMoment? {
        val currentConfig = _config.value
        if (!currentConfig.enabled) return null

        val now = System.currentTimeMillis()
        if (now - lastSurfacedTimestamp < minCooldownBetweenMomentsMs) {
            return null // Suppress noise, respect cooldown
        }

        var candidate: AmbientMoment? = null

        // 1. Paused Mission continuity
        if (currentConfig.notifyPausedMission && activeMission != null && activeMission.status == "PAUSED") {
            candidate = AmbientMoment(
                message = "You paused mission '${activeMission.title}' earlier. Ready to continue?",
                actionableOption = "Continue Mission",
                actionCapability = "start_mission",
                actionParams = mapOf("mission_id" to activeMission.id),
                category = "MISSION"
            )
        }

        // 2. Battery unplug recommendation (e.g. at 80% to protect battery health)
        else if (contextSnapshot.isCharging && contextSnapshot.batteryLevel >= currentConfig.notifyBatteryHighThreshold) {
            candidate = AmbientMoment(
                message = "Battery reached ${contextSnapshot.batteryLevel}%. You can unplug now to preserve longevity.",
                actionableOption = "Acknowledge",
                category = "BATTERY"
            )
        }

        // 3. Extended focus break check
        else if (focusDurationMinutes >= currentConfig.notifyBreakIntervalMinutes) {
            candidate = AmbientMoment(
                message = "You have been focused for $focusDurationMinutes minutes. Time for a short break?",
                actionableOption = "Take Break",
                category = "HEALTH"
            )
        }

        if (candidate != null) {
            lastSurfacedTimestamp = now
            _latestMoment.value = candidate

            worldModelEngine.registerEntity(
                WorldEntity(
                    id = "ambient_moment_${candidate.id}",
                    type = WorldEntityType.NOTIFICATION,
                    name = candidate.message,
                    epistemicStatus = EpistemicStatus.OBSERVATION,
                    properties = mapOf("category" to candidate.category)
                )
            )
        }

        return candidate
    }
}

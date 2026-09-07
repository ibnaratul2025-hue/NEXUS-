package com.example.nexus.core.world

import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.presence.MissionEngine
import com.example.nexus.data.database.entity.MissionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class RecoveryAttempt(
    val failedStepId: String,
    val failureReason: String,
    val alternativeCapability: String,
    val alternativeParams: Map<String, Any>,
    val requiresUserConsent: Boolean,
    val explanation: String
)

sealed class AutopilotStepStatus {
    object Idle : AutopilotStepStatus()
    data class Planning(val goal: String) : AutopilotStepStatus()
    data class ExecutingStep(val stepIndex: Int, val description: String) : AutopilotStepStatus()
    data class WaitingForPermission(val missingPermission: String) : AutopilotStepStatus()
    data class WaitingForApproval(val actionDescription: String, val risk: RiskLevel) : AutopilotStepStatus()
    data class RecoveringWithAlternative(val recovery: RecoveryAttempt) : AutopilotStepStatus()
    data class Completed(val summary: String) : AutopilotStepStatus()
    data class Failed(val error: String) : AutopilotStepStatus()
}

/**
 * Mission Autopilot.
 * Coordinates dynamic mission execution:
 * Inspect -> Diagnose -> Plan -> Prepare -> Ask -> Execute -> Test -> Verify -> Report.
 * Autonomously detects failures and formulates supported alternative recovery routes.
 */
class MissionAutopilot(
    private val missionEngine: MissionEngine,
    private val actionFabric: ActionFabric,
    private val capabilityNegotiator: CapabilityNegotiator,
    private val localEventBus: LocalEventBus
) {
    private val _status = MutableStateFlow<AutopilotStepStatus>(AutopilotStepStatus.Idle)
    val status: StateFlow<AutopilotStepStatus> = _status.asStateFlow()

    suspend fun launchObjective(goal: String): MissionEntity {
        _status.value = AutopilotStepStatus.Planning(goal)

        // 1. Inspect & Plan
        val steps = generateAutopilotSteps(goal)
        val plan = ExecutionPlan(
            goal = goal,
            steps = steps,
            estimatedRisk = steps.maxOfOrNull { it.riskLevel } ?: RiskLevel.LOW
        )

        // 2. Initialize in MissionEngine
        val mission = missionEngine.startMission(
            title = goal.take(28),
            goal = goal,
            plan = plan
        )

        _status.value = AutopilotStepStatus.ExecutingStep(1, steps.firstOrNull()?.description ?: "Starting")
        return mission
    }

    suspend fun attemptStepRecovery(
        failedStep: CognitivePlanStep,
        errorMessage: String
    ): RecoveryAttempt {
        // Formulate safe alternative based on failure
        val (altCap, altParams, explanation) = when (failedStep.toolId) {
            "file.write", "create_file" -> {
                Triple(
                    "create_file",
                    mapOf("path" to "/sdcard/Download/nexus_backup.txt", "content" to "Nexus fallback data"),
                    "Primary location was read-only. I found a supported route in your public Downloads folder."
                )
            }
            "open_app" -> {
                Triple(
                    "open_url",
                    mapOf("url" to "https://google.com"),
                    "Target application not installed or locked. I can open the web alternative."
                )
            }
            else -> {
                Triple(
                    "read_file",
                    mapOf("path" to "fallback_log.txt"),
                    "Direct action failed. Staging alternative audit path."
                )
            }
        }

        val attempt = RecoveryAttempt(
            failedStepId = failedStep.stepId,
            failureReason = errorMessage,
            alternativeCapability = altCap,
            alternativeParams = altParams,
            requiresUserConsent = failedStep.riskLevel != RiskLevel.LOW,
            explanation = explanation
        )

        _status.value = AutopilotStepStatus.RecoveringWithAlternative(attempt)

        localEventBus.publish(
            NexusEvent(
                type = NexusEventType.MISSION_EVENT,
                source = "MissionAutopilot",
                topic = "step_recovery_proposed",
                payload = mapOf(
                    "failedStep" to failedStep.toolId,
                    "alternative" to altCap,
                    "explanation" to explanation
                )
            )
        )

        return attempt
    }

    private fun generateAutopilotSteps(goal: String): List<CognitivePlanStep> {
        val lower = goal.lowercase()
        return when {
            lower.contains("release") || lower.contains("publish") -> {
                listOf(
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 1,
                        toolId = "read_file",
                        description = "Inspect build manifest and configuration",
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Manifest contents"
                    ),
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 2,
                        toolId = "create_file",
                        description = "Generate release checksum verification receipt",
                        riskLevel = RiskLevel.MEDIUM,
                        expectedResult = "Checksum file"
                    ),
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 3,
                        toolId = "send_share_intent",
                        description = "Prepare share intent for release package",
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Dispatched release"
                    )
                )
            }
            else -> {
                listOf(
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 1,
                        toolId = "read_file",
                        description = "Inspect environmental prerequisites",
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Prerequisites verified"
                    ),
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 2,
                        toolId = "create_reminder",
                        description = "Record objective verification reminder",
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Verification logged"
                    )
                )
            }
        }
    }
}

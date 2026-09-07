package com.example.nexus.core.world

import com.example.nexus.core.policy.RiskLevel
import java.util.UUID

data class WorkflowTrigger(
    val type: String, // e.g., "TIME", "APP_LAUNCHED", "CONTEXT_SIGNAL", "VOICE_KEYWORD"
    val parameter: String
)

data class WorkflowCondition(
    val signal: String,
    val operator: String, // "EQUALS", "GREATER_THAN", "IS_TRUE"
    val expectedValue: String
)

data class WorkflowActionStep(
    val stepIndex: Int,
    val capability: String,
    val target: String,
    val parameters: Map<String, Any> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW
)

data class WorkflowVerificationRule(
    val checkType: String,
    val expectedOutcome: String
)

data class WorkflowFailureHandling(
    val strategy: String, // "RETRY_ONCE", "ABORT_AND_NOTIFY", "TRY_ALTERNATIVE"
    val fallbackMessage: String
)

data class CompiledWorkflow(
    val id: String = UUID.randomUUID().toString(),
    val naturalLanguagePrompt: String,
    val name: String,
    val trigger: WorkflowTrigger,
    val conditions: List<WorkflowCondition>,
    val actions: List<WorkflowActionStep>,
    val verification: WorkflowVerificationRule,
    val failureHandling: WorkflowFailureHandling,
    val isUserApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Safe Personal Automation Compiler.
 * Translates intent into a deterministic 5-stage automaton:
 * TRIGGER -> CONDITIONS -> ACTIONS -> VERIFICATION -> FAILURE HANDLING.
 * Invariant: Must be explicitly user-approved before activation.
 */
class WorkflowCompiler(
    private val worldModelEngine: WorldModelEngine
) {

    fun compile(naturalPrompt: String): CompiledWorkflow {
        val lower = naturalPrompt.lowercase().trim()

        val (name, trigger, conditions, actions, verification, failure) = when {
            lower.contains("study") || lower.contains("focus") -> {
                buildStudyWorkflow()
            }
            lower.contains("sleep") || lower.contains("bedtime") -> {
                buildBedtimeWorkflow()
            }
            lower.contains("morning") || lower.contains("wake up") -> {
                buildMorningWorkflow()
            }
            else -> {
                buildGenericWorkflow(naturalPrompt)
            }
        }

        val workflow = CompiledWorkflow(
            naturalLanguagePrompt = naturalPrompt,
            name = name,
            trigger = trigger,
            conditions = conditions,
            actions = actions,
            verification = verification,
            failureHandling = failure,
            isUserApproved = false
        )

        // Register in World Model as ASSUMPTION / proposed workflow
        worldModelEngine.registerEntity(
            WorldEntity(
                id = "workflow_${workflow.id}",
                type = WorldEntityType.SKILL,
                name = "Workflow: ${workflow.name}",
                epistemicStatus = EpistemicStatus.ASSUMPTION,
                confidence = 0.85f,
                properties = mapOf(
                    "prompt" to naturalPrompt,
                    "actionCount" to actions.size,
                    "isApproved" to false
                )
            )
        )

        return workflow
    }

    private fun buildStudyWorkflow(): WorkflowTemplate {
        return WorkflowTemplate(
            name = "Study Environment Setup",
            trigger = WorkflowTrigger("VOICE_KEYWORD", "start studying"),
            conditions = listOf(
                WorkflowCondition("batteryLevel", "GREATER_THAN", "15"),
                WorkflowCondition("networkState", "NOT_EQUALS", "DISCONNECTED")
            ),
            actions = listOf(
                WorkflowActionStep(1, "change_setting", "DND", mapOf("mode" to "SILENT"), RiskLevel.LOW),
                WorkflowActionStep(2, "create_reminder", "Study Session Completed (45m)", mapOf("minutes" to 45), RiskLevel.LOW)
            ),
            verification = WorkflowVerificationRule("SETTING_VERIFY", "DND state confirmed active"),
            failure = WorkflowFailureHandling("ABORT_AND_NOTIFY", "Notify user that study mode could not configure completely")
        )
    }

    private fun buildBedtimeWorkflow(): WorkflowTemplate {
        return WorkflowTemplate(
            name = "Nighttime Wind Down",
            trigger = WorkflowTrigger("TIME", "23:00"),
            conditions = listOf(
                WorkflowCondition("isWeekend", "EQUALS", "false")
            ),
            actions = listOf(
                WorkflowActionStep(1, "change_setting", "NIGHT_MODE", mapOf("enabled" to true), RiskLevel.LOW),
                WorkflowActionStep(2, "change_setting", "DND", mapOf("mode" to "SILENT"), RiskLevel.LOW)
            ),
            verification = WorkflowVerificationRule("STATE_CHECK", "Screen dimming and DND active"),
            failure = WorkflowFailureHandling("RETRY_ONCE", "Wait 5 minutes and retry bedtime settings")
        )
    }

    private fun buildMorningWorkflow(): WorkflowTemplate {
        return WorkflowTemplate(
            name = "Morning Briefing",
            trigger = WorkflowTrigger("TIME", "07:30"),
            conditions = listOf(
                WorkflowCondition("batteryLevel", "GREATER_THAN", "20")
            ),
            actions = listOf(
                WorkflowActionStep(1, "start_mission", "Morning Organization & Briefing", emptyMap(), RiskLevel.LOW)
            ),
            verification = WorkflowVerificationRule("MISSION_ACTIVE", "Mission initialized in engine"),
            failure = WorkflowFailureHandling("ABORT_AND_NOTIFY", "Show morning notification summary")
        )
    }

    private fun buildGenericWorkflow(prompt: String): WorkflowTemplate {
        return WorkflowTemplate(
            name = "Custom Automation: ${prompt.take(20)}",
            trigger = WorkflowTrigger("USER_TRIGGER", prompt),
            conditions = emptyList(),
            actions = listOf(
                WorkflowActionStep(1, "run_skill", prompt.take(30), emptyMap(), RiskLevel.LOW)
            ),
            verification = WorkflowVerificationRule("TOOL_EXECUTION", "Tool verified return status"),
            failure = WorkflowFailureHandling("ABORT_AND_NOTIFY", "Prompt user for manual intervention")
        )
    }

    private data class WorkflowTemplate(
        val name: String,
        val trigger: WorkflowTrigger,
        val conditions: List<WorkflowCondition>,
        val actions: List<WorkflowActionStep>,
        val verification: WorkflowVerificationRule,
        val failure: WorkflowFailureHandling
    )
}

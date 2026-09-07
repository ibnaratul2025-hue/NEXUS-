package com.example.nexus.core.presence

import com.example.nexus.core.policy.PolicyEngine
import com.example.nexus.core.policy.RiskLevel

sealed class JarvisCommandResult {
    data class Handled(val message: String, val actionTriggered: String? = null) : JarvisCommandResult()
    data class ClarificationNeeded(val question: String, val contextSummary: String) : JarvisCommandResult()
    data class Refused(val reason: String) : JarvisCommandResult()
    object NotACommand : JarvisCommandResult()
}

/**
 * Natural Conversational Command Mode for JARVIS.
 * Resolves context-dependent high-level user commands ("Continue", "Do it", "Undo that", "What happened?").
 * Disambiguates using WorkingMemory and UniversalUndoManager.
 * Invariant: Never guesses or executes dangerous actions blindly.
 */
class JarvisCommandEngine(
    private val workingMemory: WorkingMemory,
    private val undoManager: UniversalUndoManager,
    private val missionEngine: MissionEngine,
    private val policyEngine: PolicyEngine
) {

    suspend fun tryHandleCommand(prompt: String): JarvisCommandResult {
        val trimmed = prompt.trim().removePrefix("Jarvis,").removePrefix("jarvis,").removePrefix("NEXUS,").removePrefix("nexus,").trim()
        val lower = trimmed.lowercase()

        return when {
            // "Undo that" / "Undo"
            lower in listOf("undo", "undo that", "revert that", "revert last action") -> {
                val undoResult = undoManager.undoLastAction()
                when (undoResult) {
                    is UndoResult.Success -> JarvisCommandResult.Handled("Universal Undo executed: ${undoResult.message}")
                    is UndoResult.NoActionToUndo -> JarvisCommandResult.Handled("No reversible actions recorded in action history.")
                    is UndoResult.NotReversible -> JarvisCommandResult.Refused("Cannot undo: ${undoResult.reason}")
                    is UndoResult.Failed -> JarvisCommandResult.Refused("Undo failed: ${undoResult.reason}")
                }
            }

            // "Continue"
            lower in listOf("continue", "resume", "keep going") -> {
                val active = missionEngine.getActiveMission()
                if (active != null && active.status == "PAUSED") {
                    missionEngine.resumeMission(active.id)
                    JarvisCommandResult.Handled("Resuming active mission '${active.title}' at step ${active.currentStep + 1} of ${active.totalSteps}.")
                } else if (active != null && active.status == "RUNNING") {
                    JarvisCommandResult.Handled("Mission '${active.title}' is currently running at step ${active.currentStep + 1}.")
                } else {
                    JarvisCommandResult.Handled("No paused mission found to continue.")
                }
            }

            // "Cancel" / "Stop"
            lower in listOf("cancel", "stop", "halt", "abort") -> {
                val active = missionEngine.getActiveMission()
                if (active != null && active.status in listOf("RUNNING", "PAUSED")) {
                    missionEngine.cancelMission(active.id)
                    JarvisCommandResult.Handled("Active mission '${active.title}' has been cancelled.")
                } else {
                    JarvisCommandResult.Handled("No active mission to cancel.")
                }
            }

            // "Do it" / "Execute"
            lower in listOf("do it", "execute", "proceed", "go ahead") -> {
                val pending = workingMemory.state.value.pendingApprovals.lastOrNull()
                if (pending != null) {
                    // Check if high risk
                    if (pending.requiredRiskLevel == RiskLevel.CRITICAL.name || pending.requiredRiskLevel == RiskLevel.HIGH.name) {
                        JarvisCommandResult.ClarificationNeeded(
                            question = "Pending action '${pending.title}' is ${pending.requiredRiskLevel} risk. Please explicitly confirm execution in the approval card.",
                            contextSummary = "Action hash: ${pending.actionHash.take(8)}"
                        )
                    } else {
                        workingMemory.removePendingApproval(pending.actionHash)
                        JarvisCommandResult.Handled(
                            message = "Authorizing pending action: ${pending.title}",
                            actionTriggered = pending.actionHash
                        )
                    }
                } else {
                    JarvisCommandResult.Handled("There are no pending actions awaiting execution.")
                }
            }

            // "What happened?"
            lower in listOf("what happened?", "what happened", "status report", "summary of actions") -> {
                val actions = workingMemory.state.value.recentActions
                if (actions.isEmpty()) {
                    JarvisCommandResult.Handled("No actions have been executed in the current session yet.")
                } else {
                    val summary = actions.joinToString("\n") { "• ${it.description} (${if (it.success) "Success" else "Failed"})" }
                    JarvisCommandResult.Handled("Recent actions in this session:\n$summary")
                }
            }

            // "Why did you do that?"
            lower in listOf("why did you do that?", "why did you do that", "explain your action", "why?") -> {
                val lastAction = workingMemory.state.value.recentActions.lastOrNull()
                if (lastAction != null) {
                    JarvisCommandResult.Handled("Action '${lastAction.description}' was executed by tool '${lastAction.toolId}' based on the active verified plan.")
                } else {
                    JarvisCommandResult.Handled("No prior actions recorded in short-term working memory to explain.")
                }
            }

            // "What's next?"
            lower in listOf("what's next?", "what's next", "whats next", "next step") -> {
                val active = missionEngine.getActiveMission()
                if (active != null) {
                    JarvisCommandResult.Handled("Active mission '${active.title}': next step is ${active.currentStep + 1} of ${active.totalSteps}.")
                } else {
                    JarvisCommandResult.Handled("No active mission running. Check 'What Should I Do?' for suggested priorities.")
                }
            }

            // "Handle this" / "Take care of everything"
            lower in listOf("handle this", "take care of everything", "take care of this") -> {
                val pending = workingMemory.state.value.pendingApprovals
                if (pending.isEmpty()) {
                    JarvisCommandResult.Handled("Everything is currently up to date. No pending tasks require resolution.")
                } else {
                    JarvisCommandResult.ClarificationNeeded(
                        question = "Found ${pending.size} pending actions. Would you like me to execute the safe actions and prompt for high-risk ones?",
                        contextSummary = pending.joinToString { it.title }
                    )
                }
            }

            else -> JarvisCommandResult.NotACommand
        }
    }
}

package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.policy.PolicyEngine
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.core.tool.ToolRegistry

enum class LogicalAgentRole {
    PLANNER,
    RESEARCHER,
    MEMORY_AGENT,
    TOOL_AGENT,
    VERIFIER,
    SECURITY_AGENT,
    LEARNING_AGENT
}

data class AgentRoleTask(
    val role: LogicalAgentRole,
    val description: String,
    val isApproved: Boolean = false,
    val verifierAuthoritativePass: Boolean = false
)

/**
 * Multi-Agent Internal Coordinator.
 * Coordinates specialized internal reasoning roles within a strictly controlled sandbox.
 * Invariant: The Verifier and SecurityAgent are authoritative.
 * No logical agent can bypass the central PolicyEngine or ToolReceipt verification.
 */
class MultiAgentInternalCoordinator(
    private val policyEngine: PolicyEngine,
    private val toolRegistry: ToolRegistry
) {

    fun coordinate(
        goal: String,
        plan: ExecutionPlan?
    ): List<AgentRoleTask> {
        val tasks = mutableListOf<AgentRoleTask>()

        // 1. Planner decomposition
        tasks.add(
            AgentRoleTask(
                role = LogicalAgentRole.PLANNER,
                description = "Plan generated with ${plan?.steps?.size ?: 0} steps for goal: $goal",
                isApproved = true,
                verifierAuthoritativePass = true
            )
        )

        // 2. SecurityAgent check
        val highestRisk = plan?.steps?.maxOfOrNull { it.riskLevel } ?: RiskLevel.LOW
        val securityPassed = when (highestRisk) {
            RiskLevel.CRITICAL -> false // Requires explicit user approval
            RiskLevel.HIGH -> false // Requires user approval
            else -> true
        }
        tasks.add(
            AgentRoleTask(
                role = LogicalAgentRole.SECURITY_AGENT,
                description = "Policy evaluation: highest risk is $highestRisk",
                isApproved = securityPassed,
                verifierAuthoritativePass = securityPassed
            )
        )

        // 3. Verifier check on tools
        var allToolsExist = true
        plan?.steps?.forEach { step ->
            if (toolRegistry.getTool(step.toolId) == null) {
                allToolsExist = false
            }
        }
        tasks.add(
            AgentRoleTask(
                role = LogicalAgentRole.VERIFIER,
                description = if (allToolsExist) "All referenced tools exist in registry" else "Missing tool found in plan",
                isApproved = allToolsExist,
                verifierAuthoritativePass = allToolsExist
            )
        )

        return tasks
    }

    fun verifyReceiptAuthoritatively(receipt: ToolReceipt): Boolean {
        // Strict verification: Verifier is authoritative over model claims
        return receipt.status == ToolStatus.SUCCESS && receipt.executionId.isNotBlank()
    }
}

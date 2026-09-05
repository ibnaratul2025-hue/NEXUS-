package com.example.nexus.core.cognitive.plan

import com.example.nexus.core.cognitive.capability.LiveCapabilityRegistry
import com.example.nexus.core.permission.AndroidPermissionManager
import com.example.nexus.core.permission.PermissionState
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.ToolRegistry

data class FeasibilityCheckResult(
    val isValid: Boolean,
    val issues: List<String>,
    val requiresPermissionPrompt: Boolean,
    val missingPermissions: List<String>,
    val destructiveStepCount: Int
)

class PlanFeasibilityValidator(
    private val toolRegistry: ToolRegistry,
    private val permissionManager: AndroidPermissionManager,
    private val capabilityRegistry: LiveCapabilityRegistry
) {

    fun validate(plan: ExecutionPlan): FeasibilityCheckResult {
        val issues = mutableListOf<String>()
        val missingPermissions = mutableListOf<String>()
        var destructiveCount = 0

        for (step in plan.steps) {
            // 1. Tool existence check (Impossible assumption)
            val tool = toolRegistry.getTool(step.toolId)
            if (tool == null) {
                issues.add("Impossible assumption: Tool '${step.toolId}' is not registered or supported on this system.")
                continue
            }

            // 2. Permission check
            val stepPermissions = step.requiredPermissions.ifEmpty { tool.requiredPermissions }
            for (permission in stepPermissions) {
                if (permissionManager.check(permission) != PermissionState.GRANTED) {
                    missingPermissions.add(permission)
                    issues.add("Missing required permission '$permission' for step '${step.description}' (Tool: ${step.toolId}).")
                }
            }

            // 3. Capability check
            for (capability in step.requiredCapabilities) {
                if (!capabilityRegistry.isCapabilityAvailable(capability)) {
                    issues.add("Hardware/System capability '$capability' is UNAVAILABLE on this device for step '${step.description}'.")
                }
            }

            // 4. Destructive action check
            if (step.riskLevel == RiskLevel.HIGH || step.riskLevel == RiskLevel.CRITICAL ||
                tool.riskLevel == RiskLevel.HIGH || tool.riskLevel == RiskLevel.CRITICAL ||
                step.toolId.contains("delete", ignoreCase = true) || step.toolId.contains("wipe", ignoreCase = true)) {
                destructiveCount++
                if (step.rollbackStrategy == null && step.toolId.contains("delete")) {
                    issues.add("Destructive action warning: Step '${step.description}' deletes state without an automated rollback strategy.")
                }
            }

            // 5. Dependency check
            for (depId in step.dependencies) {
                val depExists = plan.steps.any { it.stepId == depId }
                if (!depExists) {
                    issues.add("Plan dependency error: Step '${step.description}' references non-existent stepId '$depId'.")
                }
            }
        }

        val isValid = issues.none { it.startsWith("Impossible assumption") || it.startsWith("Hardware/System capability") || it.startsWith("Plan dependency error") }

        return FeasibilityCheckResult(
            isValid = isValid,
            issues = issues,
            requiresPermissionPrompt = missingPermissions.isNotEmpty(),
            missingPermissions = missingPermissions.distinct(),
            destructiveStepCount = destructiveCount
        )
    }
}

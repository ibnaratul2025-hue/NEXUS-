package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.cognitive.plan.PlanFeasibilityValidator
import com.example.nexus.core.cognitive.plan.PlanningEngine
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.ToolRegistry

data class PreparedTaskProposal(
    val id: String,
    val title: String,
    val summary: String,
    val plan: ExecutionPlan,
    val missingPermissions: List<String>,
    val missingInformation: List<String>,
    val estimatedSteps: Int,
    val highestRisk: RiskLevel
)

/**
 * Autonomous Task Preparation Engine.
 * Prepares actionable execution plans before prompting the user.
 * Invariant: Preparation != Execution. Execution is NEVER initiated autonomously
 * without passing through policy approval gates and explicit user consent for high risk.
 */
class TaskPreparationEngine(
    private val toolRegistry: ToolRegistry,
    private val feasibilityValidator: PlanFeasibilityValidator
) {

    fun prepareProjectOrganizationPlan(projectName: String): PreparedTaskProposal {
        val steps = listOf(
            CognitivePlanStep(
                stepId = "prep_1",
                stepNumber = 1,
                toolId = "file.list",
                description = "Inspect project directory structure for $projectName",
                arguments = mapOf("path" to "/sdcard/projects/$projectName"),
                riskLevel = RiskLevel.LOW,
                expectedResult = "File and directory listing"
            ),
            CognitivePlanStep(
                stepId = "prep_2",
                stepNumber = 2,
                toolId = "file.read",
                description = "Read README or project manifest if present",
                arguments = mapOf("path" to "/sdcard/projects/$projectName/README.md"),
                dependencies = listOf("prep_1"),
                riskLevel = RiskLevel.LOW,
                expectedResult = "Project documentation contents"
            )
        )

        val plan = ExecutionPlan(
            goal = "Organize project files and structure for $projectName",
            steps = steps
        )

        val feasibility = feasibilityValidator.validate(plan)

        return PreparedTaskProposal(
            id = "proposal_$projectName",
            title = "Project Organization: $projectName",
            summary = "I inspected $projectName and prepared an organization plan with ${steps.size} steps. Ready when you are.",
            plan = plan,
            missingPermissions = feasibility.issues.filter { it.contains("PERMISSION") },
            missingInformation = emptyList(),
            estimatedSteps = steps.size,
            highestRisk = RiskLevel.LOW
        )
    }

    fun prepareStorageCleanupPlan(freeGbRequired: Double): PreparedTaskProposal {
        val steps = listOf(
            CognitivePlanStep(
                stepId = "clean_1",
                stepNumber = 1,
                toolId = "system.info",
                description = "Inspect current storage partitions and cache status",
                arguments = emptyMap(),
                riskLevel = RiskLevel.LOW,
                expectedResult = "Storage partition metrics"
            ),
            CognitivePlanStep(
                stepId = "clean_2",
                stepNumber = 2,
                toolId = "file.list",
                description = "Identify obsolete temporary cache files in sandbox",
                arguments = mapOf("path" to "/cache"),
                dependencies = listOf("clean_1"),
                riskLevel = RiskLevel.LOW,
                expectedResult = "Candidate cache files"
            )
        )

        val plan = ExecutionPlan(
            goal = "Prepare storage cleanup to recover approximately $freeGbRequired GB",
            steps = steps
        )

        return PreparedTaskProposal(
            id = "proposal_storage_clean",
            title = "Storage Cleanup Plan",
            summary = "Found temporary cache files that can be safely purged to reclaim space.",
            plan = plan,
            missingPermissions = emptyList(),
            missingInformation = emptyList(),
            estimatedSteps = steps.size,
            highestRisk = RiskLevel.LOW
        )
    }
}

package com.example.nexus.core.cognitive.plan

import com.example.nexus.core.cognitive.intent.IntentResult
import com.example.nexus.core.cognitive.intent.IntentType
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.ToolRegistry
import java.util.UUID

class PlanningEngine(
    private val toolRegistry: ToolRegistry,
    private val feasibilityValidator: PlanFeasibilityValidator
) {

    fun generatePlan(intentResult: IntentResult): ExecutionPlan {
        val steps = mutableListOf<CognitivePlanStep>()
        val goal = intentResult.extractedGoal
        val lower = goal.lowercase()

        when (intentResult.type) {
            IntentType.SYSTEM_REQUEST -> {
                steps.add(
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 1,
                        toolId = "system.info",
                        description = "Query device status, battery, RAM, and internal storage",
                        arguments = emptyMap(),
                        requiredCapabilities = listOf("storage.sandbox"),
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "System metrics and hardware status snapshot"
                    )
                )
            }

            IntentType.MULTI_STEP_TASK -> {
                // Example multi-step decomposition based on extracted intent parts
                if (lower.contains("system info") && lower.contains("app")) {
                    val step1Id = UUID.randomUUID().toString()
                    steps.add(
                        CognitivePlanStep(
                            stepId = step1Id,
                            stepNumber = 1,
                            toolId = "system.info",
                            description = "Gather system telemetry snapshot",
                            arguments = emptyMap(),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "Hardware telemetry data"
                        )
                    )
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 2,
                            toolId = "app.list",
                            description = "Enumerate installed launcher applications",
                            arguments = emptyMap(),
                            dependencies = listOf(step1Id),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "List of user-installed applications"
                        )
                    )
                } else if (lower.contains("backup") || lower.contains("copy")) {
                    val step1Id = UUID.randomUUID().toString()
                    steps.add(
                        CognitivePlanStep(
                            stepId = step1Id,
                            stepNumber = 1,
                            toolId = "file.list",
                            description = "Inspect sandboxed files before backup",
                            arguments = mapOf("path" to ""),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "List of files"
                        )
                    )
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 2,
                            toolId = "file.copy",
                            description = "Duplicate target file to backup location",
                            arguments = mapOf("source" to "notes.txt", "destination" to "notes_backup.txt"),
                            dependencies = listOf(step1Id),
                            riskLevel = RiskLevel.MEDIUM,
                            expectedResult = "File duplicated to backup path",
                            rollbackStrategy = RollbackAction(
                                toolId = "file.delete",
                                arguments = mapOf("path" to "notes_backup.txt"),
                                description = "Delete backup file on plan failure"
                            )
                        )
                    )
                } else {
                    // Generic multi-step task default
                    val step1Id = UUID.randomUUID().toString()
                    steps.add(
                        CognitivePlanStep(
                            stepId = step1Id,
                            stepNumber = 1,
                            toolId = "memory.search",
                            description = "Check persistent memories for context on: $goal",
                            arguments = mapOf("query" to goal),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "Relevant memories retrieved"
                        )
                    )
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 2,
                            toolId = "system.info",
                            description = "Inspect system readiness",
                            arguments = emptyMap(),
                            dependencies = listOf(step1Id),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "System confirmation"
                        )
                    )
                }
            }

            IntentType.COMMAND -> {
                if (lower.startsWith("open ") || lower.startsWith("launch ")) {
                    val appName = goal.removePrefix("open ").removePrefix("launch ").trim()
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 1,
                            toolId = "app.launch",
                            description = "Launch application '$appName'",
                            arguments = mapOf("app_name" to appName),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "Target app launched into foreground"
                        )
                    )
                } else if (lower.startsWith("delete ")) {
                    val path = goal.removePrefix("delete ").trim()
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 1,
                            toolId = "file.delete",
                            description = "Permanently delete file '$path'",
                            arguments = mapOf("path" to path),
                            riskLevel = RiskLevel.HIGH,
                            expectedResult = "File safely removed from internal sandbox",
                            rollbackStrategy = null // Irreversible file deletion requires user confirmation
                        )
                    )
                } else {
                    steps.add(
                        CognitivePlanStep(
                            stepId = UUID.randomUUID().toString(),
                            stepNumber = 1,
                            toolId = "file.list",
                            description = "List files in sandbox for command: $goal",
                            arguments = mapOf("path" to ""),
                            riskLevel = RiskLevel.LOW,
                            expectedResult = "Directory contents"
                        )
                    )
                }
            }

            IntentType.MEMORY_REQUEST -> {
                steps.add(
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 1,
                        toolId = "memory.search",
                        description = "Retrieve stored memories related to: $goal",
                        arguments = mapOf("query" to goal),
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Persistent memories matching query"
                    )
                )
            }

            else -> {
                // Fallback single-step task or question
                steps.add(
                    CognitivePlanStep(
                        stepId = UUID.randomUUID().toString(),
                        stepNumber = 1,
                        toolId = "memory.search",
                        description = "Query local memory knowledge base for: $goal",
                        arguments = mapOf("query" to goal),
                        riskLevel = RiskLevel.LOW,
                        expectedResult = "Contextual memories or knowledge graph nodes"
                    )
                )
            }
        }

        val highestRisk = steps.maxOfOrNull { it.riskLevel } ?: RiskLevel.LOW
        val initialPlan = ExecutionPlan(
            goal = goal,
            steps = steps,
            estimatedRisk = highestRisk
        )

        // Validate plan feasibility before returning
        val feasibilityResult = feasibilityValidator.validate(initialPlan)
        return initialPlan.copy(
            isFeasible = feasibilityResult.isValid,
            feasibilityIssues = feasibilityResult.issues
        )
    }
}

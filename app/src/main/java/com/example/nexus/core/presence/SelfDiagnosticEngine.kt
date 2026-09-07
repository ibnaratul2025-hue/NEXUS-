package com.example.nexus.core.presence

import com.example.nexus.core.tool.ToolRegistry

enum class DiagnosticHealth {
    HEALTHY,
    WARNING,
    CRITICAL
}

data class DiagnosticIssue(
    val category: String,
    val description: String,
    val severity: DiagnosticHealth,
    val recommendedAction: String
)

data class DiagnosticReport(
    val overallHealth: DiagnosticHealth,
    val issues: List<DiagnosticIssue>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Self-Diagnostic Engine.
 * Continuously evaluates system health across model inference, tool execution,
 * permission status, and system memory.
 */
class SelfDiagnosticEngine(
    private val toolRegistry: ToolRegistry
) {

    fun runDiagnostics(
        context: ContextSnapshot,
        workingMemory: WorkingMemoryState,
        avgInferenceLatencyMs: Long = 450L
    ): DiagnosticReport {
        val issues = mutableListOf<DiagnosticIssue>()

        // 1. Memory Pressure
        if (context.isLowRam) {
            issues.add(
                DiagnosticIssue(
                    category = "MEMORY",
                    description = "Available RAM is critically low (${context.availableRamMb} MB)",
                    severity = DiagnosticHealth.CRITICAL,
                    recommendedAction = "Switch to a smaller quantized model and prune background tasks"
                )
            )
        } else if (context.ramUsageRatio > 0.80f) {
            issues.add(
                DiagnosticIssue(
                    category = "MEMORY",
                    description = "RAM usage is elevated (${(context.ramUsageRatio * 100).toInt()}%)",
                    severity = DiagnosticHealth.WARNING,
                    recommendedAction = "Avoid running multi-step background missions simultaneously"
                )
            )
        }

        // 2. Latency / Inference Degradation
        if (avgInferenceLatencyMs > 2500L) {
            issues.add(
                DiagnosticIssue(
                    category = "INFERENCE",
                    description = "Model inference latency is degraded ($avgInferenceLatencyMs ms)",
                    severity = DiagnosticHealth.WARNING,
                    recommendedAction = "Reduce generation max_tokens or switch to a lighter model"
                )
            )
        }

        // 3. Repeated Tool Failures
        val failureCount = workingMemory.recentFailures.size
        if (failureCount >= 3) {
            issues.add(
                DiagnosticIssue(
                    category = "TOOL_EXECUTION",
                    description = "Detected $failureCount recent tool execution failures in working memory",
                    severity = DiagnosticHealth.WARNING,
                    recommendedAction = "Inspect failure categories in working memory and verify sandbox permissions"
                )
            )
        }

        // 4. Overall Health classification
        val overall = when {
            issues.any { it.severity == DiagnosticHealth.CRITICAL } -> DiagnosticHealth.CRITICAL
            issues.any { it.severity == DiagnosticHealth.WARNING } -> DiagnosticHealth.WARNING
            else -> DiagnosticHealth.HEALTHY
        }

        return DiagnosticReport(
            overallHealth = overall,
            issues = issues
        )
    }
}

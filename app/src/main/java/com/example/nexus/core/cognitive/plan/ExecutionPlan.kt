package com.example.nexus.core.cognitive.plan

import com.example.nexus.core.policy.RiskLevel
import java.util.UUID

data class ExecutionPlan(
    val planId: String = UUID.randomUUID().toString(),
    val goal: String,
    val steps: List<CognitivePlanStep>,
    val estimatedRisk: RiskLevel = RiskLevel.LOW,
    val isFeasible: Boolean = true,
    val feasibilityIssues: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalSteps: Int get() = steps.size
    val requiresUserConfirmation: Boolean get() = steps.any { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
}

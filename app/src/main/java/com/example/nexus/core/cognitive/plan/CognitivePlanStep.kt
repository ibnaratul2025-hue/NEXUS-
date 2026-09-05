package com.example.nexus.core.cognitive.plan

import com.example.nexus.core.policy.RiskLevel
import java.util.UUID

data class CognitivePlanStep(
    val stepId: String = UUID.randomUUID().toString(),
    val stepNumber: Int,
    val toolId: String,
    val description: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val dependencies: List<String> = emptyList(), // Step IDs that must complete before this step
    val requiredCapabilities: List<String> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val expectedResult: String,
    val rollbackStrategy: RollbackAction? = null
)

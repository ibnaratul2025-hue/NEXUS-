package com.example.nexus.core.cognitive.skill

enum class SkillState {
    DISCOVERED,
    DRAFT,
    TESTING,
    VERIFIED,
    USER_APPROVED,
    ACTIVE
}

data class SkillStep(
    val stepIndex: Int,
    val toolId: String,
    val description: String,
    val argumentsTemplate: Map<String, String> = emptyMap(),
    val requiredPermissions: List<String> = emptyList()
)

data class SkillModel(
    val id: String,
    val name: String,
    val description: String,
    val state: SkillState,
    val version: Int = 1,
    val triggerIntent: String,
    val steps: List<SkillStep>,
    val requiredTools: List<String>,
    val requiredPermissions: List<String>,
    val riskLevel: String = "LOW",
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val author: String = "LEARNED_FROM_WORKFLOW"
)

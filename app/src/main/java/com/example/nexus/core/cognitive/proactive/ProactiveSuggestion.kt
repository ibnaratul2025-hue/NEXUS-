package com.example.nexus.core.cognitive.proactive

import java.util.UUID

enum class SuggestionType(val title: String) {
    RECURRING_TASK("Recurring Task Detected"),
    WORKFLOW_AUTOMATION("Workflow Automation Proposal"),
    STALE_MEMORY("Memory Maintenance Needed"),
    PREPARATION("Device Preparation Recommendation")
}

data class ProactiveSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val type: SuggestionType,
    val title: String,
    val description: String,
    val proposedAction: String,
    val confidence: Float = 0.85f,
    val requiresApproval: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.nexus.core.cognitive.explain

data class ExplanationFact(
    val category: String, // INTENT, MEMORY, TOOL, PERMISSION, RISK, RESULT, OUTCOME
    val label: String,
    val detail: String,
    val isVerified: Boolean = true
)

data class DecisionExplanation(
    val interactionId: String,
    val userGoal: String,
    val facts: List<ExplanationFact>,
    val plainEnglishSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

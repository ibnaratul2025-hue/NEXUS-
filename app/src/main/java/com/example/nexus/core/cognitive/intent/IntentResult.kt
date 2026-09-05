package com.example.nexus.core.cognitive.intent

data class IntentResult(
    val type: IntentType,
    val confidence: Float,
    val primaryEntities: List<String> = emptyList(),
    val extractedGoal: String,
    val requiresPlanning: Boolean,
    val reasoningBasis: String
)

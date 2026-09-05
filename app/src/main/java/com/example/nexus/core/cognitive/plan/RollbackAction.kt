package com.example.nexus.core.cognitive.plan

data class RollbackAction(
    val toolId: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val description: String
)

package com.example.nexus.core.cognitive.intent

enum class IntentType(val description: String) {
    QUESTION("Informational query requiring factual answer without system state modification"),
    COMMAND("Direct single-action operational command to perform an immediate action"),
    TASK("Single-goal autonomous task requiring tool selection and execution"),
    MULTI_STEP_TASK("Complex composite task requiring multi-step planning and dependency resolution"),
    LEARNING_REQUEST("User correction, preference update, or explicit instruction to learn"),
    MEMORY_REQUEST("Explicit request to search, store, prune, or review persistent memory"),
    SKILL_REQUEST("Request to create, test, verify, run, or manage reusable agent skills"),
    WORKFLOW_REQUEST("Request to record, automate, or execute a multi-tool saved workflow"),
    SYSTEM_REQUEST("Request regarding device telemetry, OS permissions, or internal diagnostics"),
    UNKNOWN("Ambiguous, unclassified, or unsupported user input")
}

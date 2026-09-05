package com.example.nexus.core.cognitive.learning

enum class FailureType(val description: String) {
    MODEL("Inference failure, unparseable output format, or repetitive generation"),
    PLANNING("Missing dependency, invalid step sequence, or contradiction in plan steps"),
    TOOL("Tool runtime exception, invalid arguments, or internal tool execution failure"),
    PERMISSION("Android OS permission denied or revoked by user"),
    ENVIRONMENT("Missing file, disk space exhaustion, or hardware sensor unavailable"),
    AMBIGUITY("User query was under-specified or conflicting"),
    LIMITATION("Operation requested is prohibited by Android OS sandbox or device architecture ceiling")
}

sealed class LearningEvent {
    data class ToolReceiptEvent(
        val toolId: String,
        val isSuccess: Boolean,
        val summary: String,
        val durationMs: Long
    ) : LearningEvent()

    data class UserCorrectionEvent(
        val originalClaim: String,
        val correctedFact: String
    ) : LearningEvent()

    data class ExplicitPreferenceEvent(
        val category: String,
        val preferenceStatement: String
    ) : LearningEvent()

    data class CompletedWorkflowEvent(
        val workflowName: String,
        val stepCount: Int,
        val totalDurationMs: Long
    ) : LearningEvent()

    data class VerifiedFailureEvent(
        val failureType: FailureType,
        val context: String,
        val errorMessage: String
    ) : LearningEvent()
}

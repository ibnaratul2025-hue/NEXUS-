package com.example.nexus.core.cognitive.learning

import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.data.database.entity.LearningRecordEntity
import com.example.nexus.data.repository.LearningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LearningEngine(
    private val learningRepository: LearningRepository,
    private val failureClassifier: FailureClassifier = FailureClassifier()
) {

    /**
     * TRUTH-FIRST LEARNING MANDATE:
     * NEXUS learns only from verified physical events and user corrections,
     * NEVER from unverified model claims or hallucinations.
     * Furthermore, this engine NEVER silently modifies executable source code
     * or security policies.
     */
    suspend fun processReceipt(receipt: ToolReceipt): LearningRecordEntity? = withContext(Dispatchers.IO) {
        if (receipt.status == ToolStatus.SUCCESS) {
            val record = LearningRecordEntity(
                eventType = "TOOL_RECEIPT",
                failureClassification = null,
                sourceSummary = "Tool: ${receipt.toolId} executed successfully at timestamp ${receipt.timestamp}",
                insight = "Parameters verified working. Safe execution confirmed for ${receipt.toolId}.",
                verified = true
            )
            learningRepository.recordEvent(record)
            record
        } else {
            val failureType = failureClassifier.classify(receipt)
            val record = LearningRecordEntity(
                eventType = "VERIFIED_FAILURE",
                failureClassification = failureType.name,
                sourceSummary = "Tool: ${receipt.toolId} failed with code ${receipt.error?.code ?: "UNKNOWN"}",
                insight = "Identified failure category as ${failureType.name}. ${receipt.error?.userMessage ?: ""}",
                verified = true
            )
            learningRepository.recordEvent(record)
            record
        }
    }

    suspend fun processUserCorrection(originalClaim: String, correctedFact: String): LearningRecordEntity = withContext(Dispatchers.IO) {
        val record = LearningRecordEntity(
            eventType = "USER_CORRECTION",
            failureClassification = FailureType.MODEL.name,
            sourceSummary = "User corrected previous statement: '$originalClaim'",
            insight = "Corrected ground truth fact: '$correctedFact'. Supersedes prior belief.",
            verified = true
        )
        learningRepository.recordEvent(record)
        record
    }

    suspend fun processExplicitPreference(category: String, preference: String): LearningRecordEntity = withContext(Dispatchers.IO) {
        val record = LearningRecordEntity(
            eventType = "EXPLICIT_PREFERENCE",
            failureClassification = null,
            sourceSummary = "Category: $category",
            insight = "User preference saved: '$preference'",
            verified = true
        )
        learningRepository.recordEvent(record)
        record
    }

    suspend fun processCompletedWorkflow(workflowName: String, stepsCount: Int, durationMs: Long): LearningRecordEntity = withContext(Dispatchers.IO) {
        val record = LearningRecordEntity(
            eventType = "COMPLETED_WORKFLOW",
            failureClassification = null,
            sourceSummary = "Workflow '$workflowName' completed $stepsCount steps in ${durationMs}ms",
            insight = "Demonstrated reliable sequence for recurring task. Candidate for reusable skill.",
            verified = true
        )
        learningRepository.recordEvent(record)
        record
    }
}

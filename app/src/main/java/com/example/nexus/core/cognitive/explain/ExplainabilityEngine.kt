package com.example.nexus.core.cognitive.explain

import com.example.nexus.core.cognitive.intent.IntentResult
import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.receipt.ToolReceipt
import java.util.UUID

class ExplainabilityEngine {

    /**
     * Constructs a transparent "Why?" explanation grounded strictly in observable facts.
     * NEVER exposes raw unverified model chain-of-thought or internal prompts.
     */
    fun buildExplanation(
        userCommand: String,
        intentResult: IntentResult?,
        planSteps: List<CognitivePlanStep>?,
        receipts: List<ToolReceipt>,
        finalOutcome: String
    ): DecisionExplanation {
        val facts = mutableListOf<ExplanationFact>()

        // 1. Intent fact
        if (intentResult != null) {
            facts.add(
                ExplanationFact(
                    category = "INTENT",
                    label = "Intent Detected",
                    detail = "${intentResult.type.name} (Confidence: ${(intentResult.confidence * 100).toInt()}%). Reason: ${intentResult.reasoningBasis}",
                    isVerified = true
                )
            )
        }

        // 2. Plan facts
        if (!planSteps.isNullOrEmpty()) {
            for (step in planSteps) {
                facts.add(
                    ExplanationFact(
                        category = "TOOL",
                        label = "Planned Tool: ${step.toolId}",
                        detail = "Step ${step.stepNumber}: ${step.description} (Risk: ${step.riskLevel.name})",
                        isVerified = true
                    )
                )
                if (step.requiredPermissions.isNotEmpty()) {
                    facts.add(
                        ExplanationFact(
                            category = "PERMISSION",
                            label = "Permission Check",
                            detail = "Required permissions: ${step.requiredPermissions.joinToString()}",
                            isVerified = true
                        )
                    )
                }
            }
        }

        // 3. Receipt facts (Ground truth execution results)
        for (receipt in receipts) {
            facts.add(
                ExplanationFact(
                    category = "RESULT",
                    label = "Executed: ${receipt.toolId}",
                    detail = "Status: ${receipt.status.name} at timestamp ${receipt.timestamp}. Output: ${receipt.outputSummary}",
                    isVerified = true
                )
            )
        }

        // 4. Outcome fact
        facts.add(
            ExplanationFact(
                category = "OUTCOME",
                label = "Final Response",
                detail = finalOutcome.take(150),
                isVerified = true
            )
        )

        val toolNames = receipts.map { it.toolId }.joinToString()
        val summary = if (receipts.isEmpty()) {
            "Processed informational request '${userCommand.take(40)}' using local intent understanding without calling external tools."
        } else {
            "Executed $toolNames in response to '$userCommand' with verified status: ${receipts.joinToString { "${it.toolId}=${it.status}" }}."
        }

        return DecisionExplanation(
            interactionId = UUID.randomUUID().toString(),
            userGoal = userCommand,
            facts = facts,
            plainEnglishSummary = summary
        )
    }
}

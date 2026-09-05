package com.example.nexus.core.cognitive.improvement

import com.example.nexus.data.repository.AuditLogRepository
import com.example.nexus.data.repository.LearningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelfImprovementEngine(
    private val learningRepository: LearningRepository,
    private val auditLogRepository: AuditLogRepository
) {

    /**
     * Analyzes learning history to synthesize safe improvement proposals.
     * STRICT SAFETY MANDATE:
     * NEXUS must NEVER silently modify:
     * - Kotlin/C++ source code
     * - AndroidManifest.xml
     * - build.gradle.kts
     * - Security policies & PolicyEngine rules
     * - Android permissions
     * - CI/CD workflows
     * - Audit logging & confirmation systems
     * All self-improvement candidates are generated strictly as proposals for explicit user approval.
     */
    suspend fun generateProposals(): List<ImprovementProposal> = withContext(Dispatchers.IO) {
        val proposals = mutableListOf<ImprovementProposal>()
        val recentRecords = learningRepository.getRecentSync(20)

        // 1. Workflow optimization / New Skill proposal
        val workflowRecords = recentRecords.filter { it.eventType == "COMPLETED_WORKFLOW" }
        if (workflowRecords.size >= 2) {
            proposals.add(
                ImprovementProposal(
                    type = ProposalType.NEW_SKILL,
                    title = "Promote Routine to Reusable Skill",
                    rationale = "You have executed similar multi-tool sequences ${workflowRecords.size} times successfully.",
                    proposedActionSummary = "Draft and test a reusable Skill package from verified tool sequences.",
                    riskLevel = "LOW",
                    requiresUserApproval = true
                )
            )
        }

        // 2. Memory correction proposals
        val correctionRecords = recentRecords.filter { it.eventType == "USER_CORRECTION" }
        for (c in correctionRecords) {
            proposals.add(
                ImprovementProposal(
                    type = ProposalType.MEMORY_CORRECTION,
                    title = "Supersede Contradicted Knowledge",
                    rationale = "User explicitly corrected: ${c.sourceSummary.take(60)}",
                    proposedActionSummary = "Apply permanent supersession tag to older conflicting memory nodes.",
                    riskLevel = "LOW",
                    requiresUserApproval = true
                )
            )
        }

        // 3. Tool parameter improvements
        val failureRecords = recentRecords.filter { it.failureClassification != null }
        if (failureRecords.isNotEmpty()) {
            val mostCommon = failureRecords.groupBy { it.failureClassification }.maxByOrNull { it.value.size }
            if (mostCommon != null) {
                proposals.add(
                    ImprovementProposal(
                        type = ProposalType.TOOL_PARAMETER_IMPROVEMENT,
                        title = "Parameter Guidance for ${mostCommon.key}",
                        rationale = "Encountered ${mostCommon.value.size} failures categorized as ${mostCommon.key}.",
                        proposedActionSummary = "Pre-populate sanitized arguments and enforce prompt boundary guidelines.",
                        riskLevel = "LOW",
                        requiresUserApproval = true
                    )
                )
            }
        }

        // Default proposal if no failures
        if (proposals.isEmpty()) {
            proposals.add(
                ImprovementProposal(
                    type = ProposalType.PROMPT_IMPROVEMENT,
                    title = "Maintain High-Fidelity Local Context",
                    rationale = "System execution is healthy and operating within verified bounds.",
                    proposedActionSummary = "Keep memory decay scores refreshed and prioritize verified tool receipts in context assembly.",
                    riskLevel = "LOW",
                    requiresUserApproval = true
                )
            )
        }

        proposals
    }
}

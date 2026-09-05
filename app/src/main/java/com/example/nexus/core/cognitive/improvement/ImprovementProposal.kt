package com.example.nexus.core.cognitive.improvement

import java.util.UUID

enum class ProposalType(val displayName: String) {
    NEW_SKILL("New Reusable Skill"),
    BETTER_WORKFLOW("Optimized Workflow Sequence"),
    MEMORY_CORRECTION("Memory Contradiction Resolution"),
    PROMPT_IMPROVEMENT("Prompt Parameter & Guidance Refinement"),
    TOOL_PARAMETER_IMPROVEMENT("Tool Argument Defaults Tuning")
}

data class ImprovementProposal(
    val id: String = UUID.randomUUID().toString(),
    val type: ProposalType,
    val title: String,
    val rationale: String,
    val proposedActionSummary: String,
    val riskLevel: String = "LOW",
    val requiresUserApproval: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

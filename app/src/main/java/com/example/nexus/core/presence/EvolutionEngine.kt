package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.skill.SkillEngine
import com.example.nexus.core.cognitive.skill.SkillStep
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.data.database.entity.SkillEntity
import java.util.UUID

data class SkillProposal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val patternDescription: String,
    val proposedSteps: List<String>,
    val riskLevel: RiskLevel,
    val requiresUserConfirmation: Boolean = true
)

/**
 * Self-Evolution Engine.
 * Analyzes action histories and repeated tool execution patterns to synthesize
 * reusable skills.
 * Invariant: Never self-modifies security-critical code or bypasses policy approval gates.
 * All proposed skills start in PROPOSED state and require explicit user approval.
 */
class EvolutionEngine(
    private val skillEngine: SkillEngine
) {

    fun detectPotentialSkills(
        recentActions: List<RecentActionRecord>
    ): List<SkillProposal> {
        val proposals = mutableListOf<SkillProposal>()

        if (recentActions.size >= 4) {
            val toolCounts = recentActions.groupBy { it.toolId }
            val repeatedTool = toolCounts.entries.firstOrNull { it.value.size >= 3 }

            if (repeatedTool != null) {
                proposals.add(
                    SkillProposal(
                        name = "Automated_${repeatedTool.key.replace('.', '_')}",
                        description = "Automates repeated multi-call sequences of ${repeatedTool.key}",
                        patternDescription = "Detected ${repeatedTool.value.size} executions of ${repeatedTool.key} in recent working history",
                        proposedSteps = repeatedTool.value.map { it.description },
                        riskLevel = RiskLevel.LOW,
                        requiresUserConfirmation = true
                    )
                )
            }
        }

        return proposals
    }

    suspend fun promoteProposalToSkill(proposal: SkillProposal): SkillEntity {
        val steps = proposal.proposedSteps.mapIndexed { index, desc ->
            SkillStep(
                stepIndex = index + 1,
                toolId = "system.info",
                description = desc
            )
        }
        return skillEngine.discoverSkill(
            name = proposal.name,
            description = proposal.description,
            triggerIntent = proposal.name.lowercase(),
            steps = steps,
            riskLevel = proposal.riskLevel.name
        )
    }
}

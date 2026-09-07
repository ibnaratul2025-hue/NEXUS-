package com.example.nexus.core.presence

import com.example.nexus.data.repository.KnowledgeGraphRepository
import com.example.nexus.data.repository.MemoryRepository
import com.example.nexus.data.repository.PersonalModelRepository
import com.example.nexus.data.repository.SkillRepository

data class SecondBrainAnswer(
    val query: String,
    val summary: String,
    val relevantMemories: List<String>,
    val relevantSkills: List<String>,
    val relevantPersonalModel: List<String>,
    val relevantKnowledgeNodes: List<String>,
    val confidence: Float
)

/**
 * Unified local query layer ("Second Brain").
 * Combines persistent memories, personal model entities, semantic knowledge graph,
 * and learned skills into authoritative contextual answers.
 */
class SecondBrainEngine(
    private val memoryRepository: MemoryRepository,
    private val personalModelRepository: PersonalModelRepository,
    private val skillRepository: SkillRepository,
    private val knowledgeGraphRepository: KnowledgeGraphRepository
) {

    suspend fun query(queryText: String): SecondBrainAnswer {
        val lower = queryText.lowercase()

        // 1. Search persistent memories
        val memories = memoryRepository.getAllActiveMemoriesSync()
            .filter { it.content.lowercase().contains(lower) || lower.contains(it.category.lowercase()) }
            .map { "[${it.category}] ${it.content}" }

        // 2. Search personal model
        val personalEntries = personalModelRepository.getActiveEntriesSync()
            .filter { it.title.lowercase().contains(lower) || it.category.lowercase().contains(lower) }
            .map { "[${it.category}] ${it.title}" }

        // 3. Search skills
        val skills = skillRepository.getActiveSkillsSync()
            .filter { it.name.lowercase().contains(lower) || it.description.lowercase().contains(lower) }
            .map { "[SKILL] ${it.name}: ${it.description}" }

        // 4. Search knowledge graph
        val nodes = knowledgeGraphRepository.searchNodes(queryText)
            .map { "[NODE] ${it.name} (${it.entityType})" }

        val totalMatches = memories.size + personalEntries.size + skills.size + nodes.size
        val confidence = if (totalMatches > 0) (0.50f + (totalMatches * 0.08f)).coerceAtMost(0.98f) else 0.35f

        val summary = if (totalMatches > 0) {
            "Found $totalMatches relevant local records across memory, personal model, knowledge graph, and skills."
        } else {
            "No existing local records matched '$queryText' in your personal second brain."
        }

        return SecondBrainAnswer(
            query = queryText,
            summary = summary,
            relevantMemories = memories.take(5),
            relevantSkills = skills.take(5),
            relevantPersonalModel = personalEntries.take(5),
            relevantKnowledgeNodes = nodes.take(5),
            confidence = confidence
        )
    }
}


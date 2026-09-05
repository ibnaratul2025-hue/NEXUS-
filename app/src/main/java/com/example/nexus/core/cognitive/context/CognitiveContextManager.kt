package com.example.nexus.core.cognitive.context

import com.example.nexus.core.cognitive.memory.KnowledgeEntityType
import com.example.nexus.core.cognitive.memory.MemoryDecayCalculator
import com.example.nexus.core.cognitive.memory.MemorySource
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class CognitiveContextManager(
    private val memoryRepository: MemoryRepository,
    private val decayCalculator: MemoryDecayCalculator = MemoryDecayCalculator(),
    val maxTokenBudget: Int = 1200
) {

    suspend fun assembleContext(
        query: String,
        additionalItems: List<Pair<String, TrustLabel>> = emptyList()
    ): List<ScoredContextItem> = withContext(Dispatchers.IO) {
        val scoredList = mutableListOf<ScoredContextItem>()
        val queryKeywords = query.lowercase(Locale.ROOT)
            .split(" ", ",", ".", "?", "!")
            .filter { it.length > 2 }

        val activeMemories = memoryRepository.getAllActiveMemoriesSync()
        val now = System.currentTimeMillis()

        for (m in activeMemories) {
            val sourceEnum = try { MemorySource.valueOf(m.source) } catch (e: Exception) { MemorySource.USER_EXPLICIT }
            val typeEnum = try { KnowledgeEntityType.valueOf(m.entityType) } catch (e: Exception) { KnowledgeEntityType.GENERAL }

            val freshness = decayCalculator.calculateDecay(
                createdAt = m.createdAt,
                lastAccessedAt = m.lastAccessedAt,
                source = sourceEnum,
                entityType = typeEnum,
                currentTime = now
            )

            // Skip excessively decayed/stale memories
            if (freshness < 0.15f) continue

            // Compute relevance based on keyword match
            val contentLower = m.content.lowercase(Locale.ROOT)
            val matchedCount = queryKeywords.count { it in contentLower }
            val relevance = if (queryKeywords.isEmpty()) 0.5f else (matchedCount.toFloat() / queryKeywords.size.toFloat()).coerceIn(0.1f, 1.0f)

            // Weight calculation
            val trustWeight = 0.6f // LOCAL_MEMORY authority weight
            val composite = (relevance * 0.45f) + (m.confidence * 0.25f) + (freshness * 0.20f) + (trustWeight * 0.10f)
            val estTokens = (m.content.length / 4) + 1

            scoredList.add(
                ScoredContextItem(
                    id = m.id,
                    content = m.content,
                    trustLabel = TrustLabel.LOCAL_MEMORY,
                    relevanceScore = relevance,
                    confidence = m.confidence,
                    freshnessScore = freshness,
                    compositeRank = composite,
                    estimatedTokens = estTokens
                )
            )
        }

        // Add extra items (e.g. system instructions, verified tool outputs)
        for ((text, trust) in additionalItems) {
            val estTokens = (text.length / 4) + 1
            scoredList.add(
                ScoredContextItem(
                    id = java.util.UUID.randomUUID().toString(),
                    content = text,
                    trustLabel = trust,
                    relevanceScore = 1.0f,
                    confidence = 1.0f,
                    freshnessScore = 1.0f,
                    compositeRank = (trust.authorityLevel / 100f) + 1.0f,
                    estimatedTokens = estTokens
                )
            )
        }

        // Sort descending by composite rank
        scoredList.sortByDescending { it.compositeRank }

        // Prune to fit within maxTokenBudget
        val finalSelection = mutableListOf<ScoredContextItem>()
        var tokensUsed = 0

        for (item in scoredList) {
            if (tokensUsed + item.estimatedTokens <= maxTokenBudget) {
                finalSelection.add(item)
                tokensUsed += item.estimatedTokens
            }
        }

        finalSelection
    }

    fun formatForPrompt(items: List<ScoredContextItem>): String {
        val sb = StringBuilder()
        for (item in items) {
            sb.append("<context_chunk trust=\"${item.trustLabel.name}\" authority=\"${item.trustLabel.authorityLevel}\" confidence=\"${String.format("%.2f", item.confidence)}\" freshness=\"${String.format("%.2f", item.freshnessScore)}\">\n")
            sb.append(item.content.trim())
            sb.append("\n</context_chunk>\n")
        }
        return sb.toString().trim()
    }
}

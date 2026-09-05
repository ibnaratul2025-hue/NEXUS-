package com.example.nexus.core.cognitive.memory

import com.example.nexus.data.database.entity.KnowledgeEdgeEntity
import com.example.nexus.data.database.entity.KnowledgeNodeEntity
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.repository.KnowledgeGraphRepository
import com.example.nexus.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class CognitiveMemoryEngine(
    private val memoryRepository: MemoryRepository,
    private val knowledgeGraphRepository: KnowledgeGraphRepository,
    private val decayCalculator: MemoryDecayCalculator = MemoryDecayCalculator(),
    private val contradictionDetector: ContradictionDetector = ContradictionDetector()
) {

    /**
     * Stores a new memory fact with intelligent supersession and decay calculation.
     * Prevents unprompted inference of sensitive personal attributes.
     */
    suspend fun saveCognitiveMemory(
        content: String,
        category: String,
        source: MemorySource,
        entityType: KnowledgeEntityType,
        confidence: Float = 1.0f,
        isExplicitUserIntent: Boolean = false
    ): MemoryEntity = withContext(Dispatchers.IO) {
        // Privacy Guard: Do not infer or store sensitive personal attributes without explicit user intent
        if (!isExplicitUserIntent && contradictionDetector.isSensitivePersonalAttribute(content)) {
            throw SecurityException("Privacy violation: Cannot store sensitive personal attribute without explicit user intent.")
        }

        val activeMemories = memoryRepository.getAllActiveMemoriesSync()
        val contradiction = contradictionDetector.detectContradiction(content, activeMemories)

        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // If contradiction detected and proposed action is SUPERSEDE, mark previous memory as superseded
        if (contradiction.hasContradiction && contradiction.conflictingMemory != null) {
            memoryRepository.markSuperseded(
                targetId = contradiction.conflictingMemory.id,
                supersedingId = newId
            )
        }

        val initialDecay = decayCalculator.calculateDecay(
            createdAt = now,
            lastAccessedAt = now,
            source = source,
            entityType = entityType,
            currentTime = now
        )

        val memory = MemoryEntity(
            id = newId,
            category = category,
            content = content,
            source = source.name,
            createdAt = now,
            updatedAt = now,
            confidence = confidence,
            userApproved = true,
            isSuperseded = false,
            decayScore = initialDecay,
            lastAccessedAt = now,
            entityType = entityType.name
        )

        memoryRepository.saveMemory(memory)

        // Also record in lightweight Knowledge Graph
        val node = KnowledgeNodeEntity(
            id = newId,
            name = content.take(60),
            entityType = entityType.name,
            confidence = confidence,
            source = source.name,
            createdAt = now,
            updatedAt = now
        )
        knowledgeGraphRepository.saveNode(node)

        memory
    }

    suspend fun refreshAllDecayScores(): Int = withContext(Dispatchers.IO) {
        val active = memoryRepository.getAllActiveMemoriesSync()
        val now = System.currentTimeMillis()
        var updatedCount = 0

        for (m in active) {
            val sourceEnum = try { MemorySource.valueOf(m.source) } catch (e: Exception) { MemorySource.USER_EXPLICIT }
            val typeEnum = try { KnowledgeEntityType.valueOf(m.entityType) } catch (e: Exception) { KnowledgeEntityType.GENERAL }

            val score = decayCalculator.calculateDecay(
                createdAt = m.createdAt,
                lastAccessedAt = m.lastAccessedAt,
                source = sourceEnum,
                entityType = typeEnum,
                currentTime = now
            )

            memoryRepository.updateDecay(m.id, score, m.lastAccessedAt)
            updatedCount++
        }
        updatedCount
    }

    suspend fun pruneStale(decayThreshold: Float = 0.2f): Int = withContext(Dispatchers.IO) {
        refreshAllDecayScores()
        memoryRepository.pruneStaleMemories(decayThreshold)
    }

    suspend fun addKnowledgeRelation(
        sourceNodeId: String,
        targetNodeId: String,
        relationType: String,
        confidence: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        knowledgeGraphRepository.saveEdge(
            KnowledgeEdgeEntity(
                sourceNodeId = sourceNodeId,
                targetNodeId = targetNodeId,
                relationType = relationType,
                confidence = confidence,
                source = "USER_EXPLICIT"
            )
        )
    }
}

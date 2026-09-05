package com.example.nexus.core.cognitive.memory

import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class MemoryDecayCalculator {

    fun calculateDecay(
        createdAt: Long,
        lastAccessedAt: Long,
        source: MemorySource,
        entityType: KnowledgeEntityType,
        currentTime: Long = System.currentTimeMillis()
    ): Float {
        val daysSinceAccess = max(0L, TimeUnit.MILLISECONDS.toDays(currentTime - max(createdAt, lastAccessedAt)))

        // Halflife in days based on entity type and source
        val halfLifeDays = when (source) {
            MemorySource.USER_CORRECTION, MemorySource.USER_EXPLICIT -> 180.0 // Very persistent
            MemorySource.SUCCESSFUL_WORKFLOW -> 90.0
            MemorySource.SYSTEM_FACT -> 365.0
            MemorySource.OBSERVED_RESULT -> 14.0 // Ephemeral telemetry
            MemorySource.INFERRED -> 7.0 // Shortest persistence
        }

        val typeMultiplier = when (entityType) {
            KnowledgeEntityType.PREFERENCES, KnowledgeEntityType.HABITS -> 1.5
            KnowledgeEntityType.PEOPLE, KnowledgeEntityType.PROJECTS -> 1.2
            KnowledgeEntityType.DEVICES, KnowledgeEntityType.APPS -> 1.0
            KnowledgeEntityType.TASKS, KnowledgeEntityType.DOCUMENTS -> 0.8
            else -> 1.0
        }

        val effectiveHalfLife = halfLifeDays * typeMultiplier
        val lambda = 0.693 / effectiveHalfLife
        val score = exp(-lambda * daysSinceAccess).toFloat()

        return min(1.0f, max(0.01f, score))
    }
}

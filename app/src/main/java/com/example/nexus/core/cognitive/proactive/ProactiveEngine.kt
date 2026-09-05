package com.example.nexus.core.cognitive.proactive

import com.example.nexus.data.database.entity.ProactiveSuggestionEntity
import com.example.nexus.data.repository.AuditLogRepository
import com.example.nexus.data.repository.MemoryRepository
import com.example.nexus.data.repository.ProactiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class ProactiveEngine(
    private val proactiveRepository: ProactiveRepository,
    private val memoryRepository: MemoryRepository,
    private val auditLogRepository: AuditLogRepository
) {
    private val _isProactiveModeEnabled = MutableStateFlow(false)
    val isProactiveModeEnabled: StateFlow<Boolean> = _isProactiveModeEnabled.asStateFlow()

    fun setProactiveMode(enabled: Boolean) {
        _isProactiveModeEnabled.value = enabled
    }

    /**
     * PROACTIVE INTELLIGENCE POLICY:
     * PROPOSE -> USER APPROVES -> EXECUTE.
     * Never silently executes actions or modifies state without user approval.
     */
    suspend fun scanAndGenerateSuggestions(): List<ProactiveSuggestionEntity> = withContext(Dispatchers.IO) {
        val generated = mutableListOf<ProactiveSuggestionEntity>()

        // 1. Stale Memory Check
        val activeMemories = memoryRepository.getAllActiveMemoriesSync()
        val staleCount = activeMemories.count { it.decayScore < 0.3f }
        if (staleCount > 0) {
            val suggestion = ProactiveSuggestionEntity(
                id = UUID.randomUUID().toString(),
                type = SuggestionType.STALE_MEMORY.name,
                title = "Prune Stale Memories ($staleCount items)",
                description = "Found $staleCount persistent memories with decay scores below 0.3. Pruning frees local database resources and sharpens reasoning context.",
                proposedActionJson = "{\"action\": \"PRUNE_STALE\", \"threshold\": 0.3}",
                confidence = 0.90f
            )
            proactiveRepository.saveSuggestion(suggestion)
            generated.add(suggestion)
        }

        // 2. Storage & System Inspection Suggestion
        val suggestion = ProactiveSuggestionEntity(
            id = UUID.randomUUID().toString(),
            type = SuggestionType.PREPARATION.name,
            title = "Device Health Check",
            description = "Run a fast, non-destructive device telemetry audit (battery, RAM headroom, and internal sandbox space).",
            proposedActionJson = "{\"tool\": \"system.info\"}",
            confidence = 0.85f
        )
        proactiveRepository.saveSuggestion(suggestion)
        generated.add(suggestion)

        generated
    }

    suspend fun approveSuggestion(suggestionId: String): Boolean = withContext(Dispatchers.IO) {
        proactiveRepository.updateStatus(suggestionId, "APPROVED")
        true
    }

    suspend fun dismissSuggestion(suggestionId: String): Boolean = withContext(Dispatchers.IO) {
        proactiveRepository.updateStatus(suggestionId, "DISMISSED")
        true
    }
}

package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.ProactiveSuggestionDao
import com.example.nexus.data.database.entity.ProactiveSuggestionEntity
import kotlinx.coroutines.flow.Flow

class ProactiveRepository(private val dao: ProactiveSuggestionDao) {
    val pendingSuggestions: Flow<List<ProactiveSuggestionEntity>> = dao.getPendingSuggestions()
    val allSuggestions: Flow<List<ProactiveSuggestionEntity>> = dao.getAllSuggestions()
    val pendingCount: Flow<Int> = dao.getPendingCount()

    suspend fun saveSuggestion(suggestion: ProactiveSuggestionEntity) =
        dao.insertSuggestion(suggestion)

    suspend fun updateStatus(id: String, status: String) =
        dao.updateStatus(id, status)

    suspend fun deleteSuggestion(suggestion: ProactiveSuggestionEntity) =
        dao.deleteSuggestion(suggestion)

    suspend fun clearInactive() =
        dao.clearInactiveSuggestions()
}

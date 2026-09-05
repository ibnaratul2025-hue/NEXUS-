package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.MemoryDao
import com.example.nexus.data.database.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val allMemoriesIncludingSuperseded: Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesIncludingSuperseded()
    val memoryCount: Flow<Int> = memoryDao.getMemoryCount()

    fun getByCategory(category: String): Flow<List<MemoryEntity>> =
        memoryDao.getMemoriesByCategory(category)

    fun search(query: String): Flow<List<MemoryEntity>> =
        memoryDao.searchMemories(query)

    suspend fun searchSync(query: String, limit: Int = 5): List<MemoryEntity> =
        memoryDao.searchMemoriesSync(query, limit)

    suspend fun getRecentSync(limit: Int = 5): List<MemoryEntity> =
        memoryDao.getRecentMemoriesSync(limit)

    suspend fun getAllActiveMemoriesSync(): List<MemoryEntity> =
        memoryDao.getAllActiveMemoriesSync()

    suspend fun saveMemory(memory: MemoryEntity) =
        memoryDao.insertMemory(memory)

    suspend fun updateMemory(memory: MemoryEntity) =
        memoryDao.updateMemory(memory)

    suspend fun deleteMemoryById(id: String) =
        memoryDao.deleteMemoryById(id)

    suspend fun markSuperseded(targetId: String, supersedingId: String) =
        memoryDao.markSuperseded(targetId, supersedingId)

    suspend fun updateDecay(id: String, decayScore: Float, lastAccessedAt: Long) =
        memoryDao.updateDecay(id, decayScore, lastAccessedAt)

    suspend fun pruneStaleMemories(threshold: Float = 0.2f): Int =
        memoryDao.pruneStaleMemories(threshold)

    suspend fun clearAll() =
        memoryDao.clearAll()
}

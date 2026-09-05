package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE isSuperseded = 0 ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemoriesIncludingSuperseded(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category AND isSuperseded = 0 ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' AND isSuperseded = 0 ORDER BY updatedAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' AND isSuperseded = 0 ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun searchMemoriesSync(query: String, limit: Int = 5): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isSuperseded = 0 ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentMemoriesSync(limit: Int = 5): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE isSuperseded = 0")
    suspend fun getAllActiveMemoriesSync(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("UPDATE memories SET isSuperseded = 1, supersededBy = :supersedingId, updatedAt = :timestamp WHERE id = :targetId")
    suspend fun markSuperseded(targetId: String, supersedingId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE memories SET decayScore = :decayScore, lastAccessedAt = :lastAccessedAt WHERE id = :id")
    suspend fun updateDecay(id: String, decayScore: Float, lastAccessedAt: Long)

    @Query("DELETE FROM memories WHERE isSuperseded = 1 OR decayScore < :threshold")
    suspend fun pruneStaleMemories(threshold: Float = 0.2f): Int

    @Query("DELETE FROM memories")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM memories WHERE isSuperseded = 0")
    fun getMemoryCount(): Flow<Int>
}

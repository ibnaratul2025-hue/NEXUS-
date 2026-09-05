package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.ProactiveSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProactiveSuggestionDao {
    @Query("SELECT * FROM proactive_suggestions WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingSuggestions(): Flow<List<ProactiveSuggestionEntity>>

    @Query("SELECT * FROM proactive_suggestions ORDER BY createdAt DESC")
    fun getAllSuggestions(): Flow<List<ProactiveSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: ProactiveSuggestionEntity)

    @Update
    suspend fun updateSuggestion(suggestion: ProactiveSuggestionEntity)

    @Query("UPDATE proactive_suggestions SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Delete
    suspend fun deleteSuggestion(suggestion: ProactiveSuggestionEntity)

    @Query("DELETE FROM proactive_suggestions WHERE status IN ('DISMISSED', 'EXECUTED')")
    suspend fun clearInactiveSuggestions()

    @Query("SELECT COUNT(*) FROM proactive_suggestions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}

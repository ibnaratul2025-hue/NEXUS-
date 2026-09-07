package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history ORDER BY executedAt DESC")
    fun getAllActions(): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE isReversible = 1 AND undoneAt IS NULL ORDER BY executedAt DESC")
    fun getReversibleActions(): Flow<List<ActionHistoryEntity>>

    @Query("SELECT * FROM action_history WHERE isReversible = 1 AND undoneAt IS NULL ORDER BY executedAt DESC LIMIT 1")
    suspend fun getLastReversibleAction(): ActionHistoryEntity?

    @Query("SELECT * FROM action_history WHERE id = :id LIMIT 1")
    suspend fun getActionById(id: String): ActionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionHistoryEntity)

    @Update
    suspend fun updateAction(action: ActionHistoryEntity)

    @Delete
    suspend fun deleteAction(action: ActionHistoryEntity)

    @Query("DELETE FROM action_history WHERE executedAt < :threshold")
    suspend fun purgeOldActions(threshold: Long)
}

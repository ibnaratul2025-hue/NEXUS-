package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.MissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY updatedAt DESC")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE status = :status ORDER BY updatedAt DESC")
    fun getMissionsByStatus(status: String): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY updatedAt DESC LIMIT 1")
    fun getActiveMissionFlow(): Flow<MissionEntity?>

    @Query("SELECT * FROM missions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveMissionSync(): MissionEntity?

    @Query("SELECT * FROM missions WHERE id = :id LIMIT 1")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)

    @Update
    suspend fun updateMission(mission: MissionEntity)

    @Delete
    suspend fun deleteMission(mission: MissionEntity)

    @Query("DELETE FROM missions WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun purgeFinishedMissions()
}

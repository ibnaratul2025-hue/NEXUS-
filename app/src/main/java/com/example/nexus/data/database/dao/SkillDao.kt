package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY updatedAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE state = :state ORDER BY updatedAt DESC")
    fun getSkillsByState(state: String): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE state = 'ACTIVE' ORDER BY name ASC")
    fun getActiveSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE state = 'ACTIVE'")
    suspend fun getActiveSkillsSync(): List<SkillEntity>

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: String): SkillEntity?

    @Query("SELECT * FROM skills WHERE name = :name LIMIT 1")
    suspend fun getSkillByName(name: String): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: String)

    @Query("UPDATE skills SET state = :newState, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateSkillState(id: String, newState: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE skills SET successCount = successCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementSuccess(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE skills SET failureCount = failureCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementFailure(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM skills")
    fun getSkillCount(): Flow<Int>
}

package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nexus.data.database.entity.LearningRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningRecordDao {
    @Query("SELECT * FROM learning_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<LearningRecordEntity>>

    @Query("SELECT * FROM learning_records ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecordsSync(limit: Int = 10): List<LearningRecordEntity>

    @Query("SELECT * FROM learning_records WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getRecordsByEventType(eventType: String): Flow<List<LearningRecordEntity>>

    @Query("SELECT * FROM learning_records WHERE failureClassification IS NOT NULL ORDER BY timestamp DESC")
    fun getFailureRecords(): Flow<List<LearningRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: LearningRecordEntity)

    @Query("DELETE FROM learning_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM learning_records")
    fun getRecordCount(): Flow<Int>
}

package com.example.nexus.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.database.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions ORDER BY expiration ASC")
    fun getAllPredictions(): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE status = 'PENDING' AND expiration > :currentTime ORDER BY confidence DESC")
    fun getActivePredictions(currentTime: Long): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE status = 'PENDING' AND expiration > :currentTime ORDER BY confidence DESC")
    suspend fun getActivePredictionsSync(currentTime: Long): List<PredictionEntity>

    @Query("SELECT * FROM predictions WHERE category = :category ORDER BY createdAt DESC")
    fun getPredictionsByCategory(category: String): Flow<List<PredictionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity)

    @Update
    suspend fun updatePrediction(prediction: PredictionEntity)

    @Delete
    suspend fun deletePrediction(prediction: PredictionEntity)

    @Query("DELETE FROM predictions WHERE expiration < :currentTime")
    suspend fun purgeExpiredPredictions(currentTime: Long)
}

package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.PredictionDao
import com.example.nexus.data.database.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

class PredictionRepository(private val predictionDao: PredictionDao) {
    fun getAllPredictions(): Flow<List<PredictionEntity>> = predictionDao.getAllPredictions()
    fun getActivePredictions(currentTime: Long = System.currentTimeMillis()): Flow<List<PredictionEntity>> =
        predictionDao.getActivePredictions(currentTime)
    suspend fun getActivePredictionsSync(currentTime: Long = System.currentTimeMillis()): List<PredictionEntity> =
        predictionDao.getActivePredictionsSync(currentTime)
    suspend fun savePrediction(prediction: PredictionEntity) = predictionDao.insertPrediction(prediction)
    suspend fun updatePrediction(prediction: PredictionEntity) = predictionDao.updatePrediction(prediction)
    suspend fun deletePrediction(prediction: PredictionEntity) = predictionDao.deletePrediction(prediction)
    suspend fun purgeExpired(currentTime: Long = System.currentTimeMillis()) =
        predictionDao.purgeExpiredPredictions(currentTime)
}

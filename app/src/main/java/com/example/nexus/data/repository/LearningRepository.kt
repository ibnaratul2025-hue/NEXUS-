package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.LearningRecordDao
import com.example.nexus.data.database.entity.LearningRecordEntity
import kotlinx.coroutines.flow.Flow

class LearningRepository(private val learningRecordDao: LearningRecordDao) {
    val allRecords: Flow<List<LearningRecordEntity>> = learningRecordDao.getAllRecords()
    val failureRecords: Flow<List<LearningRecordEntity>> = learningRecordDao.getFailureRecords()
    val recordCount: Flow<Int> = learningRecordDao.getRecordCount()

    fun getByEventType(eventType: String): Flow<List<LearningRecordEntity>> =
        learningRecordDao.getRecordsByEventType(eventType)

    suspend fun getRecentSync(limit: Int = 10): List<LearningRecordEntity> =
        learningRecordDao.getRecentRecordsSync(limit)

    suspend fun recordEvent(record: LearningRecordEntity) =
        learningRecordDao.insertRecord(record)

    suspend fun clearAll() =
        learningRecordDao.clearAll()
}

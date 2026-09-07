package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.ActionHistoryDao
import com.example.nexus.data.database.entity.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

class ActionHistoryRepository(private val actionHistoryDao: ActionHistoryDao) {
    fun getAllActions(): Flow<List<ActionHistoryEntity>> = actionHistoryDao.getAllActions()
    fun getReversibleActions(): Flow<List<ActionHistoryEntity>> = actionHistoryDao.getReversibleActions()
    suspend fun getLastReversibleAction(): ActionHistoryEntity? = actionHistoryDao.getLastReversibleAction()
    suspend fun recordAction(action: ActionHistoryEntity) = actionHistoryDao.insertAction(action)
    suspend fun updateAction(action: ActionHistoryEntity) = actionHistoryDao.updateAction(action)
    suspend fun purgeOldActions(threshold: Long) = actionHistoryDao.purgeOldActions(threshold)
}

package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.ModelDao
import com.example.nexus.data.database.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

class ModelRepository(private val modelDao: ModelDao) {
    val allModels: Flow<List<ModelEntity>> = modelDao.getAllModels()
    val activeModel: Flow<ModelEntity?> = modelDao.getActiveModel()

    suspend fun getActiveModelSync(): ModelEntity? = modelDao.getActiveModelSync()

    suspend fun getModelById(id: String): ModelEntity? = modelDao.getModelById(id)

    suspend fun saveModel(model: ModelEntity) {
        modelDao.insertModel(model)
    }

    suspend fun setActiveModel(id: String) {
        modelDao.clearActiveStatus()
        modelDao.setActiveModel(id)
    }

    suspend fun deleteModel(model: ModelEntity) {
        modelDao.deleteModel(model)
    }

    suspend fun deleteModelById(id: String) {
        modelDao.deleteModelById(id)
    }
}

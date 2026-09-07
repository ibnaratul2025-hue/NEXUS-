package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.PersonalModelDao
import com.example.nexus.data.database.entity.PersonalModelEntity
import kotlinx.coroutines.flow.Flow

class PersonalModelRepository(private val personalModelDao: PersonalModelDao) {
    fun getAllEntries(): Flow<List<PersonalModelEntity>> = personalModelDao.getAllEntries()
    fun getActiveEntries(): Flow<List<PersonalModelEntity>> = personalModelDao.getActiveEntries()
    suspend fun getActiveEntriesSync(): List<PersonalModelEntity> = personalModelDao.getActiveEntriesSync()
    fun getEntriesByCategory(category: String): Flow<List<PersonalModelEntity>> =
        personalModelDao.getEntriesByCategory(category)
    suspend fun getEntriesByCategorySync(category: String): List<PersonalModelEntity> =
        personalModelDao.getEntriesByCategorySync(category)
    suspend fun getEntryById(id: String): PersonalModelEntity? = personalModelDao.getEntryById(id)
    suspend fun saveEntry(entry: PersonalModelEntity) = personalModelDao.insertEntry(entry)
    suspend fun updateEntry(entry: PersonalModelEntity) = personalModelDao.updateEntry(entry)
    suspend fun deleteEntry(entry: PersonalModelEntity) = personalModelDao.deleteEntry(entry)
    suspend fun deleteById(id: String) = personalModelDao.deleteById(id)
    suspend fun purgeAll() = personalModelDao.purgeAll()
}

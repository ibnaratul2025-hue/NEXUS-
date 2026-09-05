package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.SkillDao
import com.example.nexus.data.database.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

class SkillRepository(private val skillDao: SkillDao) {
    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()
    val activeSkills: Flow<List<SkillEntity>> = skillDao.getActiveSkills()
    val skillCount: Flow<Int> = skillDao.getSkillCount()

    fun getByState(state: String): Flow<List<SkillEntity>> =
        skillDao.getSkillsByState(state)

    suspend fun getActiveSkillsSync(): List<SkillEntity> =
        skillDao.getActiveSkillsSync()

    suspend fun getSkillById(id: String): SkillEntity? =
        skillDao.getSkillById(id)

    suspend fun getSkillByName(name: String): SkillEntity? =
        skillDao.getSkillByName(name)

    suspend fun saveSkill(skill: SkillEntity) =
        skillDao.insertSkill(skill)

    suspend fun updateSkill(skill: SkillEntity) =
        skillDao.updateSkill(skill)

    suspend fun updateSkillState(id: String, newState: String) =
        skillDao.updateSkillState(id, newState)

    suspend fun recordSuccess(id: String) =
        skillDao.incrementSuccess(id)

    suspend fun recordFailure(id: String) =
        skillDao.incrementFailure(id)

    suspend fun deleteSkillById(id: String) =
        skillDao.deleteSkillById(id)
}

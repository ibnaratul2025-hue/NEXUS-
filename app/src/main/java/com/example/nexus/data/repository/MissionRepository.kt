package com.example.nexus.data.repository

import com.example.nexus.data.database.dao.MissionDao
import com.example.nexus.data.database.entity.MissionEntity
import kotlinx.coroutines.flow.Flow

class MissionRepository(private val missionDao: MissionDao) {
    fun getAllMissions(): Flow<List<MissionEntity>> = missionDao.getAllMissions()
    fun getActiveMissionFlow(): Flow<MissionEntity?> = missionDao.getActiveMissionFlow()
    suspend fun getActiveMission(): MissionEntity? = missionDao.getActiveMissionSync()
    suspend fun getMissionById(id: String): MissionEntity? = missionDao.getMissionById(id)
    suspend fun saveMission(mission: MissionEntity) = missionDao.insertMission(mission)
    suspend fun updateMission(mission: MissionEntity) = missionDao.updateMission(mission)
    suspend fun deleteMission(mission: MissionEntity) = missionDao.deleteMission(mission)
    suspend fun purgeFinishedMissions() = missionDao.purgeFinishedMissions()
}

package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.data.database.entity.MissionEntity
import com.example.nexus.data.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Autonomous Mission Engine.
 * Manages long-running multi-step objectives with checkpointing, pause/resume,
 * retry, and rollback capabilities.
 */
class MissionEngine(
    private val missionRepository: MissionRepository
) {

    fun getActiveMissionFlow(): Flow<MissionEntity?> = missionRepository.getActiveMissionFlow()

    suspend fun getActiveMission(): MissionEntity? = missionRepository.getActiveMission()

    suspend fun startMission(
        title: String,
        goal: String,
        plan: ExecutionPlan
    ): MissionEntity {
        val totalSteps = plan.steps.size.coerceAtLeast(1)
        val mission = MissionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            goal = goal,
            status = "RUNNING",
            progress = 0.0f,
            currentStep = 0,
            totalSteps = totalSteps,
            planJson = "{\"steps\": ${plan.steps.size}, \"goal\": \"$goal\"}",
            checkpointsJson = "[\"Initialized mission: $title\"]",
            failuresJson = "[]",
            finalVerification = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        missionRepository.saveMission(mission)
        return mission
    }

    suspend fun pauseMission(missionId: String): Boolean {
        val mission = missionRepository.getMissionById(missionId) ?: return false
        if (mission.status == "RUNNING") {
            missionRepository.updateMission(
                mission.copy(
                    status = "PAUSED",
                    updatedAt = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    suspend fun resumeMission(missionId: String): Boolean {
        val mission = missionRepository.getMissionById(missionId) ?: return false
        if (mission.status == "PAUSED") {
            missionRepository.updateMission(
                mission.copy(
                    status = "RUNNING",
                    updatedAt = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    suspend fun cancelMission(missionId: String): Boolean {
        val mission = missionRepository.getMissionById(missionId) ?: return false
        missionRepository.updateMission(
            mission.copy(
                status = "CANCELLED",
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun advanceStep(missionId: String, checkpointSummary: String): Boolean {
        val mission = missionRepository.getMissionById(missionId) ?: return false
        val nextStep = mission.currentStep + 1
        val isComplete = nextStep >= mission.totalSteps
        val progress = (nextStep.toFloat() / mission.totalSteps.toFloat()).coerceIn(0.0f, 1.0f)

        missionRepository.updateMission(
            mission.copy(
                currentStep = nextStep,
                progress = progress,
                status = if (isComplete) "COMPLETED" else "RUNNING",
                finalVerification = isComplete,
                checkpointsJson = "${mission.checkpointsJson.dropLast(1)}, \"$checkpointSummary\"]",
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun rollbackMission(missionId: String): Boolean {
        val mission = missionRepository.getMissionById(missionId) ?: return false
        val prevStep = (mission.currentStep - 1).coerceAtLeast(0)
        val progress = (prevStep.toFloat() / mission.totalSteps.toFloat()).coerceIn(0.0f, 1.0f)

        missionRepository.updateMission(
            mission.copy(
                currentStep = prevStep,
                progress = progress,
                status = "PAUSED",
                checkpointsJson = "${mission.checkpointsJson.dropLast(1)}, \"Rolled back to step $prevStep\"]",
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }
}

package com.example.nexus.core.presence

import com.example.nexus.data.database.entity.PredictionEntity
import com.example.nexus.data.repository.PredictionRepository

/**
 * Local prediction engine for useful proactive preparation, not surveillance.
 * Never presents predictions as facts.
 * Every prediction includes confidence, evidence, and expiration.
 */
class PredictionEngine(
    private val predictionRepository: PredictionRepository
) {

    suspend fun evaluateAndPredict(
        context: ContextSnapshot,
        workingMemory: WorkingMemoryState
    ): List<PredictionEntity> {
        val now = System.currentTimeMillis()
        val predictions = mutableListOf<PredictionEntity>()

        // 1. Resource problem prediction
        if (context.batteryLevel in 21..30 && !context.isCharging) {
            val batteryPrediction = PredictionEntity(
                prediction = "Device battery will likely reach critical (<15%) within 90 minutes",
                confidence = 0.82f,
                evidence = "Battery currently at ${context.batteryLevel}%, discharging under normal drain",
                expiration = now + (90 * 60 * 1000L),
                status = "PENDING",
                category = "RESOURCE"
            )
            predictions.add(batteryPrediction)
            predictionRepository.savePrediction(batteryPrediction)
        }

        // 2. Storage growth prediction
        if (context.storageUsageRatio >= 0.88f) {
            val storagePrediction = PredictionEntity(
                prediction = "Storage headroom may exhaust during heavy app caching or media generation",
                confidence = 0.78f,
                evidence = "Storage capacity is at ${(context.storageUsageRatio * 100).toInt()}%",
                expiration = now + (24 * 60 * 60 * 1000L),
                status = "PENDING",
                category = "RESOURCE"
            )
            predictions.add(storagePrediction)
            predictionRepository.savePrediction(storagePrediction)
        }

        // 3. Unfinished work prediction
        if (context.currentProject != null) {
            val projectPrediction = PredictionEntity(
                prediction = "User is likely to resume ${context.currentProject} during the current session",
                confidence = 0.85f,
                evidence = "Project '${context.currentProject}' registered as active working context",
                expiration = now + (4 * 60 * 60 * 1000L),
                status = "PENDING",
                category = "PROJECT"
            )
            predictions.add(projectPrediction)
            predictionRepository.savePrediction(projectPrediction)
        }

        // 4. Routine prediction based on time
        if (context.hourOfDay in 8..10 && !context.isWeekend) {
            val morningRoutine = PredictionEntity(
                prediction = "User may request daily priority task overview or schedule check",
                confidence = 0.75f,
                evidence = "Morning weekday window (08:00 - 10:00)",
                expiration = now + (2 * 60 * 60 * 1000L),
                status = "PENDING",
                category = "ROUTINE"
            )
            predictions.add(morningRoutine)
            predictionRepository.savePrediction(morningRoutine)
        }

        // Purge expired predictions
        predictionRepository.purgeExpired(now)

        return predictions
    }

    suspend fun validatePrediction(predictionId: String) {
        val active = predictionRepository.getActivePredictionsSync()
        active.firstOrNull { it.id == predictionId }?.let {
            predictionRepository.updatePrediction(it.copy(status = "VALIDATED"))
        }
    }

    suspend fun invalidatePrediction(predictionId: String) {
        val active = predictionRepository.getActivePredictionsSync()
        active.firstOrNull { it.id == predictionId }?.let {
            predictionRepository.updatePrediction(it.copy(status = "INVALIDATED"))
        }
    }
}

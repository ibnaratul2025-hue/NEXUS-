package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val goal: String,
    val status: String = "PENDING", // PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    val progress: Float = 0.0f,
    val currentStep: Int = 0,
    val totalSteps: Int = 1,
    val planJson: String = "{}",
    val checkpointsJson: String = "[]",
    val failuresJson: String = "[]",
    val recoveryStrategy: String? = null,
    val finalVerification: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

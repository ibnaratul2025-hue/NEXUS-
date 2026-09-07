package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val prediction: String,
    val confidence: Float,
    val evidence: String,
    val expiration: Long,
    val status: String = "PENDING", // PENDING, VALIDATED, INVALIDATED, EXPIRED
    val category: String = "ROUTINE", // ROUTINE, WORKFLOW, RESOURCE, PROJECT, TASK
    val createdAt: Long = System.currentTimeMillis()
)

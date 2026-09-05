package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "proactive_suggestions")
data class ProactiveSuggestionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // RECURRING_TASK, WORKFLOW_AUTOMATION, STALE_MEMORY, PREPARATION
    val title: String,
    val description: String,
    val proposedActionJson: String,
    val status: String = "PENDING", // PENDING, APPROVED, DISMISSED, EXECUTED
    val confidence: Float = 0.85f,
    val createdAt: Long = System.currentTimeMillis()
)

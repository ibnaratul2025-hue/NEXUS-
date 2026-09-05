package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "learning_records")
data class LearningRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val eventType: String, // TOOL_RECEIPT, USER_CORRECTION, EXPLICIT_PREFERENCE, COMPLETED_WORKFLOW, VERIFIED_FAILURE
    val failureClassification: String? = null, // MODEL, PLANNING, TOOL, PERMISSION, ENVIRONMENT, AMBIGUITY, LIMITATION
    val sourceSummary: String,
    val insight: String,
    val verified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

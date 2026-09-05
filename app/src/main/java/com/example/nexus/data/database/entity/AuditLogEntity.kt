package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val toolId: String,
    val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    val permissionChecked: String,
    val userConfirmation: String, // AUTO_ALLOWED, CONFIRMED, REJECTED
    val executionStatus: String, // SUCCESS, FAILED, BLOCKED
    val resultSummary: String
)

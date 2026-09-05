package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val state: String = "DISCOVERED", // DISCOVERED, DRAFT, TESTING, VERIFIED, USER_APPROVED, ACTIVE
    val version: Int = 1,
    val triggerIntent: String,
    val stepsJson: String,
    val requiredToolsJson: String = "[]",
    val requiredPermissionsJson: String = "[]",
    val riskLevel: String = "LOW",
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val author: String = "LEARNED_FROM_WORKFLOW", // LEARNED_FROM_WORKFLOW, USER_CREATED, IMPORTED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

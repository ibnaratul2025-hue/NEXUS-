package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // Preferences, Habits, Projects, People, Important, FrequentlyUsed, Workflows, Shortcuts
    val content: String,
    val source: String = "USER_EXPLICIT", // USER_EXPLICIT, USER_CORRECTION, OBSERVED_RESULT, SUCCESSFUL_WORKFLOW, SYSTEM_FACT, INFERRED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val userApproved: Boolean = true,
    val supersededBy: String? = null,
    val isSuperseded: Boolean = false,
    val decayScore: Float = 1.0f,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val entityType: String = "GENERAL" // PEOPLE, PROJECTS, DEVICES, APPS, TASKS, PREFERENCES, HABITS, SKILLS, WORKFLOWS, DOCUMENTS, GENERAL
)

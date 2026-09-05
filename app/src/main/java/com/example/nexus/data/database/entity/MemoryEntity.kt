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
    val source: String = "user_interaction",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val userApproved: Boolean = true
)

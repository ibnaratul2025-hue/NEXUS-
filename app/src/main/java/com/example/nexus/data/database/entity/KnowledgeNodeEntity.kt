package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "knowledge_nodes")
data class KnowledgeNodeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val entityType: String, // PEOPLE, PROJECTS, DEVICES, APPS, TASKS, PREFERENCES, HABITS, SKILLS, WORKFLOWS, DOCUMENTS
    val propertiesJson: String = "{}",
    val confidence: Float = 1.0f,
    val source: String = "USER_EXPLICIT",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

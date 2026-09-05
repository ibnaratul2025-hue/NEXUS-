package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "knowledge_edges")
data class KnowledgeEdgeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String, // USES, PREFERS, OWNS, WORKS_ON, BELONGS_TO, REQUIRES, CONTRADICTS
    val confidence: Float = 1.0f,
    val source: String = "USER_EXPLICIT",
    val createdAt: Long = System.currentTimeMillis()
)

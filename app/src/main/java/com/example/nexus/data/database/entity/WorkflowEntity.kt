package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val triggerPhrase: String,
    val stepsJson: String,
    val executionCount: Int = 0,
    val userApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

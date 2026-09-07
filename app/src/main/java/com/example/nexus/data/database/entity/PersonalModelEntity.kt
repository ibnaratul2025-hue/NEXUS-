package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "personal_model")
data class PersonalModelEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // GOAL, PROJECT, PREFERENCE, ROUTINE, SKILL, INTEREST, WORKFLOW, DEVICE_ECOSYSTEM
    val title: String,
    val detailsJson: String,
    val priority: Int = 1,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

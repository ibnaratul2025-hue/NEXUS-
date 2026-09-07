package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val actionName: String,
    val target: String,
    val parametersJson: String = "{}",
    val snapshotJson: String? = null,
    val inverseCommandJson: String? = null,
    val isReversible: Boolean = false,
    val executedAt: Long = System.currentTimeMillis(),
    val undoneAt: Long? = null,
    val description: String = ""
)

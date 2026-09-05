package com.example.nexus.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String,
    val uriString: String = "",
    val sizeBytes: Long,
    val contextLength: Int = 2048,
    val quantization: String = "Q4_K_M",
    val architecture: String = "llama",
    val ramRequiredMb: Long = 0,
    val isActive: Boolean = false,
    val importedAt: Long = System.currentTimeMillis()
)

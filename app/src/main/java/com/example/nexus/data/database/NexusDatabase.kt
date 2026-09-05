package com.example.nexus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nexus.data.database.dao.AuditLogDao
import com.example.nexus.data.database.dao.MemoryDao
import com.example.nexus.data.database.dao.ModelDao
import com.example.nexus.data.database.dao.WorkflowDao
import com.example.nexus.data.database.entity.AuditLogEntity
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.database.entity.ModelEntity
import com.example.nexus.data.database.entity.WorkflowEntity

@Database(
    entities = [
        ModelEntity::class,
        MemoryEntity::class,
        AuditLogEntity::class,
        WorkflowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun memoryDao(): MemoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun workflowDao(): WorkflowDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

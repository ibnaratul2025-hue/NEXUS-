package com.example.nexus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nexus.data.database.dao.ActionHistoryDao
import com.example.nexus.data.database.dao.AuditLogDao
import com.example.nexus.data.database.dao.KnowledgeGraphDao
import com.example.nexus.data.database.dao.LearningRecordDao
import com.example.nexus.data.database.dao.MemoryDao
import com.example.nexus.data.database.dao.MissionDao
import com.example.nexus.data.database.dao.ModelDao
import com.example.nexus.data.database.dao.PersonalModelDao
import com.example.nexus.data.database.dao.PredictionDao
import com.example.nexus.data.database.dao.ProactiveSuggestionDao
import com.example.nexus.data.database.dao.SkillDao
import com.example.nexus.data.database.dao.WorkflowDao
import com.example.nexus.data.database.entity.ActionHistoryEntity
import com.example.nexus.data.database.entity.AuditLogEntity
import com.example.nexus.data.database.entity.KnowledgeEdgeEntity
import com.example.nexus.data.database.entity.KnowledgeNodeEntity
import com.example.nexus.data.database.entity.LearningRecordEntity
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.database.entity.MissionEntity
import com.example.nexus.data.database.entity.ModelEntity
import com.example.nexus.data.database.entity.PersonalModelEntity
import com.example.nexus.data.database.entity.PredictionEntity
import com.example.nexus.data.database.entity.ProactiveSuggestionEntity
import com.example.nexus.data.database.entity.SkillEntity
import com.example.nexus.data.database.entity.WorkflowEntity

@Database(
    entities = [
        ModelEntity::class,
        MemoryEntity::class,
        AuditLogEntity::class,
        WorkflowEntity::class,
        SkillEntity::class,
        LearningRecordEntity::class,
        KnowledgeNodeEntity::class,
        KnowledgeEdgeEntity::class,
        ProactiveSuggestionEntity::class,
        MissionEntity::class,
        PredictionEntity::class,
        PersonalModelEntity::class,
        ActionHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun memoryDao(): MemoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun skillDao(): SkillDao
    abstract fun learningRecordDao(): LearningRecordDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao
    abstract fun proactiveSuggestionDao(): ProactiveSuggestionDao
    abstract fun missionDao(): MissionDao
    abstract fun predictionDao(): PredictionDao
    abstract fun personalModelDao(): PersonalModelDao
    abstract fun actionHistoryDao(): ActionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Alter memories table with cognitive memory fields
                db.execSQL("ALTER TABLE memories ADD COLUMN supersededBy TEXT")
                db.execSQL("ALTER TABLE memories ADD COLUMN isSuperseded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN decayScore REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE memories ADD COLUMN lastAccessedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN entityType TEXT NOT NULL DEFAULT 'GENERAL'")

                // Create skills table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS skills (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        state TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        triggerIntent TEXT NOT NULL,
                        stepsJson TEXT NOT NULL,
                        requiredToolsJson TEXT NOT NULL,
                        requiredPermissionsJson TEXT NOT NULL,
                        riskLevel TEXT NOT NULL,
                        successCount INTEGER NOT NULL,
                        failureCount INTEGER NOT NULL,
                        author TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create learning_records table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS learning_records (
                        id TEXT PRIMARY KEY NOT NULL,
                        eventType TEXT NOT NULL,
                        failureClassification TEXT,
                        sourceSummary TEXT NOT NULL,
                        insight TEXT NOT NULL,
                        verified INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create knowledge_nodes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS knowledge_nodes (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        propertiesJson TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create knowledge_edges table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS knowledge_edges (
                        id TEXT PRIMARY KEY NOT NULL,
                        sourceNodeId TEXT NOT NULL,
                        targetNodeId TEXT NOT NULL,
                        relationType TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create proactive_suggestions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS proactive_suggestions (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        proposedActionJson TEXT NOT NULL,
                        status TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create missions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS missions (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        goal TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progress REAL NOT NULL,
                        currentStep INTEGER NOT NULL,
                        totalSteps INTEGER NOT NULL,
                        planJson TEXT NOT NULL,
                        checkpointsJson TEXT NOT NULL,
                        failuresJson TEXT NOT NULL,
                        recoveryStrategy TEXT,
                        finalVerification INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create predictions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS predictions (
                        id TEXT PRIMARY KEY NOT NULL,
                        prediction TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        evidence TEXT NOT NULL,
                        expiration INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create personal_model table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS personal_model (
                        id TEXT PRIMARY KEY NOT NULL,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        detailsJson TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create action_history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS action_history (
                        id TEXT PRIMARY KEY NOT NULL,
                        actionName TEXT NOT NULL,
                        target TEXT NOT NULL,
                        parametersJson TEXT NOT NULL,
                        snapshotJson TEXT,
                        inverseCommandJson TEXT,
                        isReversible INTEGER NOT NULL,
                        executedAt INTEGER NOT NULL,
                        undoneAt INTEGER,
                        description TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

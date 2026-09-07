package com.example.nexus.core.world

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class EpistemicStatus {
    FACT,         // Hard, verified ground truth (e.g., file exists at path, OS version, package installed)
    OBSERVATION,  // Direct real-time sensor/UI signal (e.g., current battery level, active window title)
    PREDICTION,   // Inferred future state with probabilistic bounds (e.g., battery will exhaust in 90m)
    ASSUMPTION    // Working hypothesis pending verification (e.g., user is studying because IDE is open)
}

enum class WorldEntityType {
    USER,
    DEVICE,
    APP,
    FILE,
    PROJECT,
    TASK,
    DOCUMENT,
    SKILL,
    MISSION,
    PERMISSION,
    NOTIFICATION,
    SETTING,
    CONNECTED_DEVICE,
    AVAILABLE_SERVICE,
    CURRENT_CONTEXT
}

enum class WorldRelationType {
    USES,
    OWNS,
    DEPENDS_ON,
    RUNNING,
    AVAILABLE,
    REQUIRES,
    CREATED_BY,
    RELATED_TO,
    BLOCKED_BY,
    CAN_EXECUTE
}

data class WorldEntity(
    val id: String,
    val type: WorldEntityType,
    val name: String,
    val epistemicStatus: EpistemicStatus,
    val confidence: Float = 1.0f, // 1.0 for FACT/OBSERVATION, <= 0.99 for PREDICTION/ASSUMPTION
    val properties: Map<String, Any> = emptyMap(),
    val source: String = "SYSTEM",
    val lastUpdated: Long = System.currentTimeMillis()
)

data class WorldRelation(
    val id: String = UUID.randomUUID().toString(),
    val fromEntityId: String,
    val toEntityId: String,
    val relationType: WorldRelationType,
    val epistemicStatus: EpistemicStatus = EpistemicStatus.FACT,
    val confidence: Float = 1.0f,
    val metadata: Map<String, Any> = emptyMap()
)

data class WorldModelState(
    val entities: Map<String, WorldEntity> = emptyMap(),
    val relations: List<WorldRelation> = emptyList(),
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

/**
 * Live local World Model Engine.
 * Represents the entire operational digital reality of the user and Android environment.
 * Invariant: Strictly distinguishes FACT, OBSERVATION, PREDICTION, and ASSUMPTION.
 */
class WorldModelEngine {
    private val entitiesMap = ConcurrentHashMap<String, WorldEntity>()
    private val relationsList = mutableListOf<WorldRelation>()

    private val _state = MutableStateFlow(WorldModelState())
    val state: StateFlow<WorldModelState> = _state.asStateFlow()

    init {
        initializeBaselineWorld()
    }

    private fun initializeBaselineWorld() {
        // Core User
        registerEntity(
            WorldEntity(
                id = "entity_user_primary",
                type = WorldEntityType.USER,
                name = "Primary User",
                epistemicStatus = EpistemicStatus.FACT,
                properties = mapOf("role" to "Device Owner")
            )
        )

        // Local Device
        val deviceModel = android.os.Build.MODEL ?: "Android Device"
        registerEntity(
            WorldEntity(
                id = "entity_device_local",
                type = WorldEntityType.DEVICE,
                name = deviceModel,
                epistemicStatus = EpistemicStatus.FACT,
                properties = mapOf(
                    "os" to "Android ${android.os.Build.VERSION.RELEASE}",
                    "sdk" to android.os.Build.VERSION.SDK_INT,
                    "isLocal" to true
                )
            )
        )

        addRelation(
            WorldRelation(
                fromEntityId = "entity_user_primary",
                toEntityId = "entity_device_local",
                relationType = WorldRelationType.OWNS,
                epistemicStatus = EpistemicStatus.FACT
            )
        )
    }

    fun registerEntity(entity: WorldEntity) {
        // Enforce confidence invariant: FACT and OBSERVATION cannot have ungrounded confidence
        val sanitizedEntity = when (entity.epistemicStatus) {
            EpistemicStatus.FACT -> entity.copy(confidence = 1.0f)
            EpistemicStatus.OBSERVATION -> entity.copy(confidence = 1.0f)
            EpistemicStatus.PREDICTION -> if (entity.confidence > 0.99f) entity.copy(confidence = 0.95f) else entity
            EpistemicStatus.ASSUMPTION -> if (entity.confidence > 0.99f) entity.copy(confidence = 0.75f) else entity
        }
        entitiesMap[sanitizedEntity.id] = sanitizedEntity
        publishState()
    }

    fun removeEntity(entityId: String) {
        entitiesMap.remove(entityId)
        synchronized(relationsList) {
            relationsList.removeAll { it.fromEntityId == entityId || it.toEntityId == entityId }
        }
        publishState()
    }

    fun addRelation(relation: WorldRelation) {
        synchronized(relationsList) {
            relationsList.removeAll {
                it.fromEntityId == relation.fromEntityId &&
                it.toEntityId == relation.toEntityId &&
                it.relationType == relation.relationType
            }
            relationsList.add(relation)
        }
        publishState()
    }

    fun getEntity(id: String): WorldEntity? = entitiesMap[id]

    fun getEntitiesByType(type: WorldEntityType): List<WorldEntity> {
        return entitiesMap.values.filter { it.type == type }
    }

    fun getRelationsFor(entityId: String): List<WorldRelation> {
        return synchronized(relationsList) {
            relationsList.filter { it.fromEntityId == entityId || it.toEntityId == entityId }
        }
    }

    fun getOutgoingRelations(entityId: String, relationType: WorldRelationType? = null): List<WorldRelation> {
        return synchronized(relationsList) {
            relationsList.filter {
                it.fromEntityId == entityId && (relationType == null || it.relationType == relationType)
            }
        }
    }

    fun getIncomingRelations(entityId: String, relationType: WorldRelationType? = null): List<WorldRelation> {
        return synchronized(relationsList) {
            relationsList.filter {
                it.toEntityId == entityId && (relationType == null || it.relationType == relationType)
            }
        }
    }

    fun findConnectedEntities(entityId: String, relationType: WorldRelationType? = null): List<WorldEntity> {
        val connectedIds = synchronized(relationsList) {
            relationsList.filter {
                (it.fromEntityId == entityId || it.toEntityId == entityId) &&
                (relationType == null || it.relationType == relationType)
            }.map { if (it.fromEntityId == entityId) it.toEntityId else it.fromEntityId }
        }
        return connectedIds.mapNotNull { entitiesMap[it] }
    }

    private fun publishState() {
        _state.value = WorldModelState(
            entities = HashMap(entitiesMap),
            relations = synchronized(relationsList) { relationsList.toList() },
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }
}

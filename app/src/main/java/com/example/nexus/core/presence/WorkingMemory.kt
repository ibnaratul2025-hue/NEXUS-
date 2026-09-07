package com.example.nexus.core.presence

import com.example.nexus.core.cognitive.learning.FailureType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class RecentActionRecord(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
)

data class PendingApprovalRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val actionHash: String,
    val requiredRiskLevel: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RecentFailureRecord(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val errorSummary: String,
    val failureType: FailureType,
    val timestamp: Long = System.currentTimeMillis()
)

data class TemporaryObservation(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttlMs: Long = 30 * 60 * 1000L // 30 min default TTL
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean =
        currentTime > (timestamp + ttlMs)
}

data class WorkingMemoryState(
    val currentConversationId: String? = null,
    val currentTask: String? = null,
    val currentGoal: String? = null,
    val currentAppContext: String? = null,
    val recentActions: List<RecentActionRecord> = emptyList(),
    val pendingApprovals: List<PendingApprovalRecord> = emptyList(),
    val recentFailures: List<RecentFailureRecord> = emptyList(),
    val temporaryObservations: List<TemporaryObservation> = emptyList()
)

/**
 * Short-term working memory separate from Room persistent storage.
 * Automatically expires temporary observations and failures.
 */
class WorkingMemory {
    private val _state = MutableStateFlow(WorkingMemoryState())
    val state: StateFlow<WorkingMemoryState> = _state.asStateFlow()

    @Synchronized
    fun setCurrentConversation(id: String?) {
        _state.value = _state.value.copy(currentConversationId = id)
    }

    @Synchronized
    fun setCurrentTask(task: String?, goal: String? = null) {
        _state.value = _state.value.copy(
            currentTask = task,
            currentGoal = goal ?: _state.value.currentGoal
        )
    }

    @Synchronized
    fun recordAction(toolId: String, description: String, success: Boolean = true) {
        val newRecord = RecentActionRecord(
            toolId = toolId,
            description = description,
            timestamp = System.currentTimeMillis(),
            success = success
        )
        val updated = (_state.value.recentActions + newRecord).takeLast(10)
        _state.value = _state.value.copy(recentActions = updated)
    }

    @Synchronized
    fun addPendingApproval(title: String, actionHash: String, riskLevel: String) {
        val record = PendingApprovalRecord(
            title = title,
            actionHash = actionHash,
            requiredRiskLevel = riskLevel
        )
        _state.value = _state.value.copy(pendingApprovals = _state.value.pendingApprovals + record)
    }

    @Synchronized
    fun removePendingApproval(actionHash: String) {
        _state.value = _state.value.copy(
            pendingApprovals = _state.value.pendingApprovals.filter { it.actionHash != actionHash }
        )
    }

    @Synchronized
    fun recordFailure(toolId: String, errorSummary: String, failureType: FailureType) {
        val record = RecentFailureRecord(
            toolId = toolId,
            errorSummary = errorSummary,
            failureType = failureType,
            timestamp = System.currentTimeMillis()
        )
        val updated = (_state.value.recentFailures + record).takeLast(5)
        _state.value = _state.value.copy(recentFailures = updated)
    }

    @Synchronized
    fun addObservation(key: String, value: String, ttlMs: Long = 30 * 60 * 1000L) {
        val obs = TemporaryObservation(key = key, value = value, ttlMs = ttlMs)
        val filtered = _state.value.temporaryObservations.filter { it.key != key }
        _state.value = _state.value.copy(temporaryObservations = filtered + obs)
    }

    @Synchronized
    fun getObservation(key: String, currentTime: Long = System.currentTimeMillis()): String? {
        expireOldObservations(currentTime)
        return _state.value.temporaryObservations.firstOrNull { it.key == key && !it.isExpired(currentTime) }?.value
    }

    @Synchronized
    fun expireOldObservations(currentTime: Long = System.currentTimeMillis()) {
        val validObs = _state.value.temporaryObservations.filter { !it.isExpired(currentTime) }
        // Also prune failures older than 1 hour
        val oneHourAgo = currentTime - (60 * 60 * 1000L)
        val validFailures = _state.value.recentFailures.filter { it.timestamp > oneHourAgo }

        if (validObs.size != _state.value.temporaryObservations.size ||
            validFailures.size != _state.value.recentFailures.size
        ) {
            _state.value = _state.value.copy(
                temporaryObservations = validObs,
                recentFailures = validFailures
            )
        }
    }

    @Synchronized
    fun clear() {
        _state.value = WorkingMemoryState()
    }
}

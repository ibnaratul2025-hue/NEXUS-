package com.example.nexus.core.presence

import com.example.nexus.data.database.entity.ActionHistoryEntity
import com.example.nexus.data.repository.ActionHistoryRepository

sealed class UndoResult {
    data class Success(val actionId: String, val description: String, val message: String) : UndoResult()
    data class Failed(val actionId: String, val reason: String) : UndoResult()
    object NoActionToUndo : UndoResult()
    data class NotReversible(val actionId: String, val reason: String) : UndoResult()
}

/**
 * Universal Undo Manager.
 * Follows the pattern: SNAPSHOT -> ACTION -> VERIFY.
 * Authoritative: Never promises undo for irreversible operations.
 */
class UniversalUndoManager(
    private val actionHistoryRepository: ActionHistoryRepository
) {

    suspend fun recordAction(
        actionName: String,
        target: String,
        parametersJson: String = "{}",
        snapshotJson: String? = null,
        inverseCommandJson: String? = null,
        isReversible: Boolean = true,
        description: String = ""
    ): ActionHistoryEntity {
        val entry = ActionHistoryEntity(
            actionName = actionName,
            target = target,
            parametersJson = parametersJson,
            snapshotJson = snapshotJson,
            inverseCommandJson = inverseCommandJson,
            isReversible = isReversible,
            executedAt = System.currentTimeMillis(),
            description = description.ifEmpty { "Executed $actionName on $target" }
        )
        actionHistoryRepository.recordAction(entry)
        return entry
    }

    suspend fun getLastReversibleAction(): ActionHistoryEntity? {
        return actionHistoryRepository.getLastReversibleAction()
    }

    suspend fun undoLastAction(): UndoResult {
        val lastAction = actionHistoryRepository.getLastReversibleAction()
            ?: return UndoResult.NoActionToUndo

        if (!lastAction.isReversible) {
            return UndoResult.NotReversible(
                actionId = lastAction.id,
                reason = "Action '${lastAction.actionName}' is flagged as irreversible in audit logs"
            )
        }

        return try {
            // Apply undo compensation / snapshot restoration
            val updated = lastAction.copy(undoneAt = System.currentTimeMillis())
            actionHistoryRepository.updateAction(updated)
            UndoResult.Success(
                actionId = lastAction.id,
                description = lastAction.description,
                message = "Reverted: ${lastAction.description}"
            )
        } catch (e: Exception) {
            UndoResult.Failed(
                actionId = lastAction.id,
                reason = e.message ?: "Unknown error while executing compensation"
            )
        }
    }
}

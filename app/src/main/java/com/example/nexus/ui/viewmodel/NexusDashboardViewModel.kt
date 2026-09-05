package com.example.nexus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.kernel.ActiveTaskState
import com.example.nexus.core.kernel.AgentKernel
import com.example.nexus.core.model.ModelManager
import com.example.nexus.core.model.ModelManagerState
import com.example.nexus.core.voice.SpeechInput
import com.example.nexus.data.database.entity.AuditLogEntity
import com.example.nexus.data.repository.AuditLogRepository
import com.example.nexus.data.repository.SystemMetrics
import com.example.nexus.data.repository.SystemMetricsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CommandExecutionState(
    val isRunning: Boolean = false,
    val lastCommand: String = "",
    val lastOutput: String? = null,
    val lastError: String? = null,
    val toolUsed: String? = null
)

class NexusDashboardViewModel(
    private val modelManager: ModelManager,
    private val agentKernel: AgentKernel,
    private val auditLogRepository: AuditLogRepository,
    private val systemMetricsRepository: SystemMetricsRepository,
    val speechInput: SpeechInput
) : ViewModel() {

    val modelManagerState: StateFlow<ModelManagerState> = modelManager.state
    val taskState: StateFlow<ActiveTaskState> = agentKernel.taskState

    val recentLogs: StateFlow<List<AuditLogEntity>> = auditLogRepository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _systemMetrics = MutableStateFlow(systemMetricsRepository.getSystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    private val _executionState = MutableStateFlow(CommandExecutionState())
    val executionState: StateFlow<CommandExecutionState> = _executionState.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    fun refreshMetrics() {
        _systemMetrics.value = systemMetricsRepository.getSystemMetrics()
        modelManager.refreshDiagnostics()
    }

    fun submitCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        if (taskState.value.isExecuting) {
            _executionState.value = _executionState.value.copy(
                lastError = "CURRENT TASK RUNNING: An autonomous task is already running."
            )
            return
        }

        viewModelScope.launch {
            _executionState.value = CommandExecutionState(
                isRunning = true,
                lastCommand = trimmed,
                lastOutput = null,
                lastError = null,
                toolUsed = "Autonomous Agent Loop"
            )

            try {
                val output = agentKernel.executeAutonomousTask(trimmed)
                _executionState.value = CommandExecutionState(
                    isRunning = false,
                    lastCommand = trimmed,
                    lastOutput = output,
                    lastError = null,
                    toolUsed = "NEXUS Agent"
                )
            } catch (e: Throwable) {
                _executionState.value = CommandExecutionState(
                    isRunning = false,
                    lastCommand = trimmed,
                    lastOutput = null,
                    lastError = e.localizedMessage ?: "Task failed",
                    toolUsed = "NEXUS Agent"
                )
            } finally {
                refreshMetrics()
            }
        }
    }

    fun confirmPendingAction() {
        agentKernel.confirmPendingStep()
    }

    fun rejectPendingAction() {
        agentKernel.rejectPendingStep()
    }

    fun cancelActiveTask() {
        agentKernel.cancelTask()
        _executionState.value = _executionState.value.copy(
            isRunning = false,
            lastOutput = "Task cancelled by user.",
            lastError = null
        )
    }

    fun triggerVoiceInput() {
        if (!speechInput.isInstalled) {
            _voiceStatusMessage.value = speechInput.statusMessage
        } else {
            speechInput.start()
        }
    }

    fun clearVoiceStatus() {
        _voiceStatusMessage.value = null
    }

    class Factory(
        private val modelManager: ModelManager,
        private val agentKernel: AgentKernel,
        private val auditLogRepository: AuditLogRepository,
        private val systemMetricsRepository: SystemMetricsRepository,
        private val speechInput: SpeechInput
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NexusDashboardViewModel(
                modelManager,
                agentKernel,
                auditLogRepository,
                systemMetricsRepository,
                speechInput
            ) as T
        }
    }
}

package com.example.nexus.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nexus.core.model.ModelManager
import com.example.nexus.core.model.ModelManagerState
import com.example.nexus.core.model.NativeEngineDiagnostics
import com.example.nexus.data.database.entity.ModelEntity
import com.example.nexus.data.repository.ModelRepository
import com.example.nexus.data.repository.SystemMetrics
import com.example.nexus.data.repository.SystemMetricsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModelManagerViewModel(
    private val modelManager: ModelManager,
    private val modelRepository: ModelRepository,
    private val systemMetricsRepository: SystemMetricsRepository
) : ViewModel() {

    val allModels: StateFlow<List<ModelEntity>> = modelRepository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val managerState: StateFlow<ModelManagerState> = modelManager.state

    private val _systemMetrics = MutableStateFlow(systemMetricsRepository.getSystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    fun refreshMetrics() {
        _systemMetrics.value = systemMetricsRepository.getSystemMetrics()
        modelManager.refreshDiagnostics()
    }

    fun dismissNotice() {
        _uiNotice.value = null
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch {
            _uiNotice.value = "Importing GGUF binary from storage..."
            val result = modelManager.importModelFromUri(uri)
            if (result.isSuccess) {
                val model = result.getOrNull()
                _uiNotice.value = "Successfully imported ${model?.name ?: "model"} (${model?.quantization})"
            } else {
                _uiNotice.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
            refreshMetrics()
        }
    }

    fun loadModel(model: ModelEntity) {
        viewModelScope.launch {
            refreshMetrics()
            val result = modelManager.loadModel(model)
            if (result.isSuccess) {
                _uiNotice.value = "Model ${model.name} loaded into memory"
            } else {
                _uiNotice.value = "Load error: ${result.exceptionOrNull()?.message}"
            }
            refreshMetrics()
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            modelManager.unloadModel()
            _uiNotice.value = "Model unloaded from memory"
            refreshMetrics()
        }
    }

    fun deleteModel(model: ModelEntity) {
        viewModelScope.launch {
            modelManager.removeModel(model)
            _uiNotice.value = "Removed model: ${model.name}"
            refreshMetrics()
        }
    }

    class Factory(
        private val modelManager: ModelManager,
        private val modelRepository: ModelRepository,
        private val systemMetricsRepository: SystemMetricsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ModelManagerViewModel(modelManager, modelRepository, systemMetricsRepository) as T
        }
    }
}

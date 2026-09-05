package com.example.nexus.core.model

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.nexus.data.database.entity.ModelEntity
import com.example.nexus.data.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ModelManagerState(
    val activeModel: ModelEntity? = null,
    val isModelLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0f,
    val statusMessage: String = "No model loaded",
    val error: String? = null,
    val diagnostics: NativeEngineDiagnostics? = null
)

class InsufficientRamException(
    requiredMb: Long,
    availableMb: Long
) : Exception("Insufficient RAM to load model: requires ~${requiredMb}MB, but only ${availableMb}MB is currently available.")

class ModelManager(
    private val context: Context,
    private val repository: ModelRepository,
    private val engine: LocalModelEngine
) {
    companion object {
        private const val TAG = "NEXUS_ModelManager"
    }

    private val _state = MutableStateFlow(
        ModelManagerState(diagnostics = engine.getRuntimeDiagnostics())
    )
    val state: StateFlow<ModelManagerState> = _state.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        _state.value = _state.value.copy(
            diagnostics = engine.getRuntimeDiagnostics(),
            isModelLoaded = engine.isLoaded()
        )
    }

    /**
     * Imports a GGUF model via Storage Access Framework (SAF) URI.
     * Copies the file safely into internal storage, parsing its binary headers.
     */
    suspend fun importModelFromUri(
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<ModelEntity> = withContext(Dispatchers.IO) {
        try {
            _state.value = _state.value.copy(isLoading = true, loadingProgress = 0.05f, error = null)

            val fileName = queryFileName(uri) ?: "imported_model_${System.currentTimeMillis()}.gguf"
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val targetFile = File(modelsDir, fileName)

            // Stream from SAF URI to target file
            val contentResolver = context.contentResolver
            val fileSize = queryFileSize(uri)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (fileSize > 0) {
                            val progress = (bytesCopied.toFloat() / fileSize.toFloat()).coerceIn(0f, 0.95f)
                            _state.value = _state.value.copy(loadingProgress = progress)
                            onProgress(progress)
                        }
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open input stream for URI: $uri"))

            // Parse real GGUF header
            val parsedInfo = GgufMetadataParser.parse(targetFile)

            val modelEntity = ModelEntity(
                name = targetFile.nameWithoutExtension.replace("_", " "),
                filePath = targetFile.absolutePath,
                uriString = uri.toString(),
                sizeBytes = targetFile.length(),
                contextLength = parsedInfo.contextLength,
                quantization = parsedInfo.quantization,
                architecture = parsedInfo.architecture,
                ramRequiredMb = parsedInfo.estimatedRamMb,
                isActive = false
            )

            repository.saveModel(modelEntity)
            _state.value = _state.value.copy(
                isLoading = false,
                loadingProgress = 1f,
                statusMessage = "Imported ${modelEntity.name}"
            )
            Result.success(modelEntity)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to import model", e)
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.localizedMessage ?: "Failed to import GGUF model"
            )
            Result.failure(e)
        }
    }

    /**
     * Loads the model into memory with Out-of-Memory (OOM) protection.
     */
    suspend fun loadModel(model: ModelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = _state.value.copy(
                isLoading = true,
                loadingProgress = 0.1f,
                statusMessage = "Verifying system memory...",
                error = null
            )

            // Check system RAM before loading
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)

            if (model.ramRequiredMb > 0 && availMb < model.ramRequiredMb) {
                val error = InsufficientRamException(model.ramRequiredMb, availMb)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message
                )
                return@withContext Result.failure(error)
            }

            _state.value = _state.value.copy(
                loadingProgress = 0.4f,
                statusMessage = "Initializing native GGUF engine..."
            )

            val result = engine.loadModel(model.filePath)
            if (result.isSuccess) {
                repository.setActiveModel(model.id)
                _state.value = _state.value.copy(
                    activeModel = model,
                    isModelLoaded = true,
                    isLoading = false,
                    loadingProgress = 1f,
                    statusMessage = "Model loaded: ${model.name}",
                    diagnostics = engine.getRuntimeDiagnostics()
                )
                Result.success(Unit)
            } else {
                val ex = result.exceptionOrNull() ?: Exception("Unknown engine load error")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = ex.localizedMessage ?: "Failed to load model"
                )
                Result.failure(ex)
            }
        } catch (e: Throwable) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.localizedMessage ?: "Model loading failure"
            )
            Result.failure(e)
        }
    }

    suspend fun unloadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = engine.unloadModel()
        _state.value = _state.value.copy(
            isModelLoaded = false,
            statusMessage = "Model unloaded",
            diagnostics = engine.getRuntimeDiagnostics()
        )
        result
    }

    suspend fun removeModel(model: ModelEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_state.value.activeModel?.id == model.id) {
                unloadModel()
            }
            val file = File(model.filePath)
            if (file.exists()) {
                file.delete()
            }
            repository.deleteModel(model)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun queryFileSize(uri: Uri): Long {
        var size: Long = 0
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    size = it.getLong(index)
                }
            }
        }
        return size
    }
}

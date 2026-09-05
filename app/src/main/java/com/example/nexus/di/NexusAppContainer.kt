package com.example.nexus.di

import android.content.Context
import com.example.nexus.core.kernel.AgentKernel
import com.example.nexus.core.kernel.ContextBuilder
import com.example.nexus.core.kernel.PromptEngine
import com.example.nexus.core.kernel.StandardContextBuilder
import com.example.nexus.core.model.InferenceController
import com.example.nexus.core.model.LlamaCppNativeAdapter
import com.example.nexus.core.model.LocalModelEngine
import com.example.nexus.core.model.ModelManager
import com.example.nexus.core.model.NexusInferenceController
import com.example.nexus.core.policy.PolicyEngine
import com.example.nexus.core.policy.StandardPolicyEngine
import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.core.tool.tools.AppListTool
import com.example.nexus.core.tool.tools.FileCopyTool
import com.example.nexus.core.tool.tools.FileCreateTool
import com.example.nexus.core.tool.tools.FileDeleteTool
import com.example.nexus.core.tool.tools.FileListTool
import com.example.nexus.core.tool.tools.FileMoveTool
import com.example.nexus.core.tool.tools.FileReadTool
import com.example.nexus.core.tool.tools.LaunchAppTool
import com.example.nexus.core.tool.tools.MemoryDeleteTool
import com.example.nexus.core.tool.tools.MemorySaveTool
import com.example.nexus.core.tool.tools.MemorySearchTool
import com.example.nexus.core.tool.tools.OpenBrowserTool
import com.example.nexus.core.tool.tools.OpenSettingsTool
import com.example.nexus.core.tool.tools.SystemInfoTool
import com.example.nexus.core.voice.OfflineSpeechInput
import com.example.nexus.core.voice.SpeechInput
import com.example.nexus.data.database.NexusDatabase
import com.example.nexus.data.repository.AuditLogRepository
import com.example.nexus.data.repository.MemoryRepository
import com.example.nexus.data.repository.ModelRepository
import com.example.nexus.data.repository.SystemMetricsRepository

/**
 * Dependency container for NEXUS subsystems.
 * Clean, lightweight dependency injection without reflection.
 */
class NexusAppContainer(val context: Context) {

    val database: NexusDatabase by lazy {
        NexusDatabase.getInstance(context)
    }

    val modelRepository: ModelRepository by lazy {
        ModelRepository(database.modelDao())
    }

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(database.memoryDao())
    }

    val auditLogRepository: AuditLogRepository by lazy {
        AuditLogRepository(database.auditLogDao())
    }

    val systemMetricsRepository: SystemMetricsRepository by lazy {
        SystemMetricsRepository(context)
    }

    val modelEngine: LocalModelEngine by lazy {
        LlamaCppNativeAdapter()
    }

    val modelManager: ModelManager by lazy {
        ModelManager(context, modelRepository, modelEngine)
    }

    val policyEngine: PolicyEngine by lazy {
        StandardPolicyEngine()
    }

    val toolRegistry: ToolRegistry by lazy {
        ToolRegistry().apply {
            register(SystemInfoTool(context, "system.info"))
            register(SystemInfoTool(context, "get_system_info"))
            register(AppListTool(context))
            register(LaunchAppTool(context, "app.launch"))
            register(LaunchAppTool(context, "open_app"))
            register(OpenSettingsTool(context))
            register(OpenBrowserTool(context))
            register(FileListTool(context))
            register(FileReadTool(context))
            register(FileCreateTool(context))
            register(FileCopyTool(context))
            register(FileMoveTool(context))
            register(FileDeleteTool(context))
            register(MemorySearchTool(memoryRepository))
            register(MemorySaveTool(memoryRepository))
            register(MemoryDeleteTool(memoryRepository))
        }
    }

    val inferenceController: InferenceController by lazy {
        NexusInferenceController(modelEngine, modelRepository, toolRegistry)
    }

    val contextBuilder: ContextBuilder by lazy {
        StandardContextBuilder(memoryRepository, toolRegistry)
    }

    val promptEngine: PromptEngine by lazy {
        PromptEngine()
    }

    val permissionManager: com.example.nexus.core.permission.AndroidPermissionManager by lazy {
        com.example.nexus.core.permission.StandardAndroidPermissionManager(context)
    }

    val capabilityRegistry: com.example.nexus.core.permission.CapabilityRegistry by lazy {
        com.example.nexus.core.permission.CapabilityRegistry(context, permissionManager)
    }

    val cancellationController: com.example.nexus.core.kernel.CancellationController by lazy {
        com.example.nexus.core.kernel.CancellationController()
    }

    val retryPolicy: com.example.nexus.core.error.RetryPolicy by lazy {
        com.example.nexus.core.error.RetryPolicy(maxRetries = 2)
    }

    val speechInput: SpeechInput by lazy {
        OfflineSpeechInput()
    }

    val agentKernel: AgentKernel by lazy {
        AgentKernel(
            toolRegistry = toolRegistry,
            policyEngine = policyEngine,
            auditLogRepository = auditLogRepository,
            contextBuilder = contextBuilder,
            promptEngine = promptEngine,
            inferenceController = inferenceController,
            cancellationController = cancellationController,
            retryPolicy = retryPolicy,
            maxSteps = 8
        )
    }
}

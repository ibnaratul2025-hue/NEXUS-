package com.example.nexus.di

import android.content.Context
import com.example.nexus.core.cognitive.capability.LimitationRegistry
import com.example.nexus.core.cognitive.capability.LiveCapabilityRegistry
import com.example.nexus.core.cognitive.context.CognitiveContextManager
import com.example.nexus.core.cognitive.explain.ExplainabilityEngine
import com.example.nexus.core.cognitive.improvement.SelfImprovementEngine
import com.example.nexus.core.cognitive.intent.IntentClassifier
import com.example.nexus.core.cognitive.learning.LearningEngine
import com.example.nexus.core.cognitive.memory.CognitiveMemoryEngine
import com.example.nexus.core.cognitive.model.ModelBenchmarkLab
import com.example.nexus.core.cognitive.plan.PlanFeasibilityValidator
import com.example.nexus.core.cognitive.plan.PlanningEngine
import com.example.nexus.core.cognitive.proactive.ProactiveEngine
import com.example.nexus.core.cognitive.skill.SkillEngine
import com.example.nexus.core.cognitive.skill.SkillTestRunner
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
import com.example.nexus.data.repository.KnowledgeGraphRepository
import com.example.nexus.data.repository.LearningRepository
import com.example.nexus.data.repository.MemoryRepository
import com.example.nexus.data.repository.ModelRepository
import com.example.nexus.data.repository.ProactiveRepository
import com.example.nexus.data.repository.SkillRepository
import com.example.nexus.data.repository.SystemMetricsRepository
import com.example.nexus.data.repository.MissionRepository
import com.example.nexus.data.repository.PredictionRepository
import com.example.nexus.data.repository.PersonalModelRepository
import com.example.nexus.data.repository.ActionHistoryRepository
import com.example.nexus.core.presence.PersonalContextEngine
import com.example.nexus.core.presence.WorkingMemory
import com.example.nexus.core.presence.AttentionManager
import com.example.nexus.core.presence.JarvisMomentsGenerator
import com.example.nexus.core.presence.PredictionEngine
import com.example.nexus.core.presence.TaskPreparationEngine
import com.example.nexus.core.presence.UniversalUndoManager
import com.example.nexus.core.presence.ResourceOrchestrator
import com.example.nexus.core.presence.SelfDiagnosticEngine
import com.example.nexus.core.presence.TimeAwareIntelligence
import com.example.nexus.core.presence.EmotionalToneAdapter
import com.example.nexus.core.presence.MultiAgentInternalCoordinator
import com.example.nexus.core.presence.MissionEngine
import com.example.nexus.core.presence.PersonalModel
import com.example.nexus.core.presence.SecondBrainEngine
import com.example.nexus.core.presence.EvolutionEngine
import com.example.nexus.core.presence.WhatShouldIDoEngine
import com.example.nexus.core.presence.JarvisCommandEngine
import com.example.nexus.core.presence.JarvisVoiceController

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

    val skillRepository: SkillRepository by lazy {
        SkillRepository(database.skillDao())
    }

    val learningRepository: LearningRepository by lazy {
        LearningRepository(database.learningRecordDao())
    }

    val knowledgeGraphRepository: KnowledgeGraphRepository by lazy {
        KnowledgeGraphRepository(database.knowledgeGraphDao())
    }

    val proactiveRepository: ProactiveRepository by lazy {
        ProactiveRepository(database.proactiveSuggestionDao())
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

    val liveCapabilityRegistry: LiveCapabilityRegistry by lazy {
        LiveCapabilityRegistry(context, permissionManager)
    }

    val limitationRegistry: LimitationRegistry by lazy {
        LimitationRegistry()
    }

    val intentClassifier: IntentClassifier by lazy {
        IntentClassifier()
    }

    val planFeasibilityValidator: PlanFeasibilityValidator by lazy {
        PlanFeasibilityValidator(toolRegistry, permissionManager, liveCapabilityRegistry)
    }

    val planningEngine: PlanningEngine by lazy {
        PlanningEngine(toolRegistry, planFeasibilityValidator)
    }

    val learningEngine: LearningEngine by lazy {
        LearningEngine(learningRepository)
    }

    val cognitiveMemoryEngine: CognitiveMemoryEngine by lazy {
        CognitiveMemoryEngine(memoryRepository, knowledgeGraphRepository)
    }

    val skillTestRunner: SkillTestRunner by lazy {
        SkillTestRunner(toolRegistry)
    }

    val skillEngine: SkillEngine by lazy {
        SkillEngine(skillRepository, skillTestRunner)
    }

    val cognitiveContextManager: CognitiveContextManager by lazy {
        CognitiveContextManager(memoryRepository)
    }

    val selfImprovementEngine: SelfImprovementEngine by lazy {
        SelfImprovementEngine(learningRepository, auditLogRepository)
    }

    val explainabilityEngine: ExplainabilityEngine by lazy {
        ExplainabilityEngine()
    }

    val proactiveEngine: ProactiveEngine by lazy {
        ProactiveEngine(proactiveRepository, memoryRepository, auditLogRepository)
    }

    val modelBenchmarkLab: ModelBenchmarkLab by lazy {
        ModelBenchmarkLab(inferenceController)
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
            maxSteps = 8,
            intentClassifier = intentClassifier,
            planningEngine = planningEngine,
            limitationRegistry = limitationRegistry,
            learningEngine = learningEngine,
            explainabilityEngine = explainabilityEngine
        )
    }

    // --- Autonomous Presence & JARVIS Capabilities ---

    val missionRepository: MissionRepository by lazy {
        MissionRepository(database.missionDao())
    }

    val predictionRepository: PredictionRepository by lazy {
        PredictionRepository(database.predictionDao())
    }

    val personalModelRepository: PersonalModelRepository by lazy {
        PersonalModelRepository(database.personalModelDao())
    }

    val actionHistoryRepository: ActionHistoryRepository by lazy {
        ActionHistoryRepository(database.actionHistoryDao())
    }

    val personalContextEngine: PersonalContextEngine by lazy {
        PersonalContextEngine(context, permissionManager)
    }

    val workingMemory: WorkingMemory by lazy {
        WorkingMemory()
    }

    val attentionManager: AttentionManager by lazy {
        AttentionManager()
    }

    val jarvisMomentsGenerator: JarvisMomentsGenerator by lazy {
        JarvisMomentsGenerator()
    }

    val predictionEngine: PredictionEngine by lazy {
        PredictionEngine(predictionRepository)
    }

    val taskPreparationEngine: TaskPreparationEngine by lazy {
        TaskPreparationEngine(toolRegistry, planFeasibilityValidator)
    }

    val universalUndoManager: UniversalUndoManager by lazy {
        UniversalUndoManager(actionHistoryRepository)
    }

    val resourceOrchestrator: ResourceOrchestrator by lazy {
        ResourceOrchestrator()
    }

    val selfDiagnosticEngine: SelfDiagnosticEngine by lazy {
        SelfDiagnosticEngine(toolRegistry)
    }

    val timeAwareIntelligence: TimeAwareIntelligence by lazy {
        TimeAwareIntelligence()
    }

    val emotionalToneAdapter: EmotionalToneAdapter by lazy {
        EmotionalToneAdapter()
    }

    val multiAgentCoordinator: MultiAgentInternalCoordinator by lazy {
        MultiAgentInternalCoordinator(policyEngine, toolRegistry)
    }

    val missionEngine: MissionEngine by lazy {
        MissionEngine(missionRepository)
    }

    val personalModel: PersonalModel by lazy {
        PersonalModel(personalModelRepository)
    }

    val secondBrainEngine: SecondBrainEngine by lazy {
        SecondBrainEngine(memoryRepository, personalModelRepository, skillRepository, knowledgeGraphRepository)
    }

    val evolutionEngine: EvolutionEngine by lazy {
        EvolutionEngine(skillEngine)
    }

    val whatShouldIDoEngine: WhatShouldIDoEngine by lazy {
        WhatShouldIDoEngine(personalModelRepository, timeAwareIntelligence)
    }

    val jarvisCommandEngine: JarvisCommandEngine by lazy {
        JarvisCommandEngine(workingMemory, universalUndoManager, missionEngine, policyEngine)
    }

    val jarvisVoiceController: JarvisVoiceController by lazy {
        JarvisVoiceController(context)
    }

    // --- PHASE 7: DIGITAL WORLD MODEL & UNIVERSAL ACTION FABRIC ---
    val localEventBus: com.example.nexus.core.world.LocalEventBus by lazy {
        com.example.nexus.core.world.LocalEventBus()
    }

    val worldModelEngine: com.example.nexus.core.world.WorldModelEngine by lazy {
        com.example.nexus.core.world.WorldModelEngine()
    }

    val actionFabric: com.example.nexus.core.world.ActionFabric by lazy {
        com.example.nexus.core.world.ActionFabric(
            context = context,
            policyEngine = policyEngine,
            undoManager = universalUndoManager,
            localEventBus = localEventBus,
            worldModelEngine = worldModelEngine
        )
    }

    val capabilityNegotiator: com.example.nexus.core.world.CapabilityNegotiator by lazy {
        com.example.nexus.core.world.CapabilityNegotiator(
            context = context,
            actionFabric = actionFabric,
            policyEngine = policyEngine
        )
    }

    val appCapabilityRegistry: com.example.nexus.core.world.AppCapabilityRegistry by lazy {
        com.example.nexus.core.world.AppCapabilityRegistry(
            context = context,
            worldModelEngine = worldModelEngine
        )
    }

    val appAdapterManager: com.example.nexus.core.world.AppAdapterManager by lazy {
        com.example.nexus.core.world.AppAdapterManager(
            context = context,
            actionFabric = actionFabric,
            worldModelEngine = worldModelEngine
        )
    }

    val accessibilityController: com.example.nexus.core.world.AccessibilityController by lazy {
        com.example.nexus.core.world.AccessibilityController(
            context = context,
            worldModelEngine = worldModelEngine
        )
    }

    val screenContextEngine: com.example.nexus.core.world.ScreenContextEngine by lazy {
        com.example.nexus.core.world.ScreenContextEngine(
            worldModelEngine = worldModelEngine
        )
    }

    val notificationIntelligence: com.example.nexus.core.world.NotificationIntelligence by lazy {
        com.example.nexus.core.world.NotificationIntelligence(
            worldModelEngine = worldModelEngine
        )
    }

    val ambientPresenceEngine: com.example.nexus.core.world.AmbientPresenceEngine by lazy {
        com.example.nexus.core.world.AmbientPresenceEngine(
            worldModelEngine = worldModelEngine
        )
    }

    val deviceCapabilityRegistry: com.example.nexus.core.world.DeviceCapabilityRegistry by lazy {
        com.example.nexus.core.world.DeviceCapabilityRegistry(
            worldModelEngine = worldModelEngine
        )
    }

    val workflowCompiler: com.example.nexus.core.world.WorkflowCompiler by lazy {
        com.example.nexus.core.world.WorkflowCompiler(
            worldModelEngine = worldModelEngine
        )
    }

    val missionAutopilot: com.example.nexus.core.world.MissionAutopilot by lazy {
        com.example.nexus.core.world.MissionAutopilot(
            missionEngine = missionEngine,
            actionFabric = actionFabric,
            capabilityNegotiator = capabilityNegotiator,
            localEventBus = localEventBus
        )
    }

    val seeUnderstandActPipeline: com.example.nexus.core.world.SeeUnderstandActPipeline by lazy {
        com.example.nexus.core.world.SeeUnderstandActPipeline(
            worldModelEngine = worldModelEngine,
            actionFabric = actionFabric,
            capabilityNegotiator = capabilityNegotiator,
            policyEngine = policyEngine,
            localEventBus = localEventBus
        )
    }

    val selfAwarenessEngine: com.example.nexus.core.world.SelfAwarenessEngine by lazy {
        com.example.nexus.core.world.SelfAwarenessEngine(
            context = context,
            actionFabric = actionFabric,
            appCapabilityRegistry = appCapabilityRegistry,
            workingMemory = workingMemory
        )
    }
}

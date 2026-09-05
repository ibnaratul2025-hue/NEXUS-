package com.example.nexus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nexus.core.cognitive.capability.LimitationRegistry
import com.example.nexus.core.cognitive.capability.LiveCapabilityRegistry
import com.example.nexus.core.cognitive.context.CognitiveContextManager
import com.example.nexus.core.cognitive.context.TrustLabel
import com.example.nexus.core.cognitive.intent.IntentClassifier
import com.example.nexus.core.cognitive.intent.IntentType
import com.example.nexus.core.cognitive.learning.FailureClassifier
import com.example.nexus.core.cognitive.learning.FailureType
import com.example.nexus.core.cognitive.learning.LearningEngine
import com.example.nexus.core.cognitive.memory.CognitiveMemoryEngine
import com.example.nexus.core.cognitive.memory.ContradictionDetector
import com.example.nexus.core.cognitive.memory.KnowledgeEntityType
import com.example.nexus.core.cognitive.memory.MemoryDecayCalculator
import com.example.nexus.core.cognitive.memory.MemorySource
import com.example.nexus.core.cognitive.model.ModelBenchmarkLab
import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.cognitive.plan.PlanFeasibilityValidator
import com.example.nexus.core.cognitive.plan.PlanningEngine
import com.example.nexus.core.cognitive.skill.SkillEngine
import com.example.nexus.core.cognitive.skill.SkillState
import com.example.nexus.core.cognitive.skill.SkillStep
import com.example.nexus.core.cognitive.skill.SkillTestRunner
import com.example.nexus.core.error.ToolError
import com.example.nexus.core.model.ChatMessage
import com.example.nexus.core.model.GenerationEvent
import com.example.nexus.core.model.GenerationOptions
import com.example.nexus.core.model.InferenceController
import com.example.nexus.core.permission.StandardAndroidPermissionManager
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import com.example.nexus.data.database.NexusDatabase
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.repository.KnowledgeGraphRepository
import com.example.nexus.data.repository.LearningRepository
import com.example.nexus.data.repository.MemoryRepository
import com.example.nexus.data.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase5CognitiveEvolutionUnitTest {

    private lateinit var context: Context
    private lateinit var database: NexusDatabase
    private lateinit var memoryRepository: MemoryRepository
    private lateinit var learningRepository: LearningRepository
    private lateinit var skillRepository: SkillRepository
    private lateinit var knowledgeGraphRepository: KnowledgeGraphRepository
    private lateinit var permissionManager: StandardAndroidPermissionManager
    private lateinit var capabilityRegistry: LiveCapabilityRegistry
    private lateinit var limitationRegistry: LimitationRegistry
    private lateinit var toolRegistry: ToolRegistry

    private val dummyTool = object : AgentTool {
        override val id: String = "system.info"
        override val name: String = "System Info"
        override val description: String = "System telemetry info"
        override val argumentSchema: ToolSchema = ToolSchema()
        override val requiredPermissions: List<String> = emptyList()
        override val riskLevel: RiskLevel = RiskLevel.LOW
        override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
            return ToolResult(success = true, output = "Battery: 95%, Storage: 20GB free")
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = NexusDatabase.getInstance(context)
        memoryRepository = MemoryRepository(database.memoryDao())
        learningRepository = LearningRepository(database.learningRecordDao())
        skillRepository = SkillRepository(database.skillDao())
        knowledgeGraphRepository = KnowledgeGraphRepository(database.knowledgeGraphDao())
        permissionManager = StandardAndroidPermissionManager(context)
        capabilityRegistry = LiveCapabilityRegistry(context, permissionManager)
        limitationRegistry = LimitationRegistry()

        toolRegistry = ToolRegistry().apply {
            register(dummyTool)
        }
    }

    @Test
    fun testIntentClassificationFidelity() {
        val classifier = IntentClassifier()

        val qResult = classifier.classify("What is the capital of France?")
        assertEquals(IntentType.QUESTION, qResult.type)
        assertTrue(qResult.confidence >= 0.7f)

        val sysResult = classifier.classify("What is the current battery level and device info?")
        assertEquals(IntentType.SYSTEM_REQUEST, sysResult.type)

        val cmdResult = classifier.classify("Open Chrome")
        assertEquals(IntentType.COMMAND, cmdResult.type)

        val multiResult = classifier.classify("First inspect storage then backup files and finally notify me")
        assertEquals(IntentType.MULTI_STEP_TASK, multiResult.type)

        val memResult = classifier.classify("What do you remember about my projects?")
        assertEquals(IntentType.MEMORY_REQUEST, memResult.type)

        val learnResult = classifier.classify("Remember that my preferred editor is Vim")
        assertEquals(IntentType.LEARNING_REQUEST, learnResult.type)

        val skillResult = classifier.classify("Create a new skill for nightly backup")
        assertEquals(IntentType.SKILL_REQUEST, skillResult.type)
    }

    @Test
    fun testPlanningEngineAndFeasibilityValidation() {
        val validator = PlanFeasibilityValidator(toolRegistry, permissionManager, capabilityRegistry)
        val planningEngine = PlanningEngine(toolRegistry, validator)

        val intent = classifierResult("Query battery and device info")
        val plan = planningEngine.generatePlan(intent)

        assertNotNull(plan)
        assertTrue("Plan must contain ordered steps", plan.steps.isNotEmpty())
        assertEquals("system.info", plan.steps.first().toolId)

        val feasibility = validator.validate(plan)
        assertTrue("Plan using registered tool must be valid", feasibility.isValid)

        // Test impossible assumption detection
        val impossiblePlan = ExecutionPlan(
            goal = "Break sandbox",
            steps = listOf(
                CognitivePlanStep(
                    stepId = "s1",
                    stepNumber = 1,
                    toolId = "system.info",
                    description = "Requires nonexistent quantum hardware",
                    requiredCapabilities = listOf("hardware.quantum_accelerator"),
                    expectedResult = "Quantum status"
                ),
                CognitivePlanStep(
                    stepId = "s2",
                    stepNumber = 2,
                    toolId = "non_existent_exploit_tool",
                    description = "Attempt unknown tool",
                    expectedResult = "Impossible result"
                )
            )
        )
        val impossibleCheck = validator.validate(impossiblePlan)
        assertFalse("Plan with missing tool and missing hardware must fail feasibility", impossibleCheck.isValid)
        assertTrue(impossibleCheck.issues.any { it.contains("Impossible assumption") })
        assertTrue(impossibleCheck.issues.any { it.contains("UNAVAILABLE") })
    }

    @Test
    fun testTruthFirstLearningOnlyFromVerifiedReceipts() = runBlocking {
        val learningEngine = LearningEngine(learningRepository)

        val successReceipt = ToolReceipt(
            toolId = "system.info",
            status = ToolStatus.SUCCESS,
            outputSummary = "Battery: 85%"
        )
        val record = learningEngine.processReceipt(successReceipt)
        assertNotNull(record)
        assertEquals("TOOL_RECEIPT", record!!.eventType)
        assertTrue(record.verified)

        val failureReceipt = ToolReceipt(
            toolId = "file.delete",
            status = ToolStatus.FAILED,
            error = ToolError(
                code = "PERMISSION_DENIED",
                userMessage = "Storage write permission denied",
                technicalMessage = "SecurityException"
            )
        )
        val failRecord = learningEngine.processReceipt(failureReceipt)
        assertNotNull(failRecord)
        assertEquals("VERIFIED_FAILURE", failRecord!!.eventType)
        assertEquals(FailureType.PERMISSION.name, failRecord.failureClassification)
        assertTrue(failRecord.verified)
    }

    @Test
    fun testFailureClassificationAccuracy() {
        val classifier = FailureClassifier()

        val permReceipt = ToolReceipt(
            toolId = "camera.take_photo",
            status = ToolStatus.FAILED,
            error = ToolError.permissionDenied("android.permission.CAMERA")
        )
        assertEquals(FailureType.PERMISSION, classifier.classify(permReceipt))

        val envReceipt = ToolReceipt(
            toolId = "file.read",
            status = ToolStatus.FAILED,
            error = ToolError.fileNotFound("/sdcard/missing.txt")
        )
        assertEquals(FailureType.ENVIRONMENT, classifier.classify(envReceipt))

        val cancelReceipt = ToolReceipt(
            toolId = "app.launch",
            status = ToolStatus.CANCELLED
        )
        assertEquals(FailureType.AMBIGUITY, classifier.classify(cancelReceipt))
    }

    @Test
    fun testMemoryDecayAndContradictionDetection() = runBlocking {
        val decayCalculator = MemoryDecayCalculator()
        val detector = ContradictionDetector()

        // 1. Decay calculation test
        val now = System.currentTimeMillis()
        val freshScore = decayCalculator.calculateDecay(
            createdAt = now,
            lastAccessedAt = now,
            source = MemorySource.USER_EXPLICIT,
            entityType = KnowledgeEntityType.PREFERENCES,
            currentTime = now
        )
        assertTrue("Fresh explicit memory should have high decay score", freshScore >= 0.95f)

        val twoWeeksAgo = now - (14L * 24L * 60L * 60L * 1000L)
        val decayedScore = decayCalculator.calculateDecay(
            createdAt = twoWeeksAgo,
            lastAccessedAt = twoWeeksAgo,
            source = MemorySource.INFERRED,
            entityType = KnowledgeEntityType.TASKS,
            currentTime = now
        )
        assertTrue("Old unaccessed inferred memory should decay substantially", decayedScore < 0.70f)

        // 2. Contradiction detection test
        val existing = listOf(
            MemoryEntity(
                id = "mem_dark",
                content = "User prefers dark mode in all environments",
                category = "Preferences",
                source = MemorySource.USER_EXPLICIT.name
            )
        )
        val contradiction = detector.detectContradiction("User prefers light mode on mobile", existing)
        assertTrue("Should detect conflicting theme preferences", contradiction.hasContradiction)
        assertEquals("mem_dark", contradiction.conflictingMemory?.id)

        // 3. Privacy Guard test: sensitive personal attributes cannot be stored without explicit user intent
        val cognitiveMemoryEngine = CognitiveMemoryEngine(memoryRepository, knowledgeGraphRepository)
        try {
            cognitiveMemoryEngine.saveCognitiveMemory(
                content = "User SSN is 123-45-6789",
                category = "Inferred",
                source = MemorySource.INFERRED,
                entityType = KnowledgeEntityType.GENERAL,
                isExplicitUserIntent = false
            )
            fail("Expected SecurityException for storing sensitive personal attribute without explicit intent")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Privacy violation") == true)
        }
    }

    @Test
    fun testSkillLifecycleAndSandboxTesting() = runBlocking {
        val testRunner = SkillTestRunner(toolRegistry)
        val skillEngine = SkillEngine(skillRepository, testRunner)

        // 1. Discover Skill
        val skill = skillEngine.discoverSkill(
            name = "Battery & Health Check",
            description = "Runs local system telemetry check",
            triggerIntent = "COMMAND",
            steps = listOf(
                SkillStep(
                    stepIndex = 1,
                    toolId = "system.info",
                    description = "Query battery and storage",
                    argumentsTemplate = emptyMap()
                )
            )
        )
        assertEquals(SkillState.DISCOVERED.name, skill.state)

        // 2. Test Skill in Sandbox
        val testResult = skillEngine.testSkill(skill.id)
        assertTrue("Skill test against valid tool must pass", testResult.isPassed)
        assertEquals(1, testResult.testedStepCount)

        val verifiedSkill = skillRepository.getSkillById(skill.id)
        assertNotNull(verifiedSkill)
        assertEquals(SkillState.VERIFIED.name, verifiedSkill!!.state)

        // 3. Approve and Activate
        val approved = skillEngine.approveAndActivateSkill(skill.id)
        assertTrue(approved)
        val activeSkill = skillRepository.getSkillById(skill.id)
        assertEquals(SkillState.ACTIVE.name, activeSkill!!.state)

        // 4. Export & Import JSON
        val model = skillEngine.toModel(activeSkill)
        val exportedJson = skillEngine.exportSkillToJson(model)
        assertTrue(exportedJson.contains("Battery & Health Check"))

        val imported = skillEngine.importSkillFromJson(exportedJson)
        assertEquals("Battery & Health Check", imported.name)
        assertEquals(SkillState.DRAFT.name, imported.state) // Imported skills start in DRAFT for safety
    }

    @Test
    fun testContextIntelligenceTrustLabelsAndBudgetPruning() = runBlocking {
        val contextManager = CognitiveContextManager(memoryRepository, maxTokenBudget = 200)

        memoryRepository.saveMemory(
            MemoryEntity(
                id = "m1",
                content = "User prefers concise answers",
                category = "Preferences",
                decayScore = 0.95f,
                confidence = 0.95f
            )
        )

        val assembled = contextManager.assembleContext(
            query = "concise answers",
            additionalItems = listOf(
                "SYSTEM: Never bypass user confirmation." to TrustLabel.SYSTEM
            )
        )

        assertTrue(assembled.isNotEmpty())
        assertTrue(assembled.any { it.trustLabel == TrustLabel.SYSTEM })
        assertTrue(assembled.any { it.trustLabel == TrustLabel.LOCAL_MEMORY })

        val formattedPrompt = contextManager.formatForPrompt(assembled)
        assertTrue(formattedPrompt.contains("SYSTEM"))
        assertTrue(formattedPrompt.contains("LOCAL_MEMORY"))
    }

    @Test
    fun testModelBenchmarkLabMetrics() = runBlocking {
        val mockInference = object : InferenceController {
            override val isRunning: Boolean = false

            override suspend fun generate(
                messages: List<ChatMessage>,
                options: GenerationOptions
            ): Flow<GenerationEvent> = flow {
                emit(GenerationEvent.Started("BenchmarkModel"))
                val text = "{\"tool\": \"system.info\", \"action\": \"query\"}"
                emit(GenerationEvent.Token(text, text))
                emit(GenerationEvent.Completed(fullText = text, tokenCount = 10, durationMs = 50L))
            }

            override fun cancel() {}
        }

        val lab = ModelBenchmarkLab(mockInference)
        val result = lab.runBenchmark()

        assertTrue(result.firstTokenLatencyMs >= 0)
        assertTrue(result.tokensPerSecond > 0f)
        assertTrue(result.peakMemoryMb >= result.initialMemoryMb)
        assertEquals(1.0f, result.jsonToolReliabilityScore, 0.01f)
        assertTrue(result.cancellationResponseMs >= 0)
    }

    private fun classifierResult(text: String) = IntentClassifier().classify(text)
}

package com.example.nexus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.policy.StandardPolicyEngine
import com.example.nexus.core.presence.UniversalUndoManager
import com.example.nexus.core.world.AccessibilityActionResult
import com.example.nexus.core.world.AccessibilityController
import com.example.nexus.core.world.ActionExecutionResult
import com.example.nexus.core.world.ActionFabric
import com.example.nexus.core.world.AppAdapter
import com.example.nexus.core.world.AppAdapterManager
import com.example.nexus.core.world.AppCapabilityRegistry
import com.example.nexus.core.world.CapabilityNegotiator
import com.example.nexus.core.world.EpistemicStatus
import com.example.nexus.core.world.LocalEventBus
import com.example.nexus.core.world.NegotiationResult
import com.example.nexus.core.world.UiElementNode
import com.example.nexus.core.world.UiNodeSelector
import com.example.nexus.core.world.UniversalAction
import com.example.nexus.core.world.WorkflowCompiler
import com.example.nexus.core.world.WorldEntity
import com.example.nexus.core.world.WorldEntityType
import com.example.nexus.core.world.WorldModelEngine
import com.example.nexus.core.world.WorldRelation
import com.example.nexus.core.world.WorldRelationType
import com.example.nexus.data.database.dao.ActionHistoryDao
import com.example.nexus.data.database.entity.ActionHistoryEntity
import com.example.nexus.data.repository.ActionHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class Phase7WorldModelActionFabricUnitTest {

    private lateinit var context: Context
    private lateinit var worldModelEngine: WorldModelEngine
    private lateinit var localEventBus: LocalEventBus
    private lateinit var policyEngine: StandardPolicyEngine
    private lateinit var undoManager: UniversalUndoManager
    private lateinit var actionFabric: ActionFabric
    private lateinit var capabilityNegotiator: CapabilityNegotiator
    private lateinit var accessibilityController: AccessibilityController
    private lateinit var appAdapterManager: AppAdapterManager
    private lateinit var appCapabilityRegistry: AppCapabilityRegistry
    private lateinit var workflowCompiler: WorkflowCompiler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        worldModelEngine = WorldModelEngine()
        localEventBus = LocalEventBus()
        policyEngine = StandardPolicyEngine()

        val fakeDao = object : ActionHistoryDao {
            private val list = mutableListOf<ActionHistoryEntity>()
            override fun getAllActions(): Flow<List<ActionHistoryEntity>> = flowOf(list)
            override fun getReversibleActions(): Flow<List<ActionHistoryEntity>> = flowOf(list.filter { it.isReversible && it.undoneAt == null })
            override suspend fun getLastReversibleAction(): ActionHistoryEntity? = list.lastOrNull { it.isReversible && it.undoneAt == null }
            override suspend fun getActionById(id: String): ActionHistoryEntity? = list.find { it.id == id }
            override suspend fun insertAction(action: ActionHistoryEntity) { list.add(action) }
            override suspend fun updateAction(action: ActionHistoryEntity) { }
            override suspend fun deleteAction(action: ActionHistoryEntity) { list.remove(action) }
            override suspend fun purgeOldActions(threshold: Long) { list.removeAll { it.executedAt < threshold } }
        }
        undoManager = UniversalUndoManager(ActionHistoryRepository(fakeDao))
        actionFabric = ActionFabric(context, policyEngine, undoManager, localEventBus, worldModelEngine)
        capabilityNegotiator = CapabilityNegotiator(context, actionFabric, policyEngine)
        accessibilityController = AccessibilityController(context, worldModelEngine)
        appAdapterManager = AppAdapterManager(context, actionFabric, worldModelEngine)
        appCapabilityRegistry = AppCapabilityRegistry(context, worldModelEngine)
        workflowCompiler = WorkflowCompiler(worldModelEngine)
    }

    // 1. World Model Consistency & Epistemic Separation
    @Test
    fun testWorldModelConsistency_epistemicInvariantEnforced() {
        // Register a FACT
        val factEntity = WorldEntity(
            id = "test_file_config",
            type = WorldEntityType.FILE,
            name = "config.json",
            epistemicStatus = EpistemicStatus.FACT,
            confidence = 0.5f // Even if supplied 0.5, FACT must be sanitized to 1.0f ground truth
        )
        worldModelEngine.registerEntity(factEntity)
        val retrieved = worldModelEngine.getEntity("test_file_config")
        assertNotNull(retrieved)
        assertEquals(1.0f, retrieved?.confidence)
        assertEquals(EpistemicStatus.FACT, retrieved?.epistemicStatus)

        // Register a PREDICTION
        val predictionEntity = WorldEntity(
            id = "test_pred_battery",
            type = WorldEntityType.NOTIFICATION,
            name = "Battery Exhaustion",
            epistemicStatus = EpistemicStatus.PREDICTION,
            confidence = 1.0f // PREDICTION cannot be 1.0f ground truth; sanitized to <= 0.99
        )
        worldModelEngine.registerEntity(predictionEntity)
        val retrievedPred = worldModelEngine.getEntity("test_pred_battery")
        assertTrue((retrievedPred?.confidence ?: 1.0f) < 1.0f)
    }

    @Test
    fun testWorldModelRelationships_bidirectionalLookup() {
        val user = WorldEntity("user_1", WorldEntityType.USER, "Owner", EpistemicStatus.FACT)
        val project = WorldEntity("proj_1", WorldEntityType.PROJECT, "NexusOS", EpistemicStatus.FACT)
        worldModelEngine.registerEntity(user)
        worldModelEngine.registerEntity(project)

        worldModelEngine.addRelation(
            WorldRelation(
                fromEntityId = "user_1",
                toEntityId = "proj_1",
                relationType = WorldRelationType.OWNS
            )
        )

        val relations = worldModelEngine.getOutgoingRelations("user_1", WorldRelationType.OWNS)
        assertEquals(1, relations.size)
        assertEquals("proj_1", relations[0].toEntityId)

        val connected = worldModelEngine.findConnectedEntities("user_1", WorldRelationType.OWNS)
        assertEquals(1, connected.size)
        assertEquals("NexusOS", connected[0].name)
    }

    // 2. Capability Discovery & Non-Hallucination Guarantees
    @Test
    fun testNonHallucinationGuarantee_unsupportedCapabilityExplainsLimitation() {
        val result = capabilityNegotiator.negotiate(
            capability = "fly_to_moon_rocket",
            target = "Spacecraft"
        )
        assertTrue("Must be a technical limitation", result is NegotiationResult.Limitation)
        val limitation = result as NegotiationResult.Limitation
        assertTrue(limitation.reason.contains("not supported"))
        assertTrue(limitation.exactLimitation.contains("no registered handler"))
    }

    // 3. Action Validation & Hard Policy Security Boundary
    @Test
    fun testSecurityBoundary_highRiskActionRequiresConfirmation() = runBlocking {
        val highRiskAction = UniversalAction(
            capability = "dangerous_action",
            target = "critical_system",
            risk = RiskLevel.HIGH,
            description = "High risk modification",
            execute = { ActionExecutionResult.Success("1", "Success", true, false) }
        )

        // Attempt execution without user confirmation
        val unconfirmedResult = actionFabric.executeAction(highRiskAction, isUserConfirmed = false)
        assertTrue(unconfirmedResult is ActionExecutionResult.RequiresConfirmation)

        // Attempt execution WITH explicit user confirmation
        val confirmedResult = actionFabric.executeAction(highRiskAction, isUserConfirmed = true)
        assertTrue(confirmedResult is ActionExecutionResult.Success)
    }

    // 4. Accessibility Automation & Emergency Stop
    @Test
    fun testAccessibilityAutomation_emergencyStopHaltsExecutionImmediately() {
        val dummyTree = UiElementNode(
            id = "1",
            packageName = context.packageName,
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true
        )

        accessibilityController.setServiceEnabled(true)
        accessibilityController.triggerEmergencyStop("Test Stop Triggered")

        val result = accessibilityController.performClick(
            targetPackage = context.packageName,
            selector = UiNodeSelector(text = "Submit"),
            currentTree = dummyTree
        )

        assertTrue(result is AccessibilityActionResult.EmergencyStopped)
    }

    @Test
    fun testAccessibilityAutomation_semanticNodeSelection() {
        accessibilityController.setServiceEnabled(true)
        accessibilityController.resetEmergencyStop()

        val dummyTree = UiElementNode(
            id = "root",
            packageName = context.packageName,
            className = "android.widget.FrameLayout",
            children = listOf(
                UiElementNode(
                    id = "btn_1",
                    packageName = context.packageName,
                    className = "android.widget.Button",
                    resourceId = "com.example:id/submit_button",
                    text = "Confirm Order",
                    isClickable = true
                )
            )
        )

        val result = accessibilityController.performClick(
            targetPackage = context.packageName,
            selector = UiNodeSelector(resourceId = "com.example:id/submit_button"),
            currentTree = dummyTree
        )

        assertTrue(result is AccessibilityActionResult.Success)
        assertEquals("com.example:id/submit_button", (result as AccessibilityActionResult.Success).nodeFound)
    }

    // 5. App Adapter Sandboxing
    @Test
    fun testAdapterSandbox_unapprovedAdapterIsBlocked() = runBlocking {
        val testAdapter = object : AppAdapter {
            override val appId = "unapproved_app"
            override val appName = "Unapproved App"
            override val version = "1.0.0"
            override val capabilities = listOf("test_cap")
            override val requiredPermissions = emptyList<String>()
            override val actions = listOf(
                com.example.nexus.core.world.AdapterActionDefinition(
                    name = "test_cap",
                    description = "Test capability",
                    riskLevel = RiskLevel.LOW,
                    requiredPermissions = emptyList(),
                    parametersSchema = emptyMap(),
                    verificationRule = "rule"
                )
            )
            override suspend fun execute(context: Context, actionName: String, parameters: Map<String, Any>): ActionExecutionResult {
                return ActionExecutionResult.Success("id", "Executed", true, false)
            }
        }

        // Register adapter with userApproved = false
        appAdapterManager.registerAdapter(testAdapter, userApproved = false)
        assertFalse(appAdapterManager.isApproved("unapproved_app"))

        val action = actionFabric.getAction("unapproved_app.test_cap", emptyMap())
        assertNotNull(action)

        val result = actionFabric.executeAction(action!!, isUserConfirmed = true)
        assertTrue("Execution must be blocked because adapter is unapproved", result is ActionExecutionResult.Blocked)
    }

    // 6. Workflow Compiler Deterministic Automaton
    @Test
    fun testWorkflowCompiler_compilesIntoStructuredFiveStages() {
        val compiled = workflowCompiler.compile("Whenever I start studying, prepare my study environment")

        assertEquals("Study Environment Setup", compiled.name)
        assertEquals("VOICE_KEYWORD", compiled.trigger.type)
        assertEquals("start studying", compiled.trigger.parameter)
        assertTrue(compiled.conditions.isNotEmpty())
        assertTrue(compiled.actions.isNotEmpty())
        assertNotNull(compiled.verification)
        assertNotNull(compiled.failureHandling)
        assertFalse("Must require user approval before activation", compiled.isUserApproved)
    }

    // 7. Dynamic Capability Discovery via AppCapabilityRegistry
    @Test
    fun testAppCapabilityRegistry_scansWithoutCrashing() = runBlocking {
        val profiles = appCapabilityRegistry.scanInstalledApps()
        assertNotNull(profiles)
        val entitiesInWorld = worldModelEngine.getEntitiesByType(WorldEntityType.APP)
        assertEquals(profiles.size, entitiesInWorld.size)
    }
}

package com.example.nexus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nexus.core.error.ErrorSeverity
import com.example.nexus.core.error.RetryPolicy
import com.example.nexus.core.error.ToolError
import com.example.nexus.core.kernel.AntiHallucinationValidator
import com.example.nexus.core.kernel.CancellationController
import com.example.nexus.core.kernel.MismatchCategory
import com.example.nexus.core.permission.AndroidPermissionManager
import com.example.nexus.core.permission.CapabilityAvailability
import com.example.nexus.core.permission.CapabilityRegistry
import com.example.nexus.core.permission.PermissionState
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.receipt.ConfirmationRequest
import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.core.tool.tools.FileSandboxHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase3ReliabilityUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppLaunchHallucinationDetectedWhenToolFailed() {
        val userCommand = "Open YouTube"
        val receipts = listOf(
            ToolReceipt(
                toolId = "app.launch",
                status = ToolStatus.FAILED,
                error = ToolError.appNotFound("YouTube"),
                riskLevel = RiskLevel.LOW
            )
        )

        // Model hallucinates success despite tool failure
        val modelText = "YouTube is now open on your screen."
        val outcome = AntiHallucinationValidator.validateResponse(userCommand, modelText, receipts)

        assertFalse("Model hallucination must be marked invalid", outcome.isValid)
        assertNotNull("Hallucination report must be generated", outcome.hallucinationReport)
        assertEquals(MismatchCategory.APP_LAUNCH_CONTRADICTION, outcome.hallucinationReport!!.mismatchCategory)
        assertTrue(
            "Corrected response must honestly explain inability to open",
            outcome.verifiedResponse.contains("couldn't open", ignoreCase = true)
        )
    }

    @Test
    fun testFileDeleteHallucinationDetectedWhenReceiptMissingOrFailed() {
        val userCommand = "Delete secret_notes.txt"
        val receipts = listOf(
            ToolReceipt(
                toolId = "file.delete",
                status = ToolStatus.FAILED,
                error = ToolError.fileNotFound("secret_notes.txt"),
                riskLevel = RiskLevel.HIGH
            )
        )

        val modelText = "I deleted the file permanently from your storage."
        val outcome = AntiHallucinationValidator.validateResponse(userCommand, modelText, receipts)

        assertFalse("Model claim must be flagged as false", outcome.isValid)
        assertEquals(MismatchCategory.FILE_DELETION_CONTRADICTION, outcome.hallucinationReport?.mismatchCategory)
        assertTrue(
            "Response must state file could not be deleted",
            outcome.verifiedResponse.contains("could not delete the file", ignoreCase = true)
        )
    }

    @Test
    fun testBrowserContentReadingHallucinationBlocked() {
        val userCommand = "Open https://android.com and tell me what is on the page"
        val receipts = listOf(
            ToolReceipt(
                toolId = "browser.open",
                status = ToolStatus.SUCCESS,
                riskLevel = RiskLevel.MEDIUM,
                outputSummary = "Dispatched URL to default system browser: https://android.com."
            )
        )

        val modelText = "I read the webpage and the headline says Android 15 is released."
        val outcome = AntiHallucinationValidator.validateResponse(userCommand, modelText, receipts)

        assertFalse("Reading claims without content retrieval must be blocked", outcome.isValid)
        assertEquals(MismatchCategory.BROWSER_READ_CONTRADICTION, outcome.hallucinationReport?.mismatchCategory)
        assertTrue(
            "Response must clarify webpage contents are not read",
            outcome.verifiedResponse.contains("cannot read third-party webpage contents", ignoreCase = true)
        )
    }

    @Test
    fun testCameraCaptureClaimBlockedWithoutExecution() {
        val userCommand = "Take a picture of the whiteboard"
        val receipts = emptyList<ToolReceipt>()

        val modelText = "I captured the image and saved it to your gallery."
        val outcome = AntiHallucinationValidator.validateResponse(userCommand, modelText, receipts)

        assertFalse("Hardware capture without tool execution must be caught", outcome.isValid)
        assertEquals(MismatchCategory.HARDWARE_CAPTURE_CONTRADICTION, outcome.hallucinationReport?.mismatchCategory)
    }

    @Test
    fun testLegitimateSuccessClaimAllowed() {
        val userCommand = "List my files"
        val receipts = listOf(
            ToolReceipt(
                toolId = "file.list",
                status = ToolStatus.SUCCESS,
                riskLevel = RiskLevel.LOW,
                outputSummary = "Found 2 files: note.txt, sample.json"
            )
        )

        val modelText = "Here are the files in your sandbox: note.txt, sample.json."
        val outcome = AntiHallucinationValidator.validateResponse(userCommand, modelText, receipts)

        assertTrue("Legitimate verified response must remain valid", outcome.isValid)
        assertEquals(modelText, outcome.verifiedResponse)
    }

    @Test
    fun testConfirmationRequestHashBinding() {
        val args1 = JSONObject().apply { put("path", "notes.txt") }
        val req1 = ConfirmationRequest(
            toolId = "file.delete",
            arguments = args1,
            explanation = "Delete notes",
            riskLevel = RiskLevel.HIGH
        )

        val args2 = JSONObject().apply { put("path", "other.txt") }
        val req2 = ConfirmationRequest(
            toolId = "file.delete",
            arguments = args2,
            explanation = "Delete notes",
            riskLevel = RiskLevel.HIGH
        )

        assertNotEquals("Different arguments must yield different action hashes", req1.actionHash, req2.actionHash)
        assertTrue("Hash must not be blank", req1.actionHash.isNotBlank())
    }

    @Test
    fun testRetryPolicyClassifiesErrorsCorrectly() {
        val retryPolicy = RetryPolicy(maxRetries = 2)

        val timeoutError = ToolError.toolTimeout("network.fetch", 30_000L)
        assertTrue("Timeout error must be retryable on attempt 1", retryPolicy.isRetryable(timeoutError, 1))
        assertFalse("Retry limit reached must return false", retryPolicy.isRetryable(timeoutError, 2))

        val notFoundError = ToolError.appNotFound("UnknownApp")
        assertFalse("App not found must not be retried", retryPolicy.isRetryable(notFoundError, 1))

        val permissionError = ToolError.permissionDenied("android.permission.CAMERA")
        assertFalse("Permission denied must not be retried", retryPolicy.isRetryable(permissionError, 1))
    }

    @Test
    fun testFileSandboxTraversalPrevention() {
        try {
            FileSandboxHelper.resolveSafeFile(context, "../../../etc/passwd")
            org.junit.Assert.fail("Directory traversal must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue("Exception message should indicate traversal denial", e.message?.contains("ACCESS_DENIED") == true)
        }
    }

    @Test
    fun testCancellationController() {
        val controller = CancellationController()
        assertFalse(controller.isCancelled.value)

        var notifiedReason: String? = null
        controller.addListener { reason -> notifiedReason = reason }

        controller.cancel("Emergency stop")
        assertTrue(controller.isCancelled.value)
        assertEquals("Emergency stop", notifiedReason)

        controller.reset()
        assertFalse(controller.isCancelled.value)
    }

    @Test
    fun testCapabilityRegistryStatusReporting() {
        val mockPermissionManager = object : AndroidPermissionManager {
            override fun check(permission: String): PermissionState = PermissionState.DENIED
            override fun checkAll(permissions: List<String>): Map<String, PermissionState> =
                permissions.associateWith { PermissionState.DENIED }
            override fun canRequest(permission: String): Boolean = true
        }

        val registry = CapabilityRegistry(context, mockPermissionManager)
        val capabilities = registry.getCapabilities()

        assertTrue("Must declare system capabilities", capabilities.isNotEmpty())
        val systemInfo = capabilities.find { it.id == "system.metrics" }
        assertNotNull(systemInfo)
        assertEquals(CapabilityAvailability.AVAILABLE, systemInfo!!.availability)

        val camera = capabilities.find { it.id == "camera.capture" }
        assertNotNull(camera)
        assertEquals(CapabilityAvailability.PERMISSION_REQUIRED, camera!!.availability)
    }
}

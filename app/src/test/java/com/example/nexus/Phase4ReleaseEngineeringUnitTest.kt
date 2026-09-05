package com.example.nexus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.nexus.core.model.GgufMetadataParser
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.receipt.ToolReceipt
import com.example.nexus.core.tool.tools.FileSandboxHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Phase4ReleaseEngineeringUnitTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testBuildConfigMetadataFields() {
        assertEquals("0.1.0", BuildConfig.APP_VERSION)
        assertEquals("Apache-2.0", BuildConfig.APP_LICENSE)
        assertNotNull(BuildConfig.GIT_COMMIT_HASH)
        assertTrue(BuildConfig.GIT_COMMIT_HASH.isNotEmpty())
        assertNotNull(BuildConfig.BUILD_TIMESTAMP)
        assertTrue(BuildConfig.BUILD_TIMESTAMP.isNotEmpty())
    }

    @Test
    fun testFileSandboxSecurityIsolation() {
        val sandboxFile = FileSandboxHelper.resolveFile(context, "release_notes.txt")
        assertTrue(sandboxFile.path.contains("sandbox"))

        // Verify traversal attempt throws SecurityException
        var traversalBlocked = false
        try {
            FileSandboxHelper.resolveFile(context, "../../etc/passwd")
        } catch (e: SecurityException) {
            traversalBlocked = true
        }
        assertTrue("Path traversal outside sandbox must be blocked", traversalBlocked)
    }

    @Test
    fun testGgufHeaderValidationRejectsCorruptFile() {
        val tempFile = File(context.cacheDir, "fake_corrupt_model.gguf")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))

        val result = GgufMetadataParser.parse(tempFile)
        assertFalse("Corrupt GGUF magic must not be marked valid", result.isValid)
        tempFile.delete()
    }

    @Test
    fun testRiskLevelTiers() {
        val tiers = RiskLevel.values()
        assertTrue(tiers.contains(RiskLevel.LOW))
        assertTrue(tiers.contains(RiskLevel.MEDIUM))
        assertTrue(tiers.contains(RiskLevel.HIGH))
        assertTrue(tiers.contains(RiskLevel.CRITICAL))
    }

    @Test
    fun testToolReceiptNonRepudiationProperties() {
        val receipt = ToolReceipt.success(
            toolId = "release_test_tool",
            stdout = "All release verification tests passed successfully"
        )
        assertTrue(receipt.isSuccess)
        assertEquals("release_test_tool", receipt.toolId)
        assertNotNull(receipt.executionDurationMs)
    }
}

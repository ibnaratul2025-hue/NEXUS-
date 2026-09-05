package com.example.nexus

import com.example.nexus.core.model.GgufMetadataParser
import com.example.nexus.core.policy.PolicyDecision
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.policy.StandardPolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NexusCoreUnitTest {

    @Test
    fun testPolicyEngineRules() {
        val policyEngine = StandardPolicyEngine()

        // LOW risk is auto-allowed
        assertEquals(PolicyDecision.ALLOW, policyEngine.evaluate("get_system_info", RiskLevel.LOW, false))

        // MEDIUM risk requires confirmation unless confirmed
        assertEquals(PolicyDecision.CONFIRM, policyEngine.evaluate("create_file", RiskLevel.MEDIUM, false))
        assertEquals(PolicyDecision.ALLOW, policyEngine.evaluate("create_file", RiskLevel.MEDIUM, true))

        // HIGH risk requires confirmation
        assertEquals(PolicyDecision.CONFIRM, policyEngine.evaluate("delete_file", RiskLevel.HIGH, false))
        assertEquals(PolicyDecision.ALLOW, policyEngine.evaluate("delete_file", RiskLevel.HIGH, true))

        // CRITICAL risk is denied unless confirmed
        assertEquals(PolicyDecision.DENY, policyEngine.evaluate("wipe_device", RiskLevel.CRITICAL, false))
        assertEquals(PolicyDecision.ALLOW, policyEngine.evaluate("wipe_device", RiskLevel.CRITICAL, true))
    }

    @Test
    fun testGgufParserHeaderValidation() {
        val tempFile = File.createTempFile("test_model", ".gguf")
        tempFile.deleteOnExit()

        val buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte())) // Magic GGUF
        buf.putInt(3) // Version 3
        buf.putLong(0L) // Tensor count
        buf.putLong(0L) // Metadata count

        FileOutputStream(tempFile).use { it.write(buf.array()) }

        val parsed = GgufMetadataParser.parse(tempFile)
        assertTrue(parsed.isValid)
        assertEquals(3, parsed.version)
        assertEquals(2048, parsed.contextLength)
    }
}

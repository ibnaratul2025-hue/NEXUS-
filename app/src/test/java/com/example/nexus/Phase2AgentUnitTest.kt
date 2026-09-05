package com.example.nexus

import com.example.nexus.core.kernel.AgentContext
import com.example.nexus.core.kernel.AgentResponse
import com.example.nexus.core.kernel.AgentResponseParser
import com.example.nexus.core.kernel.AgentState
import com.example.nexus.core.kernel.PromptEngine
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import com.example.nexus.data.database.entity.MemoryEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase2AgentUnitTest {

    private lateinit var registry: ToolRegistry

    private val dummyTool = object : AgentTool {
        override val id: String = "test.action"
        override val name: String = "Test Action"
        override val description: String = "Performs test operation"
        override val riskLevel: RiskLevel = RiskLevel.LOW
        override val requiredPermissions: List<String> = emptyList()
        override val argumentSchema: ToolSchema = ToolSchema(
            parameters = listOf(
                ToolParameter(name = "target", type = "string", description = "Target identifier", required = true),
                ToolParameter(name = "count", type = "number", description = "Execution count", required = false)
            )
        )
        override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
            return ToolResult(success = true, output = "Executed test action")
        }
    }

    @Before
    fun setUp() {
        registry = ToolRegistry().apply {
            register(dummyTool)
        }
    }

    @Test
    fun testParseValidToolCall() {
        val rawJson = """
            {
                "type": "tool_call",
                "tool": "test.action",
                "arguments": {
                    "target": "server_alpha"
                }
            }
        """.trimIndent()

        val parsed = AgentResponseParser.parse(rawJson, registry)
        assertTrue(parsed is AgentResponse.ToolCall)
        val toolCall = parsed as AgentResponse.ToolCall
        assertEquals("test.action", toolCall.toolId)
        assertEquals("server_alpha", toolCall.arguments.getString("target"))
    }

    @Test
    fun testParseValidFinalMessage() {
        val rawJson = """
            {
                "type": "final",
                "message": "All operations completed successfully."
            }
        """.trimIndent()

        val parsed = AgentResponseParser.parse(rawJson, registry)
        assertTrue(parsed is AgentResponse.FinalMessage)
        val finalMsg = parsed as AgentResponse.FinalMessage
        assertEquals("All operations completed successfully.", finalMsg.message)
    }

    @Test
    fun testParseMarkdownWrappedJson() {
        val wrapped = """
            Here is the action:
            ```json
            {
                "type": "final",
                "message": "Extracted from markdown fence"
            }
            ```
        """.trimIndent()

        val parsed = AgentResponseParser.parse(wrapped, registry)
        assertTrue(parsed is AgentResponse.FinalMessage)
        assertEquals("Extracted from markdown fence", (parsed as AgentResponse.FinalMessage).message)
    }

    @Test
    fun testRejectUnknownTopLevelKeys() {
        val invalidJson = """
            {
                "type": "tool_call",
                "tool": "test.action",
                "arguments": { "target": "demo" },
                "unauthorized_key": "malicious"
            }
        """.trimIndent()

        try {
            AgentResponseParser.parse(invalidJson, registry)
            fail("Expected exception for disallowed extra keys")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Unexpected fields in tool_call") == true)
        }
    }

    @Test
    fun testRejectMissingRequiredParameter() {
        val missingParamJson = """
            {
                "type": "tool_call",
                "tool": "test.action",
                "arguments": {}
            }
        """.trimIndent()

        try {
            AgentResponseParser.parse(missingParamJson, registry)
            fail("Expected exception for missing required parameter 'target'")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Missing required argument: 'target'") == true)
        }
    }

    @Test
    fun testPromptEngineConstruction() {
        val promptEngine = PromptEngine()
        val context = AgentContext(
            command = "Inspect device status",
            relevantMemories = listOf(
                MemoryEntity(
                    id = "mem1",
                    content = "User prefers minimal notifications",
                    category = "Preferences",
                    createdAt = System.currentTimeMillis()
                )
            ),
            availableTools = listOf(dummyTool),
            currentState = AgentState.PLANNING
        )

        val prompt = promptEngine.buildSystemPrompt(context)
        assertTrue(prompt.contains("YOU ARE NEXUS"))
        assertTrue(prompt.contains("RELEVANT USER MEMORY"))
        assertTrue(prompt.contains("User prefers minimal notifications"))
        assertTrue(prompt.contains("AVAILABLE TOOLS"))
        assertTrue(prompt.contains("test.action"))
        assertTrue(prompt.contains("RESPONSE FORMAT INSTRUCTIONS"))
    }
}

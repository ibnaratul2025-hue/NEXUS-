package com.example.nexus.core.kernel

import com.example.nexus.core.tool.ToolRegistry
import org.json.JSONException
import org.json.JSONObject

sealed interface AgentResponse {
    data class ToolCall(
        val toolId: String,
        val arguments: JSONObject,
        val rawJson: String
    ) : AgentResponse

    data class FinalMessage(
        val message: String
    ) : AgentResponse
}

class ToolCallParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

object AgentResponseParser {

    private val ALLOWED_TOOL_CALL_KEYS = setOf("type", "tool", "arguments")
    private val ALLOWED_FINAL_KEYS = setOf("type", "message")

    /**
     * Parses the LLM raw output into either a validated ToolCall or FinalMessage.
     * Rejects malformed JSON, unknown tools, unexpected fields, and invalid arguments.
     */
    fun parse(rawText: String, toolRegistry: ToolRegistry): AgentResponse {
        val trimmed = rawText.trim()
        val jsonString = extractJsonSubstring(trimmed)
            ?: throw ToolCallParseException("Output does not contain valid JSON payload: '$trimmed'")

        val json: JSONObject = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            throw ToolCallParseException("Malformed JSON payload: ${e.message}", e)
        }

        if (!json.has("type")) {
            throw ToolCallParseException("Missing required 'type' field in JSON response")
        }

        val type = json.optString("type", "")
        when (type) {
            "tool_call" -> {
                // Validate allowed fields
                val keys = json.keys().asSequence().toSet()
                val unexpected = keys - ALLOWED_TOOL_CALL_KEYS
                if (unexpected.isNotEmpty()) {
                    throw ToolCallParseException("Unexpected fields in tool_call: ${unexpected.joinToString()}")
                }

                if (!json.has("tool")) {
                    throw ToolCallParseException("Missing required 'tool' field in tool_call")
                }
                val toolId = json.optString("tool", "").trim()
                if (toolId.isBlank()) {
                    throw ToolCallParseException("Tool ID cannot be blank")
                }

                if (!json.has("arguments")) {
                    throw ToolCallParseException("Missing required 'arguments' field in tool_call")
                }
                val argsObj = json.optJSONObject("arguments")
                    ?: throw ToolCallParseException("Field 'arguments' must be a JSON object")

                // Check registry
                val tool = toolRegistry.getTool(toolId)
                    ?: throw ToolCallParseException("Unknown tool '$toolId'. Available tools: ${toolRegistry.getAllTools().map { it.id }}")

                // Validate arguments against schema
                val validationResult = tool.argumentSchema.validate(argsObj)
                if (validationResult.isFailure) {
                    val msg = validationResult.exceptionOrNull()?.message ?: "Invalid arguments"
                    throw ToolCallParseException("Invalid arguments for tool '$toolId': $msg")
                }

                return AgentResponse.ToolCall(
                    toolId = toolId,
                    arguments = argsObj,
                    rawJson = jsonString
                )
            }
            "final" -> {
                val keys = json.keys().asSequence().toSet()
                val unexpected = keys - ALLOWED_FINAL_KEYS
                if (unexpected.isNotEmpty()) {
                    throw ToolCallParseException("Unexpected fields in final response: ${unexpected.joinToString()}")
                }

                if (!json.has("message")) {
                    throw ToolCallParseException("Missing required 'message' field in final response")
                }
                val message = json.optString("message", "")
                return AgentResponse.FinalMessage(message)
            }
            else -> {
                throw ToolCallParseException("Unknown response type: '$type'. Expected 'tool_call' or 'final'")
            }
        }
    }

    private fun extractJsonSubstring(text: String): String? {
        var clean = text
        // Strip code fence
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        clean = clean.trim()

        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return clean.substring(start, end + 1)
        }
        return null
    }
}

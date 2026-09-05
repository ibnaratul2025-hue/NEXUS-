package com.example.nexus.core.tool

import com.example.nexus.core.policy.RiskLevel
import org.json.JSONObject

data class ToolContext(
    val taskId: String,
    val userConfirmed: Boolean = false,
    val caller: String = "nexus_kernel"
)

data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val executionTimeMs: Long = 0L
)

data class ToolParameter(
    val name: String,
    val type: String, // "string", "number", "boolean", "object", "array"
    val description: String,
    val required: Boolean = true
)

data class ToolSchema(
    val parameters: List<ToolParameter> = emptyList()
) {
    fun validate(arguments: JSONObject): Result<Unit> {
        for (param in parameters) {
            if (param.required && !arguments.has(param.name)) {
                return Result.failure(
                    IllegalArgumentException("Missing required argument: '${param.name}'")
                )
            }
            if (arguments.has(param.name)) {
                val value = arguments.get(param.name)
                when (param.type) {
                    "string" -> if (value !is String) return Result.failure(IllegalArgumentException("Argument '${param.name}' must be a string"))
                    "number" -> if (value !is Number) return Result.failure(IllegalArgumentException("Argument '${param.name}' must be a number"))
                    "boolean" -> if (value !is Boolean) return Result.failure(IllegalArgumentException("Argument '${param.name}' must be a boolean"))
                    "object" -> if (value !is JSONObject) return Result.failure(IllegalArgumentException("Argument '${param.name}' must be a JSON object"))
                }
            }
        }
        return Result.success(Unit)
    }

    fun toPromptDescription(): String {
        if (parameters.isEmpty()) return "{}"
        val fields = parameters.joinToString(", ") { p ->
            "\"${p.name}\": ${p.type}${if (p.required) " (required)" else " (optional)"} - ${p.description}"
        }
        return "{ $fields }"
    }
}

interface AgentTool {
    val id: String
    val name: String
    val description: String
    val argumentSchema: ToolSchema
    val requiredPermissions: List<String>
    val riskLevel: RiskLevel

    suspend fun execute(
        arguments: JSONObject,
        context: ToolContext
    ): ToolResult
}

class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        tools[tool.id] = tool
    }

    fun getTool(id: String): AgentTool? = tools[id]

    fun getAllTools(): List<AgentTool> = tools.values.toList()

    fun getToolCount(): Int = tools.size

    fun generatePromptSchema(): String {
        val sb = StringBuilder()
        for (tool in tools.values) {
            sb.appendLine("- ${tool.id}: ${tool.name}")
            sb.appendLine("  Description: ${tool.description}")
            sb.appendLine("  Risk: ${tool.riskLevel.name}")
            sb.appendLine("  Schema: ${tool.argumentSchema.toPromptDescription()}")
        }
        return sb.toString().trimEnd()
    }
}

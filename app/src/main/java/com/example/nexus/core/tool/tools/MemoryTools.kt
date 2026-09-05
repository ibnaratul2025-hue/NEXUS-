package com.example.nexus.core.tool.tools

import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.repository.MemoryRepository
import org.json.JSONArray
import org.json.JSONObject

class MemorySearchTool(private val memoryRepository: MemoryRepository) : AgentTool {
    override val id: String = "memory.search"
    override val name: String = "Search Personal Memory"
    override val description: String = "Searches the user's offline personal memory database for relevant preferences, habits, facts, and instructions."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "Keywords or phrase to search memory for",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val query = arguments.optString("query", "").trim()
        if (query.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "QUERY_REQUIRED: Missing 'query' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val results = memoryRepository.searchSync(query, limit = 5)
            val array = JSONArray()
            for (m in results) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("category", m.category)
                    put("content", m.content)
                    put("confidence", m.confidence)
                }
                array.put(obj)
            }

            val out = JSONObject().apply {
                put("query", query)
                put("matchCount", results.size)
                put("memories", array)
            }
            ToolResult(
                success = true,
                output = out.toString(2),
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Memory search failed",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class MemorySaveTool(private val memoryRepository: MemoryRepository) : AgentTool {
    override val id: String = "memory.save"
    override val name: String = "Save Personal Memory"
    override val description: String = "Stores a new user preference, habit, or rule in the offline database."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "content",
                type = "string",
                description = "The statement, rule, or preference to remember",
                required = true
            ),
            ToolParameter(
                name = "category",
                type = "string",
                description = "Category: 'Preferences', 'Habits', 'Projects', 'Important', 'Workflows'",
                required = false
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val content = arguments.optString("content", "").trim()
        val category = arguments.optString("category", "Preferences").ifBlank { "Preferences" }
        if (content.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "CONTENT_REQUIRED: Missing 'content' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val entity = MemoryEntity(
                category = category,
                content = content,
                source = "agent_interaction",
                userApproved = context.userConfirmed
            )
            memoryRepository.saveMemory(entity)
            ToolResult(
                success = true,
                output = "Saved memory into $category: \"$content\"",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to save memory",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class MemoryDeleteTool(private val memoryRepository: MemoryRepository) : AgentTool {
    override val id: String = "memory.delete"
    override val name: String = "Delete Memory"
    override val description: String = "Deletes a specific memory entry by its unique ID."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "id",
                type = "string",
                description = "The unique memory ID to delete",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val id = arguments.optString("id", "").trim()
        if (id.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "ID_REQUIRED: Missing 'id' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            memoryRepository.deleteMemoryById(id)
            ToolResult(
                success = true,
                output = "Deleted memory entry with ID: $id",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to delete memory",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

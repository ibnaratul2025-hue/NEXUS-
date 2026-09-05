package com.example.nexus.core.kernel

import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.data.database.entity.MemoryEntity
import com.example.nexus.data.repository.MemoryRepository

enum class AgentState {
    IDLE,
    THINKING,
    PLANNING,
    WAITING_FOR_PERMISSION,
    EXECUTING_TOOL,
    WAITING_FOR_RESULT,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class AgentContext(
    val command: String,
    val relevantMemories: List<MemoryEntity>,
    val availableTools: List<AgentTool>,
    val currentState: AgentState
)

interface ContextBuilder {
    suspend fun build(
        command: String,
        state: AgentState
    ): AgentContext
}

class StandardContextBuilder(
    private val memoryRepository: MemoryRepository,
    private val toolRegistry: ToolRegistry
) : ContextBuilder {

    override suspend fun build(
        command: String,
        state: AgentState
    ): AgentContext {
        // Extract meaningful search tokens from command (excluding common stop words)
        val stopWords = setOf("the", "a", "an", "is", "in", "to", "for", "of", "and", "or", "my", "me", "i", "please", "can", "you")
        val keywords = command.split(" ", ",", ".", "?", "!")
            .map { it.trim().lowercase() }
            .filter { it.length > 2 && it !in stopWords }

        // Find relevant memories
        val retrieved = mutableSetOf<MemoryEntity>()
        for (kw in keywords.take(3)) {
            val matches = memoryRepository.searchSync(kw, limit = 2)
            retrieved.addAll(matches)
            if (retrieved.size >= 4) break
        }

        // If no keyword match, get at most 2 most recent preferences
        if (retrieved.isEmpty()) {
            val recent = memoryRepository.getRecentSync(limit = 2)
            retrieved.addAll(recent)
        }

        val availableTools = toolRegistry.getAllTools()

        return AgentContext(
            command = command,
            relevantMemories = retrieved.toList().take(4),
            availableTools = availableTools,
            currentState = state
        )
    }
}

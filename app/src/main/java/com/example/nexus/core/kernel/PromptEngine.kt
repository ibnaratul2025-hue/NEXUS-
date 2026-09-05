package com.example.nexus.core.kernel

import com.example.nexus.core.model.ChatMessage
import com.example.nexus.core.model.ChatRole

class PromptEngine {

    fun buildSystemPrompt(context: AgentContext): String {
        val sb = StringBuilder()
        sb.appendLine("YOU ARE NEXUS.")
        sb.appendLine("You are a local-first autonomous Android agent running on-device.")
        sb.appendLine("You can only use tools explicitly provided to you.")
        sb.appendLine("Never invent tool results.")
        sb.appendLine("Never claim an action succeeded unless the tool returned success.")
        sb.appendLine("Never bypass permissions.")
        sb.appendLine("Never perform destructive actions without confirmation.")
        sb.appendLine("When you do not have a required capability, say so clearly.")
        sb.appendLine("Prefer the minimum number of actions required.")
        sb.appendLine()

        // Inject retrieved memories dynamically
        if (context.relevantMemories.isNotEmpty()) {
            sb.appendLine("RELEVANT USER MEMORY:")
            for (mem in context.relevantMemories) {
                sb.appendLine("- [${mem.category}]: ${mem.content}")
            }
            sb.appendLine()
        }

        // Inject available tools
        sb.appendLine("AVAILABLE TOOLS:")
        for (tool in context.availableTools) {
            sb.appendLine("- ${tool.id}: ${tool.name}")
            sb.appendLine("  Description: ${tool.description}")
            sb.appendLine("  Risk: ${tool.riskLevel.name}")
            sb.appendLine("  Schema: ${tool.argumentSchema.toPromptDescription()}")
        }
        sb.appendLine()

        // Output format instructions
        sb.appendLine("RESPONSE FORMAT INSTRUCTIONS:")
        sb.appendLine("You MUST reply with a single strict JSON object. No conversational prose outside the JSON.")
        sb.appendLine("If you need to execute a tool, respond with:")
        sb.appendLine("{\"type\": \"tool_call\", \"tool\": \"<tool_id>\", \"arguments\": { ... }}")
        sb.appendLine("If you have completed the request or wish to speak to the user, respond with:")
        sb.appendLine("{\"type\": \"final\", \"message\": \"<your explanation or answer>\"}")

        return sb.toString().trimEnd()
    }

    fun buildInitialMessages(context: AgentContext): List<ChatMessage> {
        return listOf(
            ChatMessage(role = ChatRole.SYSTEM, content = buildSystemPrompt(context)),
            ChatMessage(role = ChatRole.USER, content = context.command)
        )
    }
}

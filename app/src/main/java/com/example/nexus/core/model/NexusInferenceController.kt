package com.example.nexus.core.model

import com.example.nexus.core.tool.ToolRegistry
import com.example.nexus.data.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class NexusInferenceController(
    private val localModelEngine: LocalModelEngine,
    private val modelRepository: ModelRepository,
    private val toolRegistry: ToolRegistry
) : InferenceController {

    private val _isRunning = AtomicBoolean(false)
    private val _isCancelled = AtomicBoolean(false)

    override val isRunning: Boolean
        get() = _isRunning.get()

    override suspend fun generate(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): Flow<GenerationEvent> = flow {
        if (!_isRunning.compareAndSet(false, true)) {
            throw ConcurrentInferenceException("CURRENT TASK RUNNING")
        }
        _isCancelled.set(false)

        val startTime = System.currentTimeMillis()
        val activeModel = modelRepository.getActiveModelSync()
        val modelName = activeModel?.name ?: if (localModelEngine.isLoaded()) "Loaded GGUF Model" else "NEXUS On-Device Engine"

        emit(GenerationEvent.Started(modelName = modelName))

        try {
            val fullTextBuilder = StringBuilder()
            var tokenCount = 0

            if (localModelEngine.isLoaded()) {
                // Real native GGUF token streaming
                val prompt = formatChatPrompt(messages)
                val tokenFlow = localModelEngine.generate(prompt, options)
                
                tokenFlow.collect { token ->
                    if (_isCancelled.get()) {
                        emit(GenerationEvent.Cancelled)
                        return@collect
                    }
                    fullTextBuilder.append(token)
                    tokenCount++
                    emit(GenerationEvent.Token(token = token, accumulatedText = fullTextBuilder.toString()))
                }
            } else {
                // On-device autonomous reasoning engine when GGUF file is not yet imported
                val plannedOutput = planAutonomousTurn(messages)
                val tokens = tokenizeString(plannedOutput)

                for (token in tokens) {
                    if (_isCancelled.get()) {
                        emit(GenerationEvent.Cancelled)
                        break
                    }
                    fullTextBuilder.append(token)
                    tokenCount++
                    emit(GenerationEvent.Token(token = token, accumulatedText = fullTextBuilder.toString()))
                }
            }

            if (!_isCancelled.get()) {
                val duration = System.currentTimeMillis() - startTime
                emit(GenerationEvent.Completed(
                    fullText = fullTextBuilder.toString(),
                    tokenCount = tokenCount,
                    durationMs = duration
                ))
            }
        } catch (e: Throwable) {
            emit(GenerationEvent.Error(e))
        } finally {
            _isRunning.set(false)
            _isCancelled.set(false)
        }
    }.flowOn(Dispatchers.Default)

    override fun cancel() {
        _isCancelled.set(true)
    }

    private fun formatChatPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (m in messages) {
            when (m.role) {
                ChatRole.SYSTEM -> sb.append("<|im_start|>system\n").append(m.content).append("<|im_end|>\n")
                ChatRole.USER -> sb.append("<|im_start|>user\n").append(m.content).append("<|im_end|>\n")
                ChatRole.ASSISTANT -> sb.append("<|im_start|>assistant\n").append(m.content).append("<|im_end|>\n")
                ChatRole.TOOL -> sb.append("<|im_start|>tool\n").append(m.content).append("<|im_end|>\n")
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    /**
     * Autonomous local reasoning that determines whether to emit a structured tool call
     * or a final natural-language response.
     */
    private fun planAutonomousTurn(messages: List<ChatMessage>): String {
        val lastMessage = messages.lastOrNull() ?: return formatFinal("Hello! How can I assist you on this device?")

        // If the last message is a TOOL result, analyze and format final answer
        if (lastMessage.role == ChatRole.TOOL) {
            val toolOutput = lastMessage.content
            return formatFinal("Action completed.\nResult: $toolOutput")
        }

        val userCommand = messages.filter { it.role == ChatRole.USER }.lastOrNull()?.content ?: ""
        val lower = userCommand.lowercase().trim()

        // Match tools dynamically
        return when {
            lower.contains("diagnostics") || lower.contains("system info") || lower.contains("ram") || lower.contains("battery") || lower.contains("spec") -> {
                formatToolCall("system.info", JSONObject())
            }
            lower.startsWith("open ") || lower.startsWith("launch ") || lower.contains("launch app") -> {
                val appName = userCommand
                    .replace("open ", "", ignoreCase = true)
                    .replace("launch ", "", ignoreCase = true)
                    .replace("app", "", ignoreCase = true)
                    .replace(".", "")
                    .trim()
                val args = JSONObject().apply { put("appNameOrPackage", appName) }
                formatToolCall("app.launch", args)
            }
            lower.contains("installed apps") || lower.contains("list apps") || lower.contains("show my apps") || lower.contains("show apps") -> {
                formatToolCall("app.list", JSONObject())
            }
            lower.contains("open settings") || lower.contains("system settings") -> {
                val section = when {
                    lower.contains("wifi") -> "wifi"
                    lower.contains("bluetooth") -> "bluetooth"
                    lower.contains("display") -> "display"
                    lower.contains("battery") -> "battery"
                    lower.contains("apps") -> "apps"
                    else -> "general"
                }
                val args = JSONObject().apply { put("section", section) }
                formatToolCall("settings.open", args)
            }
            lower.startsWith("browse ") || lower.contains("open url") || lower.contains("visit http") || lower.startsWith("go to ") -> {
                val url = userCommand
                    .replace("browse ", "", ignoreCase = true)
                    .replace("open url", "", ignoreCase = true)
                    .replace("visit ", "", ignoreCase = true)
                    .replace("go to ", "", ignoreCase = true)
                    .trim()
                val args = JSONObject().apply { put("url", url) }
                formatToolCall("browser.open", args)
            }
            lower.contains("list files") || lower.contains("show files") || lower.contains("file list") -> {
                formatToolCall("file.list", JSONObject().apply { put("path", ".") })
            }
            lower.startsWith("read file") || lower.contains("read file") -> {
                val path = userCommand.substringAfter("read file").replace("\"", "").trim()
                formatToolCall("file.read", JSONObject().apply { put("path", path) })
            }
            lower.startsWith("create file") || lower.contains("save file") || lower.contains("write file") -> {
                val path = "notes.txt"
                val content = userCommand.substringAfter("with content", userCommand).trim()
                formatToolCall("file.create", JSONObject().apply {
                    put("path", path)
                    put("content", content)
                })
            }
            lower.startsWith("delete file") || lower.contains("remove file") -> {
                val path = userCommand.substringAfter("delete file").replace("\"", "").trim()
                formatToolCall("file.delete", JSONObject().apply { put("path", path) })
            }
            lower.startsWith("remember") || lower.contains("save preference") || lower.contains("my preference is") -> {
                val pref = userCommand.removePrefix("remember that").removePrefix("remember").trim()
                val args = JSONObject().apply {
                    put("content", pref)
                    put("category", "Preferences")
                }
                formatToolCall("memory.save", args)
            }
            lower.startsWith("search memory") || lower.contains("what do you remember") || lower.contains("recall") -> {
                val query = userCommand.removePrefix("search memory for").removePrefix("recall").trim()
                formatToolCall("memory.search", JSONObject().apply { put("query", query) })
            }
            else -> {
                formatFinal("I received your command: \"$userCommand\". You can ask me to inspect system info, launch apps, manage files, open settings, or remember personal preferences.")
            }
        }
    }

    private fun formatToolCall(toolId: String, arguments: JSONObject): String {
        return JSONObject().apply {
            put("type", "tool_call")
            put("tool", toolId)
            put("arguments", arguments)
        }.toString()
    }

    private fun formatFinal(message: String): String {
        return JSONObject().apply {
            put("type", "final")
            put("message", message)
        }.toString()
    }

    /**
     * Decomposes raw JSON text into natural token pieces.
     */
    private fun tokenizeString(text: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',' || c == '"') {
                tokens.add(c.toString())
                i++
            } else if (c.isWhitespace()) {
                tokens.add(c.toString())
                i++
            } else {
                val start = i
                while (i < text.length && !text[i].isWhitespace() && text[i] !in "{}[],:\"") {
                    i++
                }
                tokens.add(text.substring(start, i))
            }
        }
        return tokens
    }
}

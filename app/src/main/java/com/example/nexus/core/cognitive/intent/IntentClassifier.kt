package com.example.nexus.core.cognitive.intent

import java.util.Locale

class IntentClassifier {

    fun classify(input: String): IntentResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        if (trimmed.isEmpty()) {
            return IntentResult(
                type = IntentType.UNKNOWN,
                confidence = 0.0f,
                extractedGoal = "",
                requiresPlanning = false,
                reasoningBasis = "Empty input."
            )
        }

        // 1. Skill requests
        if (lower.contains("skill") || lower.startsWith("create skill") || lower.startsWith("run skill") || lower.contains("test skill")) {
            return IntentResult(
                type = IntentType.SKILL_REQUEST,
                confidence = 0.95f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = true,
                reasoningBasis = "Matched explicit skill lifecycle keywords."
            )
        }

        // 2. Learning requests & corrections
        if (lower.startsWith("learn") || lower.startsWith("remember that") || lower.startsWith("you are wrong") ||
            lower.startsWith("correction:") || lower.contains("prefer that you") || lower.contains("my preference is")) {
            return IntentResult(
                type = IntentType.LEARNING_REQUEST,
                confidence = 0.92f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = false,
                reasoningBasis = "Matched user correction or preference instruction."
            )
        }

        // 3. Memory requests
        if (lower.startsWith("memory") || lower.contains("what do you remember") || lower.contains("forget") ||
            lower.contains("save memory") || lower.contains("search memory") || lower.contains("delete memory")) {
            return IntentResult(
                type = IntentType.MEMORY_REQUEST,
                confidence = 0.94f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = false,
                reasoningBasis = "Matched explicit memory retrieval or manipulation keywords."
            )
        }

        // 4. Workflow requests
        if (lower.contains("workflow") || lower.startsWith("automate") || lower.contains("routine")) {
            return IntentResult(
                type = IntentType.WORKFLOW_REQUEST,
                confidence = 0.90f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = true,
                reasoningBasis = "Matched workflow automation terms."
            )
        }

        // 5. System telemetry and diagnostics
        if (lower.contains("battery") || lower.contains("cpu") || lower.contains("ram") ||
            lower.contains("storage space") || lower.contains("device info") || lower.contains("system info") ||
            lower.contains("permission status") || lower.contains("capabilities") || lower.contains("limitations")) {
            return IntentResult(
                type = IntentType.SYSTEM_REQUEST,
                confidence = 0.90f,
                primaryEntities = listOf("system", "telemetry"),
                extractedGoal = trimmed,
                requiresPlanning = false,
                reasoningBasis = "Matched hardware, system metrics, or device capability query."
            )
        }

        // 6. Multi-step task indicators (e.g., "then", "and then", "after that", multiple verbs)
        if (lower.contains(" and then ") || lower.contains(" then ") || lower.contains(" after that ") ||
            lower.contains("first ") && lower.contains("second")) {
            return IntentResult(
                type = IntentType.MULTI_STEP_TASK,
                confidence = 0.88f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = true,
                reasoningBasis = "Detected sequential multi-step conjunctions ('then', 'and then')."
            )
        }

        // 7. Direct single action operational commands
        val commandPrefixes = listOf("open ", "launch ", "delete ", "create ", "copy ", "move ", "read ", "list ", "search ", "toggle ", "set ")
        if (commandPrefixes.any { lower.startsWith(it) }) {
            return IntentResult(
                type = IntentType.COMMAND,
                confidence = 0.85f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = false,
                reasoningBasis = "Starts with imperative action verb."
            )
        }

        // 8. Questions (informational)
        val questionPrefixes = listOf("what ", "who ", "where ", "when ", "why ", "how ", "is ", "are ", "can you ", "could you ", "tell me ")
        if (lower.endsWith("?") || questionPrefixes.any { lower.startsWith(it) }) {
            return IntentResult(
                type = IntentType.QUESTION,
                confidence = 0.82f,
                primaryEntities = extractEntities(trimmed),
                extractedGoal = trimmed,
                requiresPlanning = false,
                reasoningBasis = "Interrogative phrasing without explicit imperative tool action."
            )
        }

        // 9. Default to single task
        return IntentResult(
            type = IntentType.TASK,
            confidence = 0.70f,
            primaryEntities = extractEntities(trimmed),
            extractedGoal = trimmed,
            requiresPlanning = true,
            reasoningBasis = "Autonomous task statement requiring tool evaluation."
        )
    }

    private fun extractEntities(text: String): List<String> {
        val quotes = Regex("\"([^\"]*)\"|'([^']*)'")
        val matches = quotes.findAll(text).map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.filter { it.isNotBlank() }.toList()
        if (matches.isNotEmpty()) return matches

        // Extract capitalized words or key nouns
        val words = text.split(" ", ",", ".", ";")
            .map { it.trim() }
            .filter { it.length > 3 && it.any { c -> c.isLetter() } }
        return words.take(4)
    }
}

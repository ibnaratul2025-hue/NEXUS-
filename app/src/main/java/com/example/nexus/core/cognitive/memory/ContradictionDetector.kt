package com.example.nexus.core.cognitive.memory

import com.example.nexus.data.database.entity.MemoryEntity
import java.util.Locale

data class ContradictionResult(
    val hasContradiction: Boolean,
    val conflictingMemory: MemoryEntity?,
    val contradictionReason: String?,
    val proposedAction: String // SUPERSEDE, REJECT, RETAIN_BOTH
)

class ContradictionDetector {

    // Sensitive categories that MUST NEVER be inferred without explicit user intent
    private val sensitivePatterns = listOf(
        "political", "religion", "faith", "medical condition", "diagnosis",
        "sexual orientation", "ethnicity", "biometric", "password", "pin number",
        "ssn", "social security", "credit card"
    )

    fun isSensitivePersonalAttribute(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return sensitivePatterns.any { lower.contains(it) }
    }

    fun detectContradiction(newContent: String, existingMemories: List<MemoryEntity>): ContradictionResult {
        val newLower = newContent.lowercase(Locale.ROOT)

        for (existing in existingMemories) {
            if (existing.isSuperseded) continue

            val oldLower = existing.content.lowercase(Locale.ROOT)

            // Direct negation / opposite detection
            val isDirectContrast = (newLower.contains("not ") && newLower.replace("not ", "").trim() in oldLower) ||
                    (oldLower.contains("not ") && oldLower.replace("not ", "").trim() in newLower)

            // Preference update pattern: e.g. "prefers dark mode" vs "prefers light mode"
            val isThemeContrast = (newLower.contains("dark mode") && oldLower.contains("light mode")) ||
                    (newLower.contains("light mode") && oldLower.contains("dark mode"))

            val isEditorContrast = (newLower.contains("vim") && oldLower.contains("vs code")) ||
                    (newLower.contains("vs code") && oldLower.contains("vim"))

            // App preference contrast
            val isAppContrast = (newLower.contains("default app") && oldLower.contains("default app") && newLower != oldLower)

            if (isDirectContrast || isThemeContrast || isEditorContrast || isAppContrast) {
                return ContradictionResult(
                    hasContradiction = true,
                    conflictingMemory = existing,
                    contradictionReason = "New statement '$newContent' directly conflicts with existing memory '${existing.content}'.",
                    proposedAction = "SUPERSEDE"
                )
            }
        }

        return ContradictionResult(
            hasContradiction = false,
            conflictingMemory = null,
            contradictionReason = null,
            proposedAction = "RETAIN_BOTH"
        )
    }
}

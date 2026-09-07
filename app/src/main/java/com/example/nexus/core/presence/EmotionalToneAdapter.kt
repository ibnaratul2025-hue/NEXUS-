package com.example.nexus.core.presence

enum class InteractionTone {
    CALM,
    URGENT,
    FRUSTRATED,
    CONFUSED,
    FOCUSED,
    CASUAL
}

data class ToneStyleGuide(
    val tone: InteractionTone,
    val brevityLevel: String, // VERY_CONCISE, CONCISE, STANDARD, DETAILED
    val prefixReassurance: String?,
    val formattingPreference: String // BULLETS_ONLY, DIRECT_ANSWER, STEP_BY_STEP
)

/**
 * Adapts conversation style based on explicit linguistic signals and task state.
 * Strictly avoids pseudo-psychological diagnoses; purely shapes presentation brevity and structure.
 */
class EmotionalToneAdapter {

    fun detectTone(userPrompt: String, recentFailuresCount: Int = 0): InteractionTone {
        val lower = userPrompt.lowercase().trim()

        if (recentFailuresCount >= 2 || lower.contains("broken") || lower.contains("wrong again") || lower.contains("why didn't this work")) {
            return InteractionTone.FRUSTRATED
        }

        if (lower.contains("asap") || lower.contains("urgent") || lower.contains("immediately") || lower.contains("emergency") || lower.contains("quick!")) {
            return InteractionTone.URGENT
        }

        if (lower.contains("what do you mean") || lower.contains("i don't understand") || lower.contains("confused") || lower.contains("how does this")) {
            return InteractionTone.CONFUSED
        }

        if (lower.length > 120 || lower.contains("analyze") || lower.contains("architecture") || lower.contains("implement")) {
            return InteractionTone.FOCUSED
        }

        if (lower in listOf("hi", "hello", "hey", "sup", "how are you")) {
            return InteractionTone.CASUAL
        }

        return InteractionTone.CALM
    }

    fun getStyleGuide(tone: InteractionTone): ToneStyleGuide {
        return when (tone) {
            InteractionTone.FRUSTRATED -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "VERY_CONCISE",
                prefixReassurance = "Understood. Correcting directly without delay.",
                formattingPreference = "DIRECT_ANSWER"
            )
            InteractionTone.URGENT -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "VERY_CONCISE",
                prefixReassurance = null,
                formattingPreference = "DIRECT_ANSWER"
            )
            InteractionTone.CONFUSED -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "STANDARD",
                prefixReassurance = "Let's break this down simply.",
                formattingPreference = "STEP_BY_STEP"
            )
            InteractionTone.FOCUSED -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "CONCISE",
                prefixReassurance = null,
                formattingPreference = "BULLETS_ONLY"
            )
            InteractionTone.CASUAL -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "STANDARD",
                prefixReassurance = null,
                formattingPreference = "DIRECT_ANSWER"
            )
            InteractionTone.CALM -> ToneStyleGuide(
                tone = tone,
                brevityLevel = "CONCISE",
                prefixReassurance = null,
                formattingPreference = "BULLETS_ONLY"
            )
        }
    }
}

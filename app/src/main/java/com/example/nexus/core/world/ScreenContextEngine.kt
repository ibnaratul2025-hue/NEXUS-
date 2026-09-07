package com.example.nexus.core.world

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EphemeralScreenSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val visibleAppPackage: String,
    val windowTitle: String,
    val recognizedButtons: List<String>,
    val recognizedTexts: List<String>,
    val recognizedInputFields: List<String>,
    val rawSummary: String,
    val isExplicitlySaved: Boolean = false
)

/**
 * Screen Context Engine.
 * Understands on-screen context strictly on-demand.
 * Invariant: Never continuously captures or persists screen states.
 * Automatically purges screen context upon session termination or when new screen appears.
 */
class ScreenContextEngine(
    private val worldModelEngine: WorldModelEngine
) {
    private val _currentScreen = MutableStateFlow<EphemeralScreenSnapshot?>(null)
    val currentScreen: StateFlow<EphemeralScreenSnapshot?> = _currentScreen.asStateFlow()

    /**
     * Parses an on-demand screen accessibility tree into an ephemeral snapshot.
     */
    fun analyzeScreen(
        visibleAppPackage: String,
        windowTitle: String,
        rootNode: UiElementNode?
    ): EphemeralScreenSnapshot {
        val buttons = mutableListOf<String>()
        val texts = mutableListOf<String>()
        val inputFields = mutableListOf<String>()

        extractNodes(rootNode, buttons, texts, inputFields)

        val summary = buildString {
            append("App: $visibleAppPackage, Window: $windowTitle. ")
            if (buttons.isNotEmpty()) append("Buttons: [${buttons.take(4).joinToString(", ")}]. ")
            if (inputFields.isNotEmpty()) append("Fields: [${inputFields.take(3).joinToString(", ")}]. ")
            if (texts.isNotEmpty()) append("Text snippet: '${texts.firstOrNull()?.take(60)}'")
        }

        val snapshot = EphemeralScreenSnapshot(
            visibleAppPackage = visibleAppPackage,
            windowTitle = windowTitle,
            recognizedButtons = buttons,
            recognizedTexts = texts,
            recognizedInputFields = inputFields,
            rawSummary = summary,
            isExplicitlySaved = false
        )

        _currentScreen.value = snapshot

        // Ephemeral observation registered in World Model
        worldModelEngine.registerEntity(
            WorldEntity(
                id = "screen_active_window",
                type = WorldEntityType.CURRENT_CONTEXT,
                name = windowTitle.ifEmpty { visibleAppPackage },
                epistemicStatus = EpistemicStatus.OBSERVATION,
                properties = mapOf(
                    "package" to visibleAppPackage,
                    "buttons" to buttons.size,
                    "fields" to inputFields.size
                )
            )
        )

        return snapshot
    }

    /**
     * Discards the temporary screen snapshot to respect user privacy.
     */
    fun discardCurrentScreen() {
        _currentScreen.value = null
        worldModelEngine.removeEntity("screen_active_window")
    }

    private fun extractNodes(
        node: UiElementNode?,
        buttons: MutableList<String>,
        texts: MutableList<String>,
        inputFields: MutableList<String>
    ) {
        if (node == null) return

        if (node.isClickable && (node.className.contains("Button") || !node.text.isNullOrBlank())) {
            buttons.add(node.text ?: node.contentDescription ?: node.resourceId ?: "Button")
        } else if (node.isEditable || node.className.contains("EditText")) {
            inputFields.add(node.text ?: node.resourceId ?: "Input Field")
        } else if (!node.text.isNullOrBlank()) {
            texts.add(node.text)
        }

        for (child in node.children) {
            extractNodes(child, buttons, texts, inputFields)
        }
    }
}

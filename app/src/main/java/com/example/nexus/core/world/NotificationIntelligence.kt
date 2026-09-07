package com.example.nexus.core.world

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class NotificationPriorityClassification {
    IMPORTANT,          // Urgent messages, security alerts, calendar reminders
    ACTION_REQUIRED,    // Confirmation requests, 2FA codes, pending mission checkpoints
    INFORMATIONAL,      // Status updates, weather, news updates
    PROMOTIONAL,        // Marketing, sales, discounts
    NOISE               // Low value repetitive updates
}

data class ClassifiedNotification(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val title: String,
    val content: String,
    val classification: NotificationPriorityClassification,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedActionTitle: String? = null,
    val suggestedActionCapability: String? = null,
    val suggestedActionParams: Map<String, Any> = emptyMap()
)

data class ActionableNexusPrompt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val positiveActionText: String,
    val positiveActionCapability: String,
    val positiveActionParams: Map<String, Any> = emptyMap(),
    val negativeActionText: String = "Dismiss",
    val negativeActionCapability: String? = null
)

/**
 * Local Notification Intelligence.
 * Invariant: Never uploads notification content to remote servers. All processing is 100% on-device.
 */
class NotificationIntelligence(
    private val worldModelEngine: WorldModelEngine
) {
    private val notificationsList = mutableListOf<ClassifiedNotification>()
    private val _notifications = MutableStateFlow<List<ClassifiedNotification>>(emptyList())
    val notifications: StateFlow<List<ClassifiedNotification>> = _notifications.asStateFlow()

    private val _activeActionablePrompt = MutableStateFlow<ActionableNexusPrompt?>(null)
    val activeActionablePrompt: StateFlow<ActionableNexusPrompt?> = _activeActionablePrompt.asStateFlow()

    fun ingestNotification(
        packageName: String,
        title: String,
        text: String
    ): ClassifiedNotification {
        val classification = classifyLocally(title, text, packageName)

        val (suggestedTitle, suggestedCap, suggestedParams) = deriveSuggestedAction(classification, title, text, packageName)

        val item = ClassifiedNotification(
            packageName = packageName,
            title = title,
            content = text,
            classification = classification,
            suggestedActionTitle = suggestedTitle,
            suggestedActionCapability = suggestedCap,
            suggestedActionParams = suggestedParams
        )

        synchronized(notificationsList) {
            notificationsList.add(0, item)
            if (notificationsList.size > 50) notificationsList.removeLast()
            _notifications.value = notificationsList.toList()
        }

        // Register in World Model
        worldModelEngine.registerEntity(
            WorldEntity(
                id = "notif_${item.id}",
                type = WorldEntityType.NOTIFICATION,
                name = item.title,
                epistemicStatus = EpistemicStatus.OBSERVATION,
                properties = mapOf(
                    "package" to packageName,
                    "classification" to classification.name
                )
            )
        )

        return item
    }

    fun setActionablePrompt(prompt: ActionableNexusPrompt?) {
        _activeActionablePrompt.value = prompt
    }

    fun generateSummary(): String {
        val list = _notifications.value
        if (list.isEmpty()) return "No pending notifications."

        val actionRequired = list.count { it.classification == NotificationPriorityClassification.ACTION_REQUIRED }
        val important = list.count { it.classification == NotificationPriorityClassification.IMPORTANT }

        return when {
            actionRequired > 0 -> "You have $actionRequired notification(s) requiring attention."
            important > 0 -> "You have $important high-priority message(s)."
            else -> "${list.size} notification(s) received. All informational."
        }
    }

    private fun classifyLocally(
        title: String,
        text: String,
        packageName: String
    ): NotificationPriorityClassification {
        val combined = "$title $text".lowercase()

        // 1. Action Required
        if (combined.contains("verification code") || combined.contains("confirm your") ||
            combined.contains("tap to verify") || combined.contains("action required") ||
            combined.contains("permission needed") || combined.contains("approve sign-in")
        ) {
            return NotificationPriorityClassification.ACTION_REQUIRED
        }

        // 2. Important
        if (combined.contains("warning") || combined.contains("security alert") ||
            combined.contains("battery low") || combined.contains("calendar") ||
            combined.contains("reminder") || combined.contains("meeting in")
        ) {
            return NotificationPriorityClassification.IMPORTANT
        }

        // 3. Promotional
        if (combined.contains("sale") || combined.contains("% off") ||
            combined.contains("deal of the day") || combined.contains("discount") ||
            combined.contains("coupon") || combined.contains("promo")
        ) {
            return NotificationPriorityClassification.PROMOTIONAL
        }

        // 4. Noise
        if (combined.contains("running in the background") || combined.contains("syncing") ||
            combined.contains("download complete") || combined.contains("updated successfully")
        ) {
            return NotificationPriorityClassification.NOISE
        }

        return NotificationPriorityClassification.INFORMATIONAL
    }

    private fun deriveSuggestedAction(
        classification: NotificationPriorityClassification,
        title: String,
        text: String,
        packageName: String
    ): Triple<String?, String?, Map<String, Any>> {
        return when (classification) {
            NotificationPriorityClassification.ACTION_REQUIRED -> {
                Triple("Open App", "open_app", mapOf("package_name" to packageName))
            }
            NotificationPriorityClassification.IMPORTANT -> {
                Triple("Review", "open_app", mapOf("package_name" to packageName))
            }
            else -> Triple(null, null, emptyMap())
        }
    }
}

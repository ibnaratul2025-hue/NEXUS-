package com.example.nexus.core.presence

import com.example.nexus.core.policy.RiskLevel
import java.util.UUID

enum class InteractionStatus {
    PENDING,
    ACCEPTED,
    DISMISSED,
    EXPIRED,
    EXECUTED
}

enum class InteractionDecision {
    IGNORE,
    SILENT_UPDATE,
    SUGGEST,
    ASK,
    ALERT,
    EXECUTE_SAFE,
    REQUEST_CONFIRMATION
}

data class InteractionAction(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val isPrimary: Boolean = false,
    val isDestructive: Boolean = false,
    val payload: String? = null
)

data class NexusInteraction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val reason: String,
    val context: String,
    val suggestedAction: String? = null,
    val confidence: Float = 1.0f,
    val priority: InteractionPriority = InteractionPriority.MEDIUM,
    val expirationTimestamp: Long = System.currentTimeMillis() + (30 * 60 * 1000L),
    val requiredPermission: String? = null,
    val risk: RiskLevel = RiskLevel.LOW,
    val actions: List<InteractionAction> = listOf(
        InteractionAction(label = "Review", isPrimary = true),
        InteractionAction(label = "Later", isPrimary = false)
    ),
    val status: InteractionStatus = InteractionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean =
        currentTime > expirationTimestamp
}

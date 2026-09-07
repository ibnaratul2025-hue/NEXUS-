package com.example.nexus.core.world

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

enum class NexusEventType {
    DEVICE_EVENT,
    APP_EVENT,
    MISSION_EVENT,
    MEMORY_EVENT,
    TOOL_EVENT,
    PERMISSION_EVENT,
    USER_EVENT,
    SYSTEM_EVENT
}

data class NexusEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: NexusEventType,
    val source: String,
    val topic: String,
    val payload: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val ttlMs: Long = 60_000L, // Default 1 minute TTL
    val deduplicationKey: String = "$type:$topic:${payload.hashCode()}"
) {
    val isExpired: Boolean get() = (System.currentTimeMillis() - timestamp) > ttlMs
}

/**
 * High-performance decoupled local event bus for NEXUS.
 * Enforces event expiration and transparent deduplication.
 */
class LocalEventBus {
    private val _events = MutableSharedFlow<NexusEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NexusEvent> = _events.asSharedFlow()

    // Deduplication cache: key -> timestamp of last event
    private val seenEvents = ConcurrentHashMap<String, Long>()
    private val deduplicationWindowMs = 2_000L

    fun publish(event: NexusEvent): Boolean {
        val now = System.currentTimeMillis()
        if (event.isExpired) return false

        // Check deduplication
        val lastSeen = seenEvents[event.deduplicationKey]
        if (lastSeen != null && (now - lastSeen) < deduplicationWindowMs) {
            // Deduplicated
            return false
        }

        seenEvents[event.deduplicationKey] = now
        cleanupOldDeduplicationKeys(now)

        return _events.tryEmit(event)
    }

    private fun cleanupOldDeduplicationKeys(now: Long) {
        if (seenEvents.size > 200) {
            seenEvents.entries.removeIf { (now - it.value) > 30_000L }
        }
    }
}

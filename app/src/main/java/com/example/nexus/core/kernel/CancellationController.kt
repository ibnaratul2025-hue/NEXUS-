package com.example.nexus.core.kernel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class CancellationController {

    private val _isCancelled = MutableStateFlow(false)
    val isCancelled: StateFlow<Boolean> = _isCancelled.asStateFlow()

    private val _cancelReason = MutableStateFlow<String?>(null)
    val cancelReason: StateFlow<String?> = _cancelReason.asStateFlow()

    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun cancel(reason: String = "User requested cancellation") {
        _cancelReason.value = reason
        _isCancelled.value = true
        for (listener in listeners) {
            try {
                listener(reason)
            } catch (_: Throwable) {}
        }
    }

    fun reset() {
        _isCancelled.value = false
        _cancelReason.value = null
    }

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }
}

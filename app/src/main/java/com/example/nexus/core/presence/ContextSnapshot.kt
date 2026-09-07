package com.example.nexus.core.presence

import java.util.Calendar

/**
 * Ephemeral snapshot of real-time local Android context.
 * Privacy-first: strictly transient, never permanently logged in raw format.
 */
data class ContextSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    val isWeekend: Boolean = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY),
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val chargingSpeed: String = "NORMAL", // SLOW, NORMAL, FAST, UNKNOWN
    val deviceTemperature: Float? = null, // In Celsius when available from hardware, null if unpermitted/unavailable
    val availableRamMb: Long = 2048,
    val totalRamMb: Long = 4096,
    val availableStorageMb: Long = 10240,
    val totalStorageMb: Long = 65536,
    val currentApp: String? = null, // Foreground package if usage stats permitted
    val screenState: String = "ON", // ON, OFF, LOCKED, UNKNOWN
    val networkState: String = "WIFI", // WIFI, CELLULAR, OFFLINE, UNKNOWN
    val activeNexusTask: String? = null,
    val currentProject: String? = null,
    val recentActivity: List<String> = emptyList(),
    val userDefinedRoutines: List<String> = emptyList(),
    val permittedSignals: Set<String> = emptySet(),
    val restrictedSignals: Set<String> = emptySet()
) {
    val ramUsageRatio: Float
        get() = if (totalRamMb > 0) 1.0f - (availableRamMb.toFloat() / totalRamMb.toFloat()) else 0f

    val storageUsageRatio: Float
        get() = if (totalStorageMb > 0) 1.0f - (availableStorageMb.toFloat() / totalStorageMb.toFloat()) else 0f

    val isLowBattery: Boolean
        get() = batteryLevel <= 20 && !isCharging

    val isLowStorage: Boolean
        get() = storageUsageRatio >= 0.90f || availableStorageMb < 2048

    val isLowRam: Boolean
        get() = availableRamMb < 400
}

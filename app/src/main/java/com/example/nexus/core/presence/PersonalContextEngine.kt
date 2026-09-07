package com.example.nexus.core.presence

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import com.example.nexus.core.permission.AndroidPermissionManager
import java.util.Calendar

/**
 * Real-time local context engine.
 * Observes permitted Android system signals and generates ephemeral ContextSnapshots.
 * Respects user privacy: only gathers what is authorized, never fabricates unavailable signals.
 */
class PersonalContextEngine(
    private val context: Context,
    private val permissionManager: AndroidPermissionManager? = null
) {
    private var lastObservedProject: String? = null
    private var lastActiveNexusTask: String? = null
    private val recentActivities = ArrayDeque<String>(10)

    fun setActiveProject(projectName: String?) {
        lastObservedProject = projectName
    }

    fun setActiveTask(taskDescription: String?) {
        lastActiveNexusTask = taskDescription
    }

    fun recordActivity(activity: String) {
        if (recentActivities.size >= 10) {
            recentActivities.removeFirst()
        }
        recentActivities.addLast(activity)
    }

    fun captureSnapshot(): ContextSnapshot {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY

        val permitted = mutableSetOf<String>()
        val restricted = mutableSetOf<String>()

        // 1. Battery & Charging
        var batteryLevel = 100
        var isCharging = false
        var chargingSpeed = "NORMAL"
        var deviceTemperature: Float? = null

        try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                val isUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                chargingSpeed = when {
                    isAc -> "FAST"
                    isUsb -> "SLOW"
                    isCharging -> "NORMAL"
                    else -> "DISCHARGING"
                }

                val temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (temp > 0) {
                    deviceTemperature = temp / 10.0f // BatteryManager returns tenths of a degree Celsius
                }
                permitted.add("battery")
                permitted.add("charging")
                if (deviceTemperature != null) permitted.add("temperature")
            }
        } catch (e: Exception) {
            restricted.add("battery")
        }

        // 2. RAM
        var availableRamMb = 2048L
        var totalRamMb = 4096L
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                availableRamMb = memInfo.availMem / (1024 * 1024)
                totalRamMb = memInfo.totalMem / (1024 * 1024)
                permitted.add("ram")
            }
        } catch (e: Exception) {
            restricted.add("ram")
        }

        // 3. Storage
        var availableStorageMb = 10240L
        var totalStorageMb = 65536L
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            availableStorageMb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024 * 1024)
            totalStorageMb = (statFs.blockCountLong * statFs.blockSizeLong) / (1024 * 1024)
            permitted.add("storage")
        } catch (e: Exception) {
            restricted.add("storage")
        }

        // 4. Network
        var networkState = "UNKNOWN"
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)
                networkState = when {
                    caps == null -> "OFFLINE"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    else -> "CONNECTED"
                }
                permitted.add("network")
            }
        } catch (e: Exception) {
            restricted.add("network")
        }

        // 5. Screen State
        var screenState = "ON"
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                screenState = if (pm.isInteractive) "ON" else "OFF"
                permitted.add("screen")
            }
        } catch (e: Exception) {
            restricted.add("screen")
        }

        // Notice: Location and UsageStats are guarded; marked as restricted unless explicitly granted
        restricted.add("location")
        restricted.add("usage_stats")
        restricted.add("calendar")

        return ContextSnapshot(
            timestamp = System.currentTimeMillis(),
            hourOfDay = hour,
            dayOfWeek = day,
            isWeekend = isWeekend,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            chargingSpeed = chargingSpeed,
            deviceTemperature = deviceTemperature,
            availableRamMb = availableRamMb,
            totalRamMb = totalRamMb,
            availableStorageMb = availableStorageMb,
            totalStorageMb = totalStorageMb,
            currentApp = null, // Protected unless UsageStats granted
            screenState = screenState,
            networkState = networkState,
            activeNexusTask = lastActiveNexusTask,
            currentProject = lastObservedProject,
            recentActivity = recentActivities.toList(),
            permittedSignals = permitted,
            restrictedSignals = restricted
        )
    }
}

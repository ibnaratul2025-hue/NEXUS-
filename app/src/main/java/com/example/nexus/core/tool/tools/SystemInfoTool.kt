package com.example.nexus.core.tool.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONObject

class SystemInfoTool(
    private val context: Context,
    override val id: String = "system.info"
) : AgentTool {
    override val name: String = "System Diagnostics"
    override val description: String = "Reads Android OS version, manufacturer, model, CPU architecture, RAM, storage, battery status, and app version."
    override val argumentSchema: ToolSchema = ToolSchema(emptyList())
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val info = JSONObject()

        // Android OS and Hardware
        info.put("androidVersion", Build.VERSION.RELEASE ?: "N/A")
        info.put("sdkInt", Build.VERSION.SDK_INT)
        info.put("deviceManufacturer", Build.MANUFACTURER ?: "N/A")
        info.put("deviceModel", Build.MODEL ?: "N/A")
        info.put("device", Build.DEVICE ?: "N/A")
        info.put("cpuArchitecture", Build.SUPPORTED_ABIS.joinToString().ifBlank { System.getProperty("os.arch") ?: "N/A" })

        // RAM stats
        try {
            val actManager = this.context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            if (actManager != null) {
                val availMb = memInfo.availMem / (1024 * 1024)
                val totalMb = memInfo.totalMem / (1024 * 1024)
                info.put("availableRamMb", availMb)
                info.put("totalRamMb", totalMb)
                info.put("isLowMemory", memInfo.lowMemory)
            } else {
                info.put("availableRamMb", "N/A")
                info.put("totalRamMb", "N/A")
            }
        } catch (e: Exception) {
            info.put("availableRamMb", "N/A")
            info.put("totalRamMb", "N/A")
        }

        // Storage stats
        try {
            val stat = StatFs(this.context.filesDir.path)
            val availStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
            val totalStorageMb = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024)
            info.put("availableStorageMb", availStorageMb)
            info.put("totalStorageMb", totalStorageMb)
        } catch (e: Exception) {
            info.put("availableStorageMb", "N/A")
            info.put("totalStorageMb", "N/A")
        }

        // Battery stats
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = this.context.registerReceiver(null, ifilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val batteryPct = if (level != -1 && scale > 0) (level * 100 / scale) else -1

                info.put("batteryPercentage", if (batteryPct >= 0) "$batteryPct%" else "N/A")
                info.put("isCharging", isCharging)
                info.put("chargingState", when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                })
            } else {
                info.put("batteryPercentage", "N/A")
                info.put("chargingState", "N/A")
            }
        } catch (e: Exception) {
            info.put("batteryPercentage", "N/A")
            info.put("chargingState", "N/A")
        }

        // App version
        try {
            val pInfo = this.context.packageManager.getPackageInfo(this.context.packageName, 0)
            info.put("appVersion", pInfo.versionName ?: "1.0")
        } catch (e: Exception) {
            info.put("appVersion", "N/A")
        }

        return ToolResult(
            success = true,
            output = info.toString(2),
            executionTimeMs = System.currentTimeMillis() - start
        )
    }
}

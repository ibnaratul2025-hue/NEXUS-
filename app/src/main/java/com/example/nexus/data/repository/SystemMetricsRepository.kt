package com.example.nexus.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class SystemMetrics(
    val totalRamMb: Long,
    val availRamMb: Long,
    val usedRamMb: Long,
    val ramUsagePercent: Float,
    val isLowMemory: Boolean,
    val availableStorageMb: Long,
    val sandboxReady: Boolean,
    val cpuArch: String
)

class SystemMetricsRepository(private val context: Context) {

    fun getSystemMetrics(): SystemMetrics {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = maxOf(0L, totalRamMb - availRamMb)
        val ramUsage = if (totalRamMb > 0) (usedRamMb.toFloat() / totalRamMb.toFloat()) else 0f

        val stat = StatFs(Environment.getDataDirectory().path)
        val availableStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        // Sandbox readiness: check app workspace directory write permission
        val workspaceDir = File(context.filesDir, "workspaces")
        val sandboxReady = workspaceDir.exists() || workspaceDir.mkdirs()

        return SystemMetrics(
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            usedRamMb = usedRamMb,
            ramUsagePercent = ramUsage,
            isLowMemory = memInfo.lowMemory,
            availableStorageMb = availableStorageMb,
            sandboxReady = sandboxReady,
            cpuArch = System.getProperty("os.arch") ?: "unknown"
        )
    }
}

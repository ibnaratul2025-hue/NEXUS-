package com.example.nexus.core.tool.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONArray
import org.json.JSONObject

class AppListTool(
    private val context: Context,
    override val id: String = "app.list"
) : AgentTool {
    override val name: String = "List Installed Applications"
    override val description: String = "Lists all installed launchable applications on the device, including display names and package identifiers."
    override val argumentSchema: ToolSchema = ToolSchema(emptyList())
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val pm = this.context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveList = pm.queryIntentActivities(launcherIntent, 0)
        val appArray = JSONArray()

        val sortedList = resolveList.sortedBy { it.loadLabel(pm).toString() }
        for (info in sortedList) {
            val label = info.loadLabel(pm).toString()
            val pkg = info.activityInfo.packageName
            val appObj = JSONObject().apply {
                put("name", label)
                put("packageName", pkg)
            }
            appArray.put(appObj)
        }

        val outputObj = JSONObject().apply {
            put("count", sortedList.size)
            put("apps", appArray)
        }

        return ToolResult(
            success = true,
            output = outputObj.toString(2),
            executionTimeMs = System.currentTimeMillis() - start
        )
    }
}

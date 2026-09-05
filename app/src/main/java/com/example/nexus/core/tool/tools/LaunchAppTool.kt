package com.example.nexus.core.tool.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONObject

class LaunchAppTool(
    private val context: Context,
    override val id: String = "app.launch"
) : AgentTool {
    override val name: String = "Launch Application"
    override val description: String = "Searches for an installed application by user-facing label or package name and launches it."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "appNameOrPackage",
                type = "string",
                description = "The display name (e.g. 'YouTube', 'Chrome', 'Settings') or Android package name",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val query = (arguments.optString("appNameOrPackage").ifBlank { arguments.optString("packageName") }).trim()
        if (query.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "APP_NOT_SPECIFIED: Missing 'appNameOrPackage' argument",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // Special check for Settings
        if (query.equals("settings", ignoreCase = true) || query.equals("system settings", ignoreCase = true)) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                this.context.startActivity(intent)
                ToolResult(
                    success = true,
                    output = "Opened Android System Settings",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            } catch (e: Exception) {
                ToolResult(
                    success = false,
                    output = "",
                    error = "FAILED_TO_OPEN_SETTINGS: ${e.localizedMessage}",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        val pm = this.context.packageManager

        // 1. Direct package lookup
        val directIntent = pm.getLaunchIntentForPackage(query)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                this.context.startActivity(directIntent)
                ToolResult(
                    success = true,
                    output = "Dispatched launch intent for package: $query to Android system.",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            } catch (e: Throwable) {
                ToolResult(
                    success = false,
                    output = "",
                    error = "LAUNCH_FAILED: ${e.localizedMessage}",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 2. Dynamic lookup in installed launchable apps
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList: List<ResolveInfo> = pm.queryIntentActivities(launcherIntent, 0)
        
        // Exact label match first
        val exactMatches = resolveList.filter {
            val label = it.loadLabel(pm).toString()
            label.equals(query, ignoreCase = true)
        }

        val matches = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else {
            resolveList.filter {
                val label = it.loadLabel(pm).toString()
                val pkg = it.activityInfo.packageName
                label.contains(query, ignoreCase = true) || pkg.contains(query, ignoreCase = true)
            }
        }

        if (matches.isEmpty()) {
            return ToolResult(
                success = false,
                output = "",
                error = "APP_NOT_FOUND: No installed application matched '$query'. Use 'app.list' to inspect available apps.",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        if (matches.size > 1) {
            val sb = StringBuilder("I found multiple matching apps:\n")
            matches.take(5).forEachIndexed { index, resolveInfo ->
                val label = resolveInfo.loadLabel(pm)
                val pkg = resolveInfo.activityInfo.packageName
                sb.appendLine("${index + 1}. $label ($pkg)")
            }
            sb.append("Which one would you like me to open?")
            return ToolResult(
                success = true,
                output = sb.toString(),
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // Single match found
        val matchedApp = matches.first()
        val pkg = matchedApp.activityInfo.packageName
        val label = matchedApp.loadLabel(pm)
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
            ?: return ToolResult(
                success = false,
                output = "",
                error = "APP_NOT_LAUNCHABLE: Could not create launch intent for $label ($pkg)",
                executionTimeMs = System.currentTimeMillis() - start
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            this.context.startActivity(launchIntent)
            ToolResult(
                success = true,
                output = "Dispatched launch intent for $label ($pkg) to Android system.",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Throwable) {
            ToolResult(
                success = false,
                output = "",
                error = "LAUNCH_FAILED: ${e.localizedMessage}",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

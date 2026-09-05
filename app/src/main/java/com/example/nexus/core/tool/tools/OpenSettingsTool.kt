package com.example.nexus.core.tool.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONObject

class OpenSettingsTool(
    private val context: Context,
    override val id: String = "settings.open"
) : AgentTool {
    override val name: String = "Open Settings"
    override val description: String = "Opens Android system settings or a specific settings panel (e.g. 'wifi', 'bluetooth', 'display', 'apps', 'battery')."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "section",
                type = "string",
                description = "Optional section: 'wifi', 'bluetooth', 'display', 'apps', 'battery', or 'general'",
                required = false
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val section = arguments.optString("section", "general").lowercase()

        val action = when (section) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            this.context.startActivity(intent)
            ToolResult(
                success = true,
                output = "Opened Android settings (${if (section.isNotBlank()) section else "general"}).",
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
}

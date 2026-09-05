package com.example.nexus.core.tool.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONObject

class OpenBrowserTool(
    private val context: Context,
    override val id: String = "browser.open"
) : AgentTool {
    override val name: String = "Open Web Browser"
    override val description: String = "Opens a web URL safely in the default system web browser."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "url",
                type = "string",
                description = "The HTTP/HTTPS web address to open",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        var url = arguments.optString("url", "").trim()
        if (url.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "URL_NOT_SPECIFIED: Missing 'url' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            this.context.startActivity(intent)
            ToolResult(
                success = true,
                output = "Dispatched URL to default system browser: $url. Note: Webpage contents are not retrieved or read.",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = "BROWSER_FAILED: ${e.localizedMessage}",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

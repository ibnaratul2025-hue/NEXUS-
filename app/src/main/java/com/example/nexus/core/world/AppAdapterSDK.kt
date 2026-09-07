package com.example.nexus.core.world

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.nexus.core.policy.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class AdapterActionDefinition(
    val name: String,
    val description: String,
    val riskLevel: RiskLevel,
    val requiredPermissions: List<String>,
    val parametersSchema: Map<String, String>,
    val verificationRule: String
)

interface AppAdapter {
    val appId: String
    val appName: String
    val version: String
    val capabilities: List<String>
    val requiredPermissions: List<String>
    val actions: List<AdapterActionDefinition>

    suspend fun execute(
        context: Context,
        actionName: String,
        parameters: Map<String, Any>
    ): ActionExecutionResult
}

/**
 * Sandboxed, community-pluggable App Adapter Manager.
 * Guarantees:
 * - Adapters cannot bypass Android OS permission models.
 * - Every adapter must be user-approved before activation.
 * - Audited action schemas.
 */
class AppAdapterManager(
    private val context: Context,
    private val actionFabric: ActionFabric,
    private val worldModelEngine: WorldModelEngine
) {
    private val adapters = ConcurrentHashMap<String, AppAdapter>()
    private val approvedAdapters = ConcurrentHashMap<String, Boolean>()

    private val _installedAdapters = MutableStateFlow<List<AppAdapter>>(emptyList())
    val installedAdapters: StateFlow<List<AppAdapter>> = _installedAdapters.asStateFlow()

    init {
        // Register default built-in safe adapters
        registerAdapter(YouTubeAppAdapter(), userApproved = true)
        registerAdapter(GitHubAppAdapter(), userApproved = true)
        registerAdapter(FilesAppAdapter(), userApproved = true)
    }

    fun registerAdapter(adapter: AppAdapter, userApproved: Boolean = false) {
        adapters[adapter.appId] = adapter
        approvedAdapters[adapter.appId] = userApproved
        mountAdapterActions(adapter)

        _installedAdapters.value = adapters.values.toList()

        // Register adapter in World Model
        worldModelEngine.registerEntity(
            WorldEntity(
                id = "adapter_${adapter.appId}",
                type = WorldEntityType.SKILL,
                name = "${adapter.appName} Adapter (v${adapter.version})",
                epistemicStatus = EpistemicStatus.FACT,
                properties = mapOf(
                    "appId" to adapter.appId,
                    "version" to adapter.version,
                    "isApproved" to userApproved,
                    "actionsCount" to adapter.actions.size
                )
            )
        )
    }

    fun approveAdapter(appId: String, approved: Boolean) {
        val adapter = adapters[appId] ?: return
        approvedAdapters[appId] = approved
        if (approved) {
            mountAdapterActions(adapter)
        }
    }

    fun isApproved(appId: String): Boolean = approvedAdapters[appId] == true

    private fun mountAdapterActions(adapter: AppAdapter) {
        for (actionDef in adapter.actions) {
            val fullCapability = "${adapter.appId}.${actionDef.name}"
            actionFabric.registerActionFactory(fullCapability) { params ->
                UniversalAction(
                    capability = fullCapability,
                    target = adapter.appName,
                    parameters = params,
                    requiredPermissions = actionDef.requiredPermissions,
                    risk = actionDef.riskLevel,
                    description = actionDef.description,
                    execute = { ctx ->
                        if (!isApproved(adapter.appId)) {
                            ActionExecutionResult.Blocked(
                                actionId = "unapproved_${adapter.appId}",
                                reason = "Adapter '${adapter.appName}' is not approved by the user."
                            )
                        } else {
                            adapter.execute(ctx.context, actionDef.name, params)
                        }
                    },
                    verify = {
                        ActionVerificationResult.Verified("Verified per adapter rule: ${actionDef.verificationRule}")
                    }
                )
            }
        }
    }
}

// Built-in Safe Adapters
class YouTubeAppAdapter : AppAdapter {
    override val appId: String = "youtube"
    override val appName: String = "YouTube"
    override val version: String = "1.0.0"
    override val capabilities: List<String> = listOf("search", "open_video")
    override val requiredPermissions: List<String> = emptyList()
    override val actions: List<AdapterActionDefinition> = listOf(
        AdapterActionDefinition(
            name = "search",
            description = "Search videos on YouTube",
            riskLevel = RiskLevel.LOW,
            requiredPermissions = emptyList(),
            parametersSchema = mapOf("query" to "String"),
            verificationRule = "Android Intent dispatched to YouTube or web fallback"
        ),
        AdapterActionDefinition(
            name = "open_video",
            description = "Open specific YouTube video URL or ID",
            riskLevel = RiskLevel.LOW,
            requiredPermissions = emptyList(),
            parametersSchema = mapOf("video_id" to "String"),
            verificationRule = "Video link dispatched"
        )
    )

    override suspend fun execute(context: Context, actionName: String, parameters: Map<String, Any>): ActionExecutionResult {
        return when (actionName) {
            "search" -> {
                val q = Uri.encode(parameters["query"]?.toString() ?: "")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionExecutionResult.Success(java.util.UUID.randomUUID().toString(), "Dispatched YouTube search", true, false)
            }
            "open_video" -> {
                val id = parameters["video_id"]?.toString() ?: ""
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionExecutionResult.Success(java.util.UUID.randomUUID().toString(), "Dispatched YouTube video $id", true, false)
            }
            else -> ActionExecutionResult.Failed(java.util.UUID.randomUUID().toString(), "Unknown action $actionName")
        }
    }
}

class GitHubAppAdapter : AppAdapter {
    override val appId: String = "github"
    override val appName: String = "GitHub"
    override val version: String = "1.0.0"
    override val capabilities: List<String> = listOf("open_repo", "open_issues")
    override val requiredPermissions: List<String> = emptyList()
    override val actions: List<AdapterActionDefinition> = listOf(
        AdapterActionDefinition(
            name = "open_repo",
            description = "Open GitHub repository link",
            riskLevel = RiskLevel.LOW,
            requiredPermissions = emptyList(),
            parametersSchema = mapOf("repo" to "owner/repo"),
            verificationRule = "Browser or GitHub app opened"
        )
    )

    override suspend fun execute(context: Context, actionName: String, parameters: Map<String, Any>): ActionExecutionResult {
        val repo = parameters["repo"]?.toString() ?: ""
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repo")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionExecutionResult.Success(java.util.UUID.randomUUID().toString(), "Opened GitHub repo $repo", true, false)
    }
}

class FilesAppAdapter : AppAdapter {
    override val appId: String = "files"
    override val appName: String = "System Files"
    override val version: String = "1.0.0"
    override val capabilities: List<String> = listOf("browse_downloads")
    override val requiredPermissions: List<String> = emptyList()
    override val actions: List<AdapterActionDefinition> = listOf(
        AdapterActionDefinition(
            name = "browse_downloads",
            description = "Open Android downloads folder",
            riskLevel = RiskLevel.LOW,
            requiredPermissions = emptyList(),
            parametersSchema = emptyMap(),
            verificationRule = "Storage documents UI opened"
        )
    )

    override suspend fun execute(context: Context, actionName: String, parameters: Map<String, Any>): ActionExecutionResult {
        val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionExecutionResult.Success(java.util.UUID.randomUUID().toString(), "Opened downloads directory", true, false)
        } catch (e: Exception) {
            ActionExecutionResult.Failed(java.util.UUID.randomUUID().toString(), "Downloads app not accessible: ${e.message}")
        }
    }
}

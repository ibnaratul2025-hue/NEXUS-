package com.example.nexus.core.world

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class AppCapabilityProfile(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val hasLaunchIntent: Boolean,
    val activitiesCount: Int,
    val supportedIntents: List<String>,
    val canReceiveShare: Boolean,
    val declaredPermissions: List<String>,
    val deepLinkSchemes: List<String>,
    val capabilities: List<String>
)

/**
 * Maps installed applications to verified, supported Android public integration surfaces.
 * Invariant: Never claims unsupported private APIs or internal controls.
 */
class AppCapabilityRegistry(
    private val context: Context,
    private val worldModelEngine: WorldModelEngine
) {
    private val profiles = ConcurrentHashMap<String, AppCapabilityProfile>()
    private val _appProfiles = MutableStateFlow<List<AppCapabilityProfile>>(emptyList())
    val appProfiles: StateFlow<List<AppCapabilityProfile>> = _appProfiles.asStateFlow()

    suspend fun scanInstalledApps(): List<AppCapabilityProfile> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }

        val results = mutableListOf<AppCapabilityProfile>()

        // Check share intent receivers
        val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        val shareReceivers = pm.queryIntentActivities(shareIntent, 0).map { it.activityInfo.packageName }.toSet()

        for (pkg in installed) {
            val appInfo = pkg.applicationInfo ?: continue
            val appName = try { pm.getApplicationLabel(appInfo).toString() } catch (e: Exception) { pkg.packageName }
            val hasLaunch = pm.getLaunchIntentForPackage(pkg.packageName) != null
            val activitiesCount = pkg.activities?.size ?: 0
            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            val canShare = shareReceivers.contains(pkg.packageName)

            val supportedIntents = mutableListOf<String>()
            if (hasLaunch) supportedIntents.add("android.intent.action.MAIN")
            if (canShare) supportedIntents.add("android.intent.action.SEND")

            val capabilities = mutableListOf<String>()
            if (hasLaunch) capabilities.add("Launch")
            if (canShare) capabilities.add("Receive Share")
            if (activitiesCount > 0) capabilities.add("Explicit Activities ($activitiesCount)")

            val profile = AppCapabilityProfile(
                packageName = pkg.packageName,
                appName = appName,
                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                hasLaunchIntent = hasLaunch,
                activitiesCount = activitiesCount,
                supportedIntents = supportedIntents,
                canReceiveShare = canShare,
                declaredPermissions = permissions,
                deepLinkSchemes = emptyList(),
                capabilities = capabilities
            )

            profiles[pkg.packageName] = profile
            results.add(profile)

            // Sync with World Model
            worldModelEngine.registerEntity(
                WorldEntity(
                    id = "app_${pkg.packageName}",
                    type = WorldEntityType.APP,
                    name = appName,
                    epistemicStatus = EpistemicStatus.FACT,
                    properties = mapOf(
                        "packageName" to pkg.packageName,
                        "hasLaunchIntent" to hasLaunch,
                        "canReceiveShare" to canShare
                    )
                )
            )

            worldModelEngine.addRelation(
                WorldRelation(
                    fromEntityId = "entity_device_local",
                    toEntityId = "app_${pkg.packageName}",
                    relationType = WorldRelationType.AVAILABLE,
                    epistemicStatus = EpistemicStatus.FACT
                )
            )
        }

        val sorted = results.sortedByDescending { it.hasLaunchIntent }
        _appProfiles.value = sorted
        sorted
    }

    fun getProfile(packageName: String): AppCapabilityProfile? = profiles[packageName]

    fun findAppByName(name: String): AppCapabilityProfile? {
        val lower = name.lowercase().trim()
        return profiles.values.firstOrNull {
            it.appName.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
        }
    }
}

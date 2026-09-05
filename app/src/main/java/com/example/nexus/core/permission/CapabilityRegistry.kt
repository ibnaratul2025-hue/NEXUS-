package com.example.nexus.core.permission

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.nexus.core.policy.RiskLevel

enum class CapabilityAvailability {
    AVAILABLE,
    PERMISSION_REQUIRED,
    SERVICE_DISABLED,
    UNAVAILABLE
}

data class NexusCapability(
    val id: String,
    val name: String,
    val description: String,
    val requiredPermissions: List<String> = emptyList(),
    val serviceRequired: Boolean = false,
    val availability: CapabilityAvailability,
    val riskLevel: RiskLevel,
    val whyNeeded: String,
    val dataAccessed: String,
    val configurationRoute: String? = null
)

class CapabilityRegistry(
    private val context: Context,
    private val permissionManager: AndroidPermissionManager
) {

    /**
     * Real accessibility service detector.
     * Never assumes accessibility is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val packageName = context.packageName
            enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Returns full list of system capabilities with real live availability statuses.
     */
    fun getCapabilities(): List<NexusCapability> {
        val accessibilityEnabled = isAccessibilityServiceEnabled()

        val cameraState = permissionManager.check("android.permission.CAMERA")
        val audioState = permissionManager.check("android.permission.RECORD_AUDIO")
        val notifState = permissionManager.check("android.permission.POST_NOTIFICATIONS")

        return listOf(
            NexusCapability(
                id = "system.metrics",
                name = "System Telemetry & Metrics",
                description = "Battery level, RAM usage, storage availability, and device state.",
                requiredPermissions = emptyList(),
                serviceRequired = false,
                availability = CapabilityAvailability.AVAILABLE,
                riskLevel = RiskLevel.LOW,
                whyNeeded = "Allows NEXUS to answer questions regarding device resources, battery status, and memory.",
                dataAccessed = "Battery percentage, available RAM, and internal storage space.",
                configurationRoute = null
            ),
            NexusCapability(
                id = "app.management",
                name = "App Launcher & Discovery",
                description = "Query installed launchable apps and start applications on user command.",
                requiredPermissions = emptyList(),
                serviceRequired = false,
                availability = CapabilityAvailability.AVAILABLE,
                riskLevel = RiskLevel.LOW,
                whyNeeded = "Enables voice or text commands like 'Open Settings' or 'Launch YouTube'.",
                dataAccessed = "List of installed launcher apps and package identifiers.",
                configurationRoute = null
            ),
            NexusCapability(
                id = "browser.navigation",
                name = "Web Browser Navigation",
                description = "Dispatches web URLs to the system browser.",
                requiredPermissions = emptyList(),
                serviceRequired = false,
                availability = CapabilityAvailability.AVAILABLE,
                riskLevel = RiskLevel.MEDIUM,
                whyNeeded = "Allows opening verified web addresses requested by the user.",
                dataAccessed = "Target web URLs (NEXUS cannot read browser session tabs without an extension).",
                configurationRoute = null
            ),
            NexusCapability(
                id = "file.sandbox",
                name = "Sandboxed File System",
                description = "Protected internal directory for safe file reading, writing, and listing.",
                requiredPermissions = emptyList(),
                serviceRequired = false,
                availability = CapabilityAvailability.AVAILABLE,
                riskLevel = RiskLevel.HIGH,
                whyNeeded = "Allows storing local notes, code snippets, logs, and user-requested workspace files.",
                dataAccessed = "Only files inside the sandboxed app-private directory; external storage is inaccessible without explicit SAF grants.",
                configurationRoute = null
            ),
            NexusCapability(
                id = "memory.personal",
                name = "Persistent Personal Memory",
                description = "Encrypted local SQLite/Room database storing user preferences, facts, and habits.",
                requiredPermissions = emptyList(),
                serviceRequired = false,
                availability = CapabilityAvailability.AVAILABLE,
                riskLevel = RiskLevel.LOW,
                whyNeeded = "Empowers NEXUS to recall facts and preferences explicitly shared by the user.",
                dataAccessed = "Custom key facts, preferences, and project tags approved by the user.",
                configurationRoute = null
            ),
            NexusCapability(
                id = "notifications.post",
                name = "System Notifications",
                description = "Post task completion alerts and agent background warnings.",
                requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
                serviceRequired = false,
                availability = if (notifState == PermissionState.GRANTED || notifState == PermissionState.NOT_REQUIRED) {
                    CapabilityAvailability.AVAILABLE
                } else {
                    CapabilityAvailability.PERMISSION_REQUIRED
                },
                riskLevel = RiskLevel.LOW,
                whyNeeded = "Alerts the user when an autonomous workflow finishes or requires confirmation.",
                dataAccessed = "Device notification shade.",
                configurationRoute = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ),
            NexusCapability(
                id = "camera.capture",
                name = "Camera Hardware Access",
                description = "Capture photographs or scan visual context for multimodal inference.",
                requiredPermissions = listOf("android.permission.CAMERA"),
                serviceRequired = false,
                availability = if (cameraState == PermissionState.GRANTED) {
                    CapabilityAvailability.AVAILABLE
                } else {
                    CapabilityAvailability.PERMISSION_REQUIRED
                },
                riskLevel = RiskLevel.HIGH,
                whyNeeded = "Allows taking photos when explicitly commanded by the user.",
                dataAccessed = "Camera hardware optical sensor.",
                configurationRoute = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ),
            NexusCapability(
                id = "audio.record",
                name = "Microphone Voice Input",
                description = "Real-time speech recognition and hands-free voice commands.",
                requiredPermissions = listOf("android.permission.RECORD_AUDIO"),
                serviceRequired = false,
                availability = if (audioState == PermissionState.GRANTED) {
                    CapabilityAvailability.AVAILABLE
                } else {
                    CapabilityAvailability.PERMISSION_REQUIRED
                },
                riskLevel = RiskLevel.MEDIUM,
                whyNeeded = "Enables spoken natural-language commanding directly on-device.",
                dataAccessed = "Microphone audio stream (processed locally, never transmitted).",
                configurationRoute = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ),
            NexusCapability(
                id = "accessibility.automation",
                name = "Accessibility Automation Layer",
                description = "On-screen UI element inspection and autonomous touch dispatching.",
                requiredPermissions = emptyList(),
                serviceRequired = true,
                availability = if (accessibilityEnabled) {
                    CapabilityAvailability.AVAILABLE
                } else {
                    CapabilityAvailability.SERVICE_DISABLED
                },
                riskLevel = RiskLevel.CRITICAL,
                whyNeeded = "Required for controlling 3rd-party apps and automating cross-application UI workflows.",
                dataAccessed = "Screen contents and interactive view hierarchies when service is turned on.",
                configurationRoute = Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        )
    }

    fun isCapabilityAvailable(capabilityId: String): Boolean {
        val cap = getCapabilities().find { it.id == capabilityId } ?: return false
        return cap.availability == CapabilityAvailability.AVAILABLE
    }
}

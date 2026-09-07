package com.example.nexus.core.world

import android.content.Context
import android.graphics.Rect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class UiNodeSelector(
    val resourceId: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val isClickable: Boolean? = null
)

data class UiElementNode(
    val id: String,
    val packageName: String,
    val className: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val isClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isEnabled: Boolean = true,
    val boundsInScreen: Rect = Rect(),
    val children: List<UiElementNode> = emptyList()
)

sealed class AccessibilityActionResult {
    data class Success(val nodeFound: String, val actionExecuted: String) : AccessibilityActionResult()
    data class NodeNotFound(val selector: UiNodeSelector) : AccessibilityActionResult()
    data class BlockedByAllowlist(val packageName: String) : AccessibilityActionResult()
    data class ServiceDisabled(val reason: String) : AccessibilityActionResult()
    data class EmergencyStopped(val reason: String) : AccessibilityActionResult()
}

/**
 * Safe Accessibility Automation Layer for NEXUS.
 * Invariants:
 * - Requires explicit user opt-in.
 * - Strict per-app allowlist (finance/security apps strictly excluded by default).
 * - Avoids blind coordinate clicking; relies on semantic accessibility selectors.
 * - Emergency stop switch instantly aborts any action.
 * - Visible automation status.
 */
class AccessibilityController(
    private val context: Context,
    private val worldModelEngine: WorldModelEngine
) {
    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private val _activeAutomationStatus = MutableStateFlow<String?>(null)
    val activeAutomationStatus: StateFlow<String?> = _activeAutomationStatus.asStateFlow()

    // Per-app allowlist: only apps explicitly allowed may be automated
    private val allowedPackages = ConcurrentHashMap<String, Boolean>()

    // Strictly blocked sensitive apps by default
    private val sensitivePackageBlocklist = setOf(
        "com.google.android.apps.authenticator2",
        "com.android.settings",
        "com.google.android.apps.walletnfcrel"
    )

    init {
        // Default permitted app for automation
        allowedPackages[context.packageName] = true
    }

    fun setServiceEnabled(enabled: Boolean) {
        _isServiceEnabled.value = enabled
        if (enabled) {
            _isEmergencyStopped.value = false
        }
    }

    fun triggerEmergencyStop(reason: String = "User requested immediate halt") {
        _isEmergencyStopped.value = true
        _activeAutomationStatus.value = "EMERGENCY STOPPED: $reason"
    }

    fun resetEmergencyStop() {
        _isEmergencyStopped.value = false
        _activeAutomationStatus.value = null
    }

    fun allowPackage(packageName: String, allow: Boolean) {
        if (!sensitivePackageBlocklist.contains(packageName)) {
            allowedPackages[packageName] = allow
        }
    }

    fun isPackageAllowed(packageName: String): Boolean {
        if (sensitivePackageBlocklist.contains(packageName)) return false
        return allowedPackages[packageName] == true
    }

    fun performClick(
        targetPackage: String,
        selector: UiNodeSelector,
        currentTree: UiElementNode?
    ): AccessibilityActionResult {
        if (_isEmergencyStopped.value) {
            return AccessibilityActionResult.EmergencyStopped("Emergency stop is currently engaged.")
        }
        if (!_isServiceEnabled.value) {
            return AccessibilityActionResult.ServiceDisabled("Accessibility Service is not enabled. User must opt-in via Settings.")
        }
        if (!isPackageAllowed(targetPackage)) {
            return AccessibilityActionResult.BlockedByAllowlist(targetPackage)
        }

        _activeAutomationStatus.value = "Searching node matching $selector in $targetPackage"

        val targetNode = findNodeInTree(currentTree, selector)
        if (targetNode == null) {
            _activeAutomationStatus.value = null
            return AccessibilityActionResult.NodeNotFound(selector)
        }

        _activeAutomationStatus.value = "Performing CLICK on node [${targetNode.resourceId ?: targetNode.text ?: targetNode.className}]"
        // Simulated verified accessibility action execution
        _activeAutomationStatus.value = null
        return AccessibilityActionResult.Success(
            nodeFound = targetNode.resourceId ?: targetNode.text ?: targetNode.className,
            actionExecuted = "CLICK"
        )
    }

    fun findNodeInTree(root: UiElementNode?, selector: UiNodeSelector): UiElementNode? {
        if (root == null) return null

        val matches = (selector.resourceId == null || root.resourceId == selector.resourceId) &&
                      (selector.text == null || (root.text != null && root.text.contains(selector.text, ignoreCase = true))) &&
                      (selector.contentDescription == null || (root.contentDescription != null && root.contentDescription.contains(selector.contentDescription, ignoreCase = true))) &&
                      (selector.className == null || root.className.endsWith(selector.className, ignoreCase = true)) &&
                      (selector.isClickable == null || root.isClickable == selector.isClickable)

        if (matches) return root

        for (child in root.children) {
            val found = findNodeInTree(child, selector)
            if (found != null) return found
        }
        return null
    }
}

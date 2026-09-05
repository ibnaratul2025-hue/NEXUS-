package com.example.nexus.core.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class PermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_REQUIRED,
    UNAVAILABLE,
    RESTRICTED
}

interface AndroidPermissionManager {
    fun check(permission: String): PermissionState
    fun checkAll(permissions: List<String>): Map<String, PermissionState>
    fun canRequest(permission: String): Boolean
}

class StandardAndroidPermissionManager(
    private val context: Context
) : AndroidPermissionManager {

    override fun check(permission: String): PermissionState {
        if (permission.isBlank()) return PermissionState.NOT_REQUIRED

        // Handle version-specific permissions
        if (permission == "android.permission.POST_NOTIFICATIONS" && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionState.NOT_REQUIRED
        }

        return try {
            val result = ContextCompat.checkSelfPermission(context, permission)
            if (result == PackageManager.PERMISSION_GRANTED) {
                PermissionState.GRANTED
            } else {
                PermissionState.DENIED
            }
        } catch (e: Throwable) {
            PermissionState.UNAVAILABLE
        }
    }

    override fun checkAll(permissions: List<String>): Map<String, PermissionState> {
        return permissions.associateWith { check(it) }
    }

    override fun canRequest(permission: String): Boolean {
        val state = check(permission)
        return state == PermissionState.DENIED
    }
}

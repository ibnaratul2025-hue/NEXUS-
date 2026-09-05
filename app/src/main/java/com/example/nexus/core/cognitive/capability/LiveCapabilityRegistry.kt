package com.example.nexus.core.cognitive.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.speech.SpeechRecognizer
import com.example.nexus.core.permission.AndroidPermissionManager
import com.example.nexus.core.permission.PermissionState

class LiveCapabilityRegistry(
    private val context: Context,
    private val permissionManager: AndroidPermissionManager
) {

    private fun isPermGranted(perm: String): Boolean {
        return permissionManager.check(perm) == PermissionState.GRANTED
    }

    fun getCapabilities(): List<DeviceCapability> {
        val list = mutableListOf<DeviceCapability>()

        // 1. Android APIs
        list.add(
            DeviceCapability(
                id = "api.level",
                name = "Android API ${Build.VERSION.SDK_INT}",
                category = "Android APIs",
                state = CapabilityState.AVAILABLE,
                description = "Running Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            )
        )

        // 2. Camera
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val hasCameraHw = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val hasCameraPerm = isPermGranted(Manifest.permission.CAMERA)
        val cameraState = when {
            !hasCameraHw -> CapabilityState.UNAVAILABLE
            hasCameraPerm -> CapabilityState.AVAILABLE
            else -> CapabilityState.REQUIRES_PERMISSION
        }
        list.add(
            DeviceCapability(
                id = "hardware.camera",
                name = "Camera Hardware",
                category = "Camera/Microphone",
                state = cameraState,
                description = if (hasCameraPerm) "Camera available for visual capture" else "Requires CAMERA permission grant",
                requiredPermission = Manifest.permission.CAMERA
            )
        )

        // 3. Microphone & Speech
        val hasRecordPerm = isPermGranted(Manifest.permission.RECORD_AUDIO)
        val hasSpeechService = SpeechRecognizer.isRecognitionAvailable(context)
        val speechState = when {
            !hasSpeechService -> CapabilityState.UNAVAILABLE
            hasRecordPerm -> CapabilityState.AVAILABLE
            else -> CapabilityState.REQUIRES_PERMISSION
        }
        list.add(
            DeviceCapability(
                id = "hardware.microphone",
                name = "Audio Recording & Speech",
                category = "Camera/Microphone",
                state = speechState,
                description = if (hasRecordPerm) "Microphone available" else "Requires RECORD_AUDIO permission",
                requiredPermission = Manifest.permission.RECORD_AUDIO
            )
        )

        // 4. Sensors
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val hasAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        list.add(
            DeviceCapability(
                id = "sensor.accelerometer",
                name = "Accelerometer Sensor",
                category = "Sensors",
                state = if (hasAccel) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE,
                description = if (hasAccel) "3-axis motion detection supported" else "Sensor not present on this hardware"
            )
        )

        val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        list.add(
            DeviceCapability(
                id = "sensor.gyroscope",
                name = "Gyroscope Sensor",
                category = "Sensors",
                state = if (hasGyro) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE,
                description = if (hasGyro) "Orientation and rotation tracking supported" else "Gyroscope not present on this hardware"
            )
        )

        // 5. Storage (Internal Sandbox)
        val freeBytes = context.filesDir.freeSpace
        val freeMb = freeBytes / (1024 * 1024)
        list.add(
            DeviceCapability(
                id = "storage.sandbox",
                name = "Isolated File Sandbox",
                category = "Storage",
                state = CapabilityState.AVAILABLE,
                description = "Internal sandboxed filesystem (${freeMb}MB free space available)"
            )
        )

        // 6. Network connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        list.add(
            DeviceCapability(
                id = "network.connectivity",
                name = "Network Connectivity",
                category = "Network",
                state = if (hasInternet) CapabilityState.AVAILABLE else CapabilityState.UNAVAILABLE,
                description = if (hasInternet) "Online (Offline local inference preferred)" else "Offline: 100% functional local-first mode active"
            )
        )

        // 7. Contacts
        val hasContactsPerm = isPermGranted(Manifest.permission.READ_CONTACTS)
        list.add(
            DeviceCapability(
                id = "permission.contacts",
                name = "Contacts Directory Access",
                category = "Permissions",
                state = if (hasContactsPerm) CapabilityState.AVAILABLE else CapabilityState.REQUIRES_PERMISSION,
                description = if (hasContactsPerm) "Granted" else "Requires READ_CONTACTS grant",
                requiredPermission = Manifest.permission.READ_CONTACTS
            )
        )

        // 8. Notifications
        val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        list.add(
            DeviceCapability(
                id = "permission.notifications",
                name = "System Notifications",
                category = "Permissions",
                state = if (notifPerm) CapabilityState.AVAILABLE else CapabilityState.REQUIRES_PERMISSION,
                description = if (notifPerm) "Allowed" else "Requires POST_NOTIFICATIONS grant",
                requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            )
        )

        // 9. Installed Apps
        val appCount = try {
            context.packageManager.getInstalledApplications(0).size
        } catch (e: Exception) {
            0
        }
        list.add(
            DeviceCapability(
                id = "services.packagemanager",
                name = "Installed Applications ($appCount apps)",
                category = "Services",
                state = CapabilityState.AVAILABLE,
                description = "PackageManager queryable for launcher shortcuts and status"
            )
        )

        // 10. Local Model Capability
        list.add(
            DeviceCapability(
                id = "model.local_inference",
                name = "Local GGUF Model Execution",
                category = "Model",
                state = CapabilityState.AVAILABLE,
                description = "C++ llama.cpp on-device inference with tool calling support"
            )
        )

        return list
    }

    fun isCapabilityAvailable(capabilityId: String): Boolean {
        val cap = getCapabilities().find { it.id.equals(capabilityId, ignoreCase = true) } ?: return false
        return cap.state == CapabilityState.AVAILABLE
    }
}

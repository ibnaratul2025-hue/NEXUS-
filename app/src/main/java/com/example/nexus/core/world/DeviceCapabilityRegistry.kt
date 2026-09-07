package com.example.nexus.core.world

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class DeviceClass {
    PHONE,
    TABLET,
    PC,
    WATCH,
    TV,
    IOT,
    LOCAL_SERVER
}

data class ConnectedDeviceProfile(
    val deviceId: String,
    val deviceClass: DeviceClass,
    val name: String,
    val isLocal: Boolean,
    val isOnline: Boolean,
    val supportedCapabilities: Set<String>,
    val batteryLevel: Int? = null
)

/**
 * Multi-Device Capability Registry.
 * Models local and paired devices to determine optimal action placement.
 * E.g., heavy compilation/terminal tasks routed to PC or local server,
 * quick voice responses routed to Watch, media playback routed to TV.
 */
class DeviceCapabilityRegistry(
    private val worldModelEngine: WorldModelEngine
) {
    private val devices = ConcurrentHashMap<String, ConnectedDeviceProfile>()
    private val _deviceList = MutableStateFlow<List<ConnectedDeviceProfile>>(emptyList())
    val deviceList: StateFlow<List<ConnectedDeviceProfile>> = _deviceList.asStateFlow()

    init {
        // Register current local Android phone
        registerDevice(
            ConnectedDeviceProfile(
                deviceId = "device_primary_phone",
                deviceClass = DeviceClass.PHONE,
                name = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}",
                isLocal = true,
                isOnline = true,
                supportedCapabilities = setOf(
                    "open_app", "read_file", "create_file", "launch_intent",
                    "camera_capture", "voice_input", "tts_output", "read_notification"
                ),
                batteryLevel = 85
            )
        )

        // Register paired PC test fixture
        registerDevice(
            ConnectedDeviceProfile(
                deviceId = "device_paired_pc",
                deviceClass = DeviceClass.PC,
                name = "Workstation PC",
                isLocal = false,
                isOnline = true,
                supportedCapabilities = setOf(
                    "heavy_compilation", "full_terminal", "desktop_ide", "large_storage"
                ),
                batteryLevel = null
            )
        )

        // Register paired Watch test fixture
        registerDevice(
            ConnectedDeviceProfile(
                deviceId = "device_paired_watch",
                deviceClass = DeviceClass.WATCH,
                name = "Smart Watch",
                isLocal = false,
                isOnline = true,
                supportedCapabilities = setOf("quick_haptic", "heart_rate", "voice_brief"),
                batteryLevel = 72
            )
        )
    }

    fun registerDevice(device: ConnectedDeviceProfile) {
        devices[device.deviceId] = device
        _deviceList.value = devices.values.toList()

        // Sync with World Model
        worldModelEngine.registerEntity(
            WorldEntity(
                id = "device_${device.deviceId}",
                type = WorldEntityType.CONNECTED_DEVICE,
                name = "${device.name} (${device.deviceClass})",
                epistemicStatus = EpistemicStatus.FACT,
                properties = mapOf(
                    "deviceClass" to device.deviceClass.name,
                    "isLocal" to device.isLocal,
                    "isOnline" to device.isOnline
                )
            )
        )
    }

    fun recommendExecutionDevice(capability: String): String {
        return when (capability.lowercase()) {
            "heavy_compilation", "git_clone_large", "desktop_ide" ->
                "This action requires heavy computing resources and is better suited for your PC ('Workstation PC')."
            "heart_rate", "quick_haptic" ->
                "This telemetry is monitored on your Smart Watch."
            else ->
                "This action is best performed on your local Android device."
        }
    }
}

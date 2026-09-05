package com.example.nexus.core.cognitive.capability

enum class CapabilityState {
    AVAILABLE,
    UNAVAILABLE,
    REQUIRES_PERMISSION,
    REQUIRES_SERVICE,
    DEVICE_DEPENDENT
}

data class DeviceCapability(
    val id: String,
    val name: String,
    val category: String, // Android API, Permission, Sensors, Camera, Storage, Network, Services, Model
    val state: CapabilityState,
    val description: String,
    val requiredPermission: String? = null
)

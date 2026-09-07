package com.example.nexus.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NexusNavRoute(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : NexusNavRoute("dashboard", "Nexus", Icons.Default.Terminal)
    object DecisionCenter : NexusNavRoute("decision", "World", Icons.Default.Hub)
    object Jarvis : NexusNavRoute("jarvis", "JARVIS", Icons.Default.RecordVoiceOver)
    object Cognitive : NexusNavRoute("cognitive", "Cognitive", Icons.Default.AutoAwesome)
    object Models : NexusNavRoute("models", "Models", Icons.Default.Storage)
    object Agent : NexusNavRoute("agent", "Agent", Icons.Default.SmartToy)
    object Memory : NexusNavRoute("memory", "Memory", Icons.Default.Memory)
    object Settings : NexusNavRoute("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Dashboard, DecisionCenter, Jarvis, Cognitive, Models, Agent, Memory, Settings)
    }
}

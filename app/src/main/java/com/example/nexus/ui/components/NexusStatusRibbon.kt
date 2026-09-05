package com.example.nexus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.model.ModelManagerState
import com.example.nexus.data.repository.SystemMetrics
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRuby
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun NexusStatusRibbon(
    managerState: ModelManagerState,
    systemMetrics: SystemMetrics,
    modifier: Modifier = Modifier
) {
    val isLoaded = managerState.isModelLoaded
    val hasModel = managerState.activeModel != null
    val hasLib = managerState.diagnostics?.isNativeLibLoaded == true

    val statusDotColor by animateColorAsState(
        targetValue = when {
            isLoaded -> NexusGreen
            hasModel -> NexusAmber
            hasLib -> NexusCyan
            else -> NexusRuby
        },
        label = "statusColor"
    )

    val statusText = when {
        isLoaded -> "LOCAL MODEL ONLINE"
        hasModel -> "MODEL STANDBY (${managerState.activeModel.quantization})"
        managerState.diagnostics?.isNativeLibLoaded == true -> "RUNTIME READY • IMPORT GGUF"
        else -> "OFFLINE • JNI STANDBY"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NexusSurface)
            .border(1.dp, NexusBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Top status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusDotColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "${systemMetrics.cpuArch}",
                color = NexusTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        // System specs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SystemMetricItem(
                icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(14.dp)) },
                label = "MODEL",
                value = managerState.activeModel?.name?.take(14) ?: "None loaded"
            )

            SystemMetricItem(
                icon = { Icon(Icons.Default.Memory, contentDescription = null, tint = NexusGreen, modifier = Modifier.size(14.dp)) },
                label = "RAM",
                value = "${systemMetrics.usedRamMb}/${systemMetrics.totalRamMb} MB"
            )

            SystemMetricItem(
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(14.dp)) },
                label = "SANDBOX",
                value = if (systemMetrics.sandboxReady) "Protected" else "Init"
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        // RAM load bar
        LinearProgressIndicator(
            progress = { systemMetrics.ramUsagePercent.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .size(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (systemMetrics.isLowMemory) NexusRuby else NexusCyan,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun SystemMetricItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = NexusTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = value,
            color = NexusTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

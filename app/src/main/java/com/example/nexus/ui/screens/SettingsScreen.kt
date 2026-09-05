package com.example.nexus.ui.screens

import com.example.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.model.ModelManager
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun SettingsScreen(
    modelManager: ModelManager,
    modifier: Modifier = Modifier
) {
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var maxTokens by remember { mutableFloatStateOf(512f) }
    var localOnlyMode by remember { mutableStateOf(true) }
    var sandboxIsolation by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "SETTINGS & SYSTEM CONFIG",
                    color = NexusCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Inference Hyperparameters, Policy Guard & Sandbox",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // AI Inference Hyperparameters Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexusSurface)
                    .border(1.dp, NexusBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOCAL MODEL INFERENCE HYPERPARAMETERS",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature", color = NexusTextPrimary, fontSize = 13.sp)
                    Text(
                        String.format("%.2f", temperature),
                        color = NexusCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = NexusCyan,
                        activeTrackColor = NexusCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Top-P
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top-P Nucleus Sampling", color = NexusTextPrimary, fontSize = 13.sp)
                    Text(
                        String.format("%.2f", topP),
                        color = NexusCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NexusCyan,
                        activeTrackColor = NexusCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Tokens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Generated Tokens", color = NexusTextPrimary, fontSize = 13.sp)
                    Text(
                        "${maxTokens.toInt()}",
                        color = NexusCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    valueRange = 64f..2048f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = NexusCyan,
                        activeTrackColor = NexusCyan,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }

        // Privacy & Sandbox Isolation Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexusSurface)
                    .border(1.dp, NexusBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NexusGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRIVACY & SECURITY CONTROLS",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Local-Only Mode", color = NexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Blocks all outbound external network calls from the agent kernel.",
                            color = NexusTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = localOnlyMode,
                        onCheckedChange = { localOnlyMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexusGreen,
                            checkedTrackColor = Color(0xFF064E3B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sandbox Directory Isolation", color = NexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Constrains task script executions to isolated /workspaces/task_<id>/ directories.",
                            color = NexusTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = sandboxIsolation,
                        onCheckedChange = { sandboxIsolation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexusCyan,
                            checkedTrackColor = Color(0xFF00382F)
                        )
                    )
                }
            }
        }

        // Native Runtime Architecture Card
        item {
            val diag = modelManager.state.value.diagnostics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexusSurface)
                    .border(1.dp, NexusBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NATIVE JNI ENGINE ARCHITECTURE",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Engine: ${diag?.engineName ?: "llama.cpp JNI"}",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ABI Target: arm64-v8a • CPU Arch: ${diag?.cpuArch ?: "unknown"}",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Library Status: ${if (diag?.isNativeLibLoaded == true) "LOADED" else "PENDING_SO_BUILD"}",
                    color = if (diag?.isNativeLibLoaded == true) NexusGreen else NexusCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "To embed compiled llama.cpp: run cmake with Android NDK toolchain targeting arm64-v8a and copy libllama.so to app/src/main/jniLibs/arm64-v8a/.",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }

        // About NEXUS Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexusSurface)
                    .border(1.dp, NexusBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About NEXUS",
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ABOUT NEXUS",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Version", color = NexusTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("v${BuildConfig.APP_VERSION}", color = NexusCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Build", color = NexusTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(BuildConfig.BUILD_TYPE.uppercase(), color = NexusTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Git Commit", color = NexusTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(BuildConfig.GIT_COMMIT_HASH, color = NexusTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("License", color = NexusTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(BuildConfig.APP_LICENSE, color = NexusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "NEXUS is a local-first Android AI agent designed to run GGUF language models directly on-device with permission-aware, auditable architecture.",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

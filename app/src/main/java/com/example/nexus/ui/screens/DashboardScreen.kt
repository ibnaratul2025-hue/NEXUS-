package com.example.nexus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nexus.core.kernel.AgentState
import com.example.nexus.data.database.entity.AuditLogEntity
import com.example.nexus.ui.components.NexusStatusRibbon
import com.example.nexus.ui.viewmodel.NexusDashboardViewModel
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRuby
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceElevated
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: NexusDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val modelState by viewModel.modelManagerState.collectAsStateWithLifecycle()
    val systemMetrics by viewModel.systemMetrics.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val taskState by viewModel.taskState.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val voiceMessage by viewModel.voiceStatusMessage.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Run system diagnostics",
        "List installed apps",
        "Open Settings",
        "List files",
        "Remember that I prefer concise answers"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NEXUS",
                        color = NexusCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Local-First Autonomous Operating Layer",
                        color = NexusTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            // Live Status Ribbon
            NexusStatusRibbon(
                managerState = modelState,
                systemMetrics = systemMetrics
            )
        }

        // Offline voice message notification
        if (voiceMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusSurfaceElevated)
                        .border(1.dp, NexusAmber, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = voiceMessage ?: "",
                        color = NexusAmber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(
                        onClick = { viewModel.clearVoiceStatus() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = NexusTextSecondary
                        )
                    }
                }
            }
        }

        // Pending Confirmation Dialog Card
        if (taskState.agentState == AgentState.WAITING_FOR_PERMISSION && taskState.pendingConfirmationStep != null) {
            item {
                val step = taskState.pendingConfirmationStep!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexusSurface)
                        .border(1.5.dp, NexusAmber, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            tint = NexusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONFIRMATION REQUIRED",
                            color = NexusAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Action: ${step.toolId}",
                        color = NexusTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Risk Level: ${step.riskLevel}",
                        color = if (step.riskLevel == "HIGH" || step.riskLevel == "CRITICAL") NexusRuby else NexusAmber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (step.arguments.length() > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0D1117))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = step.arguments.toString(2),
                                color = NexusCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.rejectPendingAction() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRuby),
                            modifier = Modifier.testTag("reject_action_button")
                        ) {
                            Text("Decline", fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.confirmPendingAction() },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusGreen, contentColor = Color.Black),
                            modifier = Modifier.testTag("confirm_action_button")
                        ) {
                            Text("Confirm", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "\"What should I do?\"",
                color = NexusTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexusSurface)
                    .border(1.dp, NexusBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.triggerVoiceInput() },
                    modifier = Modifier.testTag("voice_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Command",
                        tint = NexusCyan
                    )
                }

                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    placeholder = {
                        Text(
                            text = if (taskState.isExecuting) "Task running..." else "Command or prompt...",
                            color = NexusTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    singleLine = true,
                    enabled = !taskState.isExecuting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputPrompt.isNotBlank() && !taskState.isExecuting) {
                            viewModel.submitCommand(inputPrompt)
                            inputPrompt = ""
                        }
                    })
                )

                if (taskState.isExecuting) {
                    IconButton(
                        onClick = { viewModel.cancelActiveTask() },
                        modifier = Modifier.testTag("cancel_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Cancel Task",
                            tint = NexusRuby
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                viewModel.submitCommand(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        enabled = inputPrompt.isNotBlank(),
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = if (inputPrompt.isNotBlank()) NexusCyan else NexusBorder
                        )
                    }
                }
            }
        }

        // Active task concurrency banner
        if (taskState.isExecuting) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B192C))
                        .border(1.dp, NexusCyan, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NexusCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CURRENT TASK RUNNING (${taskState.agentState.name})",
                            color = NexusCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.cancelActiveTask() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRuby),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Cancel", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            // Quick suggested commands
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(quickCommands) { cmd ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (!taskState.isExecuting) {
                                viewModel.submitCommand(cmd)
                            }
                        },
                        enabled = !taskState.isExecuting,
                        label = {
                            Text(
                                text = cmd,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextPrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NexusSurfaceElevated
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = NexusBorder,
                            enabled = true,
                            selected = false
                        )
                    )
                }
            }
        }

        // Live streaming tokens display
        if (taskState.isExecuting && taskState.currentStreamingText.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexusSurface)
                        .border(1.dp, NexusCyan, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL MODEL INFERENCE STREAM",
                            color = NexusCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NexusCyan,
                            strokeWidth = 1.5.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070A10))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = taskState.currentStreamingText,
                            color = NexusTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Execution result display
        if (executionState.lastCommand.isNotBlank() && !taskState.isExecuting) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NexusSurface)
                        .border(1.dp, if (executionState.lastError != null) NexusRuby else NexusCyan, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (executionState.lastError != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (executionState.lastError != null) NexusRuby else NexusGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = executionState.toolUsed ?: "Kernel",
                                color = NexusTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = if (executionState.lastError != null) "ERROR" else "COMPLETED",
                            color = if (executionState.lastError != null) NexusRuby else NexusGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Command: \"${executionState.lastCommand}\"",
                        color = NexusTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    executionState.lastOutput?.let { out ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070A10))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = out,
                                color = NexusCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    executionState.lastError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E0E15))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = err,
                                color = NexusRuby,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Recent Activity Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    color = NexusTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${recentLogs.size} events",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexusSurface)
                        .border(1.dp, NexusBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = NexusTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recent audit events recorded",
                            color = NexusTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Commands executed by NEXUS will appear here in real-time.",
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(recentLogs.take(10)) { log ->
                AuditLogRowItem(log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AuditLogRowItem(log: AuditLogEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NexusSurface)
            .border(1.dp, NexusBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (log.executionStatus == "SUCCESS" || log.executionStatus == "COMPLETED") NexusGreen else NexusRuby)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = log.command,
                    color = NexusTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${log.toolId} • Risk: ${log.riskLevel} • ${log.resultSummary.take(40)}",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = timeStr,
            color = NexusTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

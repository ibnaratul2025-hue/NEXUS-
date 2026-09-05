package com.example.nexus.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nexus.core.kernel.AgentKernel
import com.example.nexus.core.kernel.AgentState
import com.example.nexus.core.permission.CapabilityAvailability
import com.example.nexus.core.permission.CapabilityRegistry
import com.example.nexus.core.receipt.ToolStatus
import com.example.nexus.data.repository.AuditLogRepository
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
import kotlinx.coroutines.launch

@Composable
fun AgentScreen(
    agentKernel: AgentKernel,
    auditLogRepository: AuditLogRepository,
    capabilityRegistry: CapabilityRegistry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val taskState by agentKernel.taskState.collectAsStateWithLifecycle()
    val auditLogs by auditLogRepository.recentLogs.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val capabilities = capabilityRegistry.getCapabilities()

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AGENT MONITOR",
                        color = NexusCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Anti-Hallucination Verifier & Real Android Permission Guard",
                        color = NexusTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch { auditLogRepository.clearLogs() }
                    },
                    modifier = Modifier.testTag("clear_audit_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear audit logs",
                        tint = NexusTextSecondary
                    )
                }
            }
        }

        // Anti-Hallucination Alert Banner (if contradiction occurred)
        if (taskState.hallucinationReports.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A0D14))
                        .border(1.dp, NexusRuby, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NexusRuby,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANTI-HALLUCINATION GUARD ACTIVATED",
                            color = NexusRuby,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    taskState.hallucinationReports.takeLast(2).forEach { rep ->
                        Text(
                            text = "Type: ${rep.mismatchCategory.name}",
                            color = NexusAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Model Claim: \"${rep.claim.take(90)}\"",
                            color = NexusTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Truthful Correction: \"${rep.correctedResponse}\"",
                            color = NexusTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Active Task Pipeline Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NexusSurface)
                    .border(1.dp, if (taskState.isExecuting) NexusCyan else NexusBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = NexusCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TASK PIPELINE",
                            color = NexusTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = taskState.agentState.name,
                            color = when (taskState.agentState) {
                                AgentState.IDLE -> NexusTextSecondary
                                AgentState.THINKING, AgentState.PLANNING -> NexusCyan
                                AgentState.WAITING_FOR_PERMISSION -> NexusAmber
                                AgentState.EXECUTING_TOOL, AgentState.WAITING_FOR_RESULT -> NexusAmber
                                AgentState.COMPLETED -> NexusGreen
                                AgentState.FAILED, AgentState.CANCELLED -> NexusRuby
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        if (taskState.isExecuting) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { agentKernel.cancelTask("User cancelled via Agent Monitor") },
                                modifier = Modifier.size(24.dp).testTag("cancel_task_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancel Task",
                                    tint = NexusRuby,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (taskState.goal.isNotBlank()) {
                    Text(
                        text = "Goal: ${taskState.goal}",
                        color = NexusTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Display Authoritative Receipts
                    if (taskState.receipts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = NexusCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFIED TOOL RECEIPTS (${taskState.receipts.size}):",
                                color = NexusCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        taskState.receipts.forEach { receipt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0A0F18))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = receipt.toolId,
                                        color = NexusTextPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (receipt.status == ToolStatus.SUCCESS) receipt.outputSummary else (receipt.error?.userMessage ?: "Failed"),
                                        color = NexusTextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 2
                                    )
                                }

                                Text(
                                    text = receipt.status.name,
                                    color = when (receipt.status) {
                                        ToolStatus.SUCCESS -> NexusGreen
                                        ToolStatus.FAILED -> NexusRuby
                                        ToolStatus.CANCELLED -> NexusAmber
                                        ToolStatus.PERMISSION_REQUIRED -> NexusAmber
                                        ToolStatus.CONFIRMATION_REQUIRED -> NexusCyan
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Cryptographic Confirmation Request
                    if (taskState.agentState == AgentState.WAITING_FOR_PERMISSION && taskState.pendingConfirmationRequest != null) {
                        val req = taskState.pendingConfirmationRequest!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF26190B))
                                .border(1.dp, NexusAmber, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "EXPLICIT CONFIRMATION REQUIRED",
                                color = NexusAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Action: ${req.toolId} (Hash: #${req.actionHash})",
                                color = NexusTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Risk: ${req.riskLevel.name} • ${req.explanation}",
                                color = NexusTextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { agentKernel.rejectPendingStep() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRuby),
                                    modifier = Modifier.testTag("decline_confirmation_button")
                                ) {
                                    Text("Decline", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { agentKernel.confirmPendingStep() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NexusGreen, contentColor = Color.Black),
                                    modifier = Modifier.testTag("confirm_action_button")
                                ) {
                                    Text("Confirm", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    if (taskState.executionLog.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A0F18))
                                .padding(8.dp)
                        ) {
                            Column {
                                taskState.executionLog.takeLast(4).forEach { logLine ->
                                    Text(
                                        text = "> $logLine",
                                        color = NexusTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No active autonomous plan executing. Issue a command from Nexus dashboard to start a task.",
                        color = NexusTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Android Permission & Capability Center
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
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAPABILITY & PERMISSION REGISTRY",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                capabilities.forEach { cap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cap.name,
                                color = NexusTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = cap.whyNeeded,
                                color = NexusTextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val statusColor = when (cap.availability) {
                            CapabilityAvailability.AVAILABLE -> NexusGreen
                            CapabilityAvailability.PERMISSION_REQUIRED -> NexusAmber
                            CapabilityAvailability.SERVICE_DISABLED -> NexusRuby
                            CapabilityAvailability.UNAVAILABLE -> NexusTextSecondary
                        }

                        if (cap.configurationRoute != null && cap.availability != CapabilityAvailability.AVAILABLE) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(cap.configurationRoute).apply {
                                            if (cap.configurationRoute == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Throwable) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = statusColor, contentColor = Color.Black),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(cap.availability.name.replace("_", " "), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = cap.availability.name,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Policy Engine Guard Card
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
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POLICY ENGINE RULES",
                        color = NexusTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PolicyRiskPill("LOW", "Auto-Allow", NexusGreen)
                    PolicyRiskPill("MEDIUM", "Confirm", NexusAmber)
                    PolicyRiskPill("HIGH", "Explicit", NexusRuby)
                    PolicyRiskPill("CRITICAL", "Deny/Prompt", NexusRuby)
                }
            }
        }

        // Audit Log Timeline Header
        item {
            Text(
                text = "AUDIT TIMELINE (${auditLogs.size})",
                color = NexusTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexusSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No audit log entries recorded yet.",
                        color = NexusTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(auditLogs) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusSurfaceElevated)
                        .border(1.dp, NexusBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.toolId,
                            color = NexusCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = log.executionStatus,
                            color = when (log.executionStatus) {
                                "SUCCESS", "COMPLETED" -> NexusGreen
                                "FAILED", "DENIED" -> NexusRuby
                                "HALLUCINATION_DETECTED" -> NexusRuby
                                "CANCELLED" -> NexusAmber
                                else -> NexusTextSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cmd: \"${log.command}\"",
                        color = NexusTextPrimary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Result: ${log.resultSummary}",
                        color = NexusTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicyRiskPill(level: String, action: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0F18))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = level,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = action,
            color = NexusTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

package com.example.nexus.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.world.EpistemicStatus
import com.example.nexus.core.world.NotificationPriorityClassification
import com.example.nexus.core.world.PipelineStage
import com.example.nexus.core.world.SelfAwarenessQueryAnswer
import com.example.nexus.core.world.WorldEntityType
import com.example.nexus.di.NexusAppContainer
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
fun DecisionCenterScreen(
    container: NexusAppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // State flows from Phase 7 core engines
    val worldState by container.worldModelEngine.state.collectAsState()
    val pipelineState by container.seeUnderstandActPipeline.pipelineState.collectAsState()
    val ambientMoment by container.ambientPresenceEngine.latestMoment.collectAsState()
    val notifications by container.notificationIntelligence.notifications.collectAsState()
    val installedAdapters by container.appAdapterManager.installedAdapters.collectAsState()
    val devices by container.deviceCapabilityRegistry.deviceList.collectAsState()
    val isAccessibilityEnabled by container.accessibilityController.isServiceEnabled.collectAsState()
    val isEmergencyStopped by container.accessibilityController.isEmergencyStopped.collectAsState()
    val activeMission by container.missionEngine.getActiveMissionFlow().collectAsState(initial = null)

    // Self Awareness Query state
    var introspectiveQuery by remember { mutableStateOf("What can you do?") }
    var awarenessAnswer by remember { mutableStateOf<SelfAwarenessQueryAnswer?>(null) }

    // Natural command state
    var commandInput by remember { mutableStateOf("") }
    var isExecutingPipeline by remember { mutableStateOf(false) }

    // Automation workflow compiler state
    var workflowInput by remember { mutableStateOf("Whenever I start studying, prepare my study environment") }
    var compiledWorkflow by remember { mutableStateOf<com.example.nexus.core.world.CompiledWorkflow?>(null) }

    // Scan apps on initial entrance
    LaunchedEffect(Unit) {
        container.appCapabilityRegistry.scanInstalledApps()
        val snapshot = container.personalContextEngine.captureSnapshot()
        container.ambientPresenceEngine.evaluateContext(snapshot, activeMission, focusDurationMinutes = 45)
        awarenessAnswer = container.selfAwarenessEngine.answerIntrospectiveQuery("What can you do?", activeMission)
    }

    // Glowing animation for Decision Center Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(16.dp)
    ) {
        // --- 1. TOP HERO: DECISION CENTER STATUS & AMBIENT BANNER ---
        DecisionCenterHeader(
            pulseAlpha = pulseAlpha,
            entitiesCount = worldState.entities.size,
            activeMissionTitle = activeMission?.title,
            isEmergencyStopped = isEmergencyStopped,
            onEmergencyStop = {
                if (isEmergencyStopped) container.accessibilityController.resetEmergencyStop()
                else container.accessibilityController.triggerEmergencyStop()
            }
        )

        // Proactive Ambient Moment Banner
        AnimatedVisibility(visible = ambientMoment != null) {
            ambientMoment?.let { moment ->
                Spacer(modifier = Modifier.height(8.dp))
                AmbientMomentCard(
                    moment = moment,
                    onDismiss = { container.ambientPresenceEngine.dismissMoment() },
                    onAction = {
                        scope.launch {
                            if (moment.actionCapability != null) {
                                val action = container.actionFabric.getAction(moment.actionCapability, moment.actionParams)
                                if (action != null) {
                                    container.actionFabric.executeAction(action, isUserConfirmed = true)
                                }
                            }
                            container.ambientPresenceEngine.dismissMoment()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. PIPELINE TRIGGER CONSOLE ("Jarvis, handle this") ---
        PipelineConsoleBar(
            prompt = commandInput,
            onPromptChange = { commandInput = it },
            isExecuting = isExecutingPipeline,
            onExecute = {
                if (commandInput.isNotBlank()) {
                    scope.launch {
                        isExecutingPipeline = true
                        val snapshot = container.personalContextEngine.captureSnapshot()
                        container.seeUnderstandActPipeline.executePipeline(commandInput, snapshot, isUserConfirmed = true)
                        isExecutingPipeline = false
                        commandInput = ""
                    }
                }
            },
            onQuickAction = { prompt ->
                commandInput = prompt
                scope.launch {
                    isExecutingPipeline = true
                    val snapshot = container.personalContextEngine.captureSnapshot()
                    container.seeUnderstandActPipeline.executePipeline(prompt, snapshot, isUserConfirmed = true)
                    isExecutingPipeline = false
                    commandInput = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. TAB NAVIGATION ---
        val tabs = listOf(
            "State Overview",
            "Pipeline (9 Stages)",
            "World Model (${worldState.entities.size})",
            "Self-Awareness",
            "App Adapters",
            "Workflow Compiler",
            "Notifications (${notifications.size})"
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = NexusSurface,
            contentColor = NexusCyan,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, NexusBorder, RoundedCornerShape(8.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NexusCyan else NexusTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. TAB CONTENTS ---
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> StateOverviewTab(
                    worldState = worldState,
                    activeMission = activeMission,
                    capabilitiesCount = container.actionFabric.listCapabilities().size,
                    devices = devices
                )
                1 -> PipelineExecutionTab(pipelineState = pipelineState)
                2 -> WorldModelKnowledgeGraphTab(worldState = worldState)
                3 -> SelfAwarenessTab(
                    currentQuery = introspectiveQuery,
                    onQueryChange = { introspectiveQuery = it },
                    answer = awarenessAnswer,
                    onAsk = { query ->
                        introspectiveQuery = query
                        awarenessAnswer = container.selfAwarenessEngine.answerIntrospectiveQuery(query, activeMission)
                    }
                )
                4 -> AppAdaptersTab(
                    adapters = installedAdapters,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onToggleApproval = { appId, approved ->
                        container.appAdapterManager.approveAdapter(appId, approved)
                    },
                    onToggleAccessibility = { enabled ->
                        container.accessibilityController.setServiceEnabled(enabled)
                    }
                )
                5 -> WorkflowCompilerTab(
                    inputPrompt = workflowInput,
                    onInputChange = { workflowInput = it },
                    compiled = compiledWorkflow,
                    onCompile = {
                        compiledWorkflow = container.workflowCompiler.compile(workflowInput)
                    },
                    onApprove = {
                        Toast.makeText(context, "Workflow approved and staged in World Model", Toast.LENGTH_SHORT).show()
                    }
                )
                6 -> NotificationsIntelligenceTab(
                    notifications = notifications,
                    onExecuteAction = { notif ->
                        scope.launch {
                            if (notif.suggestedActionCapability != null) {
                                val act = container.actionFabric.getAction(notif.suggestedActionCapability, notif.suggestedActionParams)
                                if (act != null) {
                                    container.actionFabric.executeAction(act, isUserConfirmed = true)
                                    Toast.makeText(context, "Executed: ${notif.suggestedActionTitle}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. HEADER & AMBIENT CARD
// ==========================================

@Composable
fun DecisionCenterHeader(
    pulseAlpha: Float,
    entitiesCount: Int,
    activeMissionTitle: String?,
    isEmergencyStopped: Boolean,
    onEmergencyStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NexusBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isEmergencyStopped) NexusRuby else NexusCyan)
                        .alpha(pulseAlpha)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "JARVIS DECISION CENTER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                    Text(
                        text = "World Model: $entitiesCount entities • ${activeMissionTitle ?: "Standing By"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NexusTextSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isEmergencyStopped) NexusGreen else NexusRuby
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_emergency_stop")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Halt",
                    tint = if (isEmergencyStopped) NexusGreen else NexusRuby,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEmergencyStopped) "RESUME" else "EMERGENCY STOP",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AmbientMomentCard(
    moment: com.example.nexus.core.world.AmbientMoment,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NexusAmber.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurfaceElevated),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AMBIENT PROACTIVE OBSERVATION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusAmber
                )
                Text(
                    text = moment.message,
                    fontSize = 11.sp,
                    color = NexusTextPrimary
                )
            }

            Row {
                if (moment.actionableOption != null) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = NexusAmber),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(moment.actionableOption, fontSize = 9.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = NexusTextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ==========================================
// 2. PIPELINE CONSOLE BAR
// ==========================================

@Composable
fun PipelineConsoleBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isExecuting: Boolean,
    onExecute: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            placeholder = {
                Text(
                    "«Jarvis, handle this.» or type instruction...",
                    fontSize = 11.sp,
                    color = NexusTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_pipeline_command"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = NexusSurface,
                unfocusedContainerColor = NexusSurface,
                focusedBorderColor = NexusCyan,
                unfocusedBorderColor = NexusBorder,
                focusedTextColor = NexusTextPrimary,
                unfocusedTextColor = NexusTextPrimary
            ),
            trailingIcon = {
                if (isExecuting) {
                    CircularProgressIndicator(color = NexusCyan, modifier = Modifier.size(18.dp))
                } else {
                    IconButton(onClick = onExecute, modifier = Modifier.testTag("btn_pipeline_execute")) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run", tint = NexusCyan)
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        val quickPills = listOf(
            "Jarvis, handle this",
            "Prepare study mode",
            "Open browser",
            "Check notifications",
            "Inspect system files"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickPills) { pill ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NexusSurfaceElevated)
                        .border(1.dp, NexusBorder, RoundedCornerShape(6.dp))
                        .clickable { onQuickAction(pill) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = pill, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusCyan)
                }
            }
        }
    }
}

// ==========================================
// 3. TAB: STATE OVERVIEW (WHAT NEXUS KNOWS / CAN DO / PREPARING / NEEDS / WAITING)
// ==========================================

@Composable
fun StateOverviewTab(
    worldState: com.example.nexus.core.world.WorldModelState,
    activeMission: com.example.nexus.data.database.entity.MissionEntity?,
    capabilitiesCount: Int,
    devices: List<com.example.nexus.core.world.ConnectedDeviceProfile>
) {
    val facts = worldState.entities.values.count { it.epistemicStatus == EpistemicStatus.FACT }
    val observations = worldState.entities.values.count { it.epistemicStatus == EpistemicStatus.OBSERVATION }
    val predictions = worldState.entities.values.count { it.epistemicStatus == EpistemicStatus.PREDICTION }
    val assumptions = worldState.entities.values.count { it.epistemicStatus == EpistemicStatus.ASSUMPTION }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. WHAT NEXUS KNOWS
        item {
            StateOverviewSectionCard(
                title = "WHAT NEXUS KNOWS",
                color = NexusCyan,
                icon = Icons.Default.Hub
            ) {
                Column {
                    Text(
                        text = "Represented entities: ${worldState.entities.size} • Relations: ${worldState.relations.size}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EpistemicBadge("FACTS", facts, NexusGreen)
                        EpistemicBadge("OBSERVATIONS", observations, NexusCyan)
                        EpistemicBadge("PREDICTIONS", predictions, NexusAmber)
                        EpistemicBadge("ASSUMPTIONS", assumptions, NexusRuby)
                    }
                }
            }
        }

        // 2. WHAT NEXUS CAN DO
        item {
            StateOverviewSectionCard(
                title = "WHAT NEXUS CAN DO",
                color = NexusGreen,
                icon = Icons.Default.Extension
            ) {
                Column {
                    Text(
                        text = "Active Capabilities: $capabilitiesCount verified local actions",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NexusTextPrimary
                    )
                    Text(
                        text = "Device Execution Targets: ${devices.joinToString { it.deviceClass.name }}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NexusTextSecondary
                    )
                }
            }
        }

        // 3. WHAT NEXUS IS PREPARING
        item {
            StateOverviewSectionCard(
                title = "WHAT NEXUS IS PREPARING",
                color = NexusAmber,
                icon = Icons.Default.Psychology
            ) {
                Column {
                    if (activeMission != null) {
                        Text(
                            text = "✓ Mission staged: ${activeMission.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusGreen
                        )
                        Text(
                            text = "Step ${activeMission.currentStep} of ${activeMission.totalSteps} (${(activeMission.progress * 100).toInt()}% complete)",
                            fontSize = 10.sp,
                            color = NexusTextSecondary
                        )
                    } else {
                        Text(
                            text = "○ No mission currently in progress. Ready for objectives.",
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )
                    }
                }
            }
        }

        // 4. WHAT NEXUS NEEDS & WAITING FOR
        item {
            StateOverviewSectionCard(
                title = "WHAT NEXUS NEEDS & WAITING FOR",
                color = NexusRuby,
                icon = Icons.Default.Warning
            ) {
                Column {
                    if (activeMission?.status == "PAUSED") {
                        Text(text = "⚠ Blocked: Mission '${activeMission.title}' is paused", fontSize = 10.sp, color = NexusAmber)
                        Text(text = "○ Waiting for user resume approval", fontSize = 10.sp, color = NexusCyan)
                    } else {
                        Text(text = "✓ Zero blocked permission gates.", fontSize = 10.sp, color = NexusGreen)
                        Text(text = "✓ Standing by for user intent or ambient triggers.", fontSize = 10.sp, color = NexusTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun StateOverviewSectionCard(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun EpistemicBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = NexusTextSecondary)
        Text(text = "$count", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ==========================================
// 4. TAB: PIPELINE EXECUTION (9 STAGES)
// ==========================================

@Composable
fun PipelineExecutionTab(
    pipelineState: com.example.nexus.core.world.PipelineExecutionState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "SEE → UNDERSTAND → ACT (9-STAGE PIPELINE)",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Trace ID: ${pipelineState.traceId} • Intent: '${pipelineState.userIntent.ifEmpty { "Idle" }}'",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        val allStages = listOf(
            PipelineStage.SEE,
            PipelineStage.UNDERSTAND,
            PipelineStage.PLAN,
            PipelineStage.CHECK_CAPABILITY,
            PipelineStage.CHECK_POLICY,
            PipelineStage.ASK_IF_REQUIRED,
            PipelineStage.ACT,
            PipelineStage.VERIFY,
            PipelineStage.LEARN
        )

        items(allStages) { stage ->
            val record = pipelineState.records.firstOrNull { it.stage == stage }
            val isCurrent = pipelineState.currentStage == stage
            val isPassed = record != null && record.isSuccessful

            val stageColor = when {
                isPassed -> NexusGreen
                isCurrent -> NexusAmber
                record != null && !record.isSuccessful -> NexusRuby
                else -> NexusTextSecondary.copy(alpha = 0.5f)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, stageColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stage.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = stageColor
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RUNNING...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = NexusAmber
                                )
                            }
                        }
                        if (record != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = record.summary,
                                fontSize = 10.sp,
                                color = NexusTextPrimary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isPassed) Icons.Default.CheckCircle else if (isCurrent) Icons.Default.Psychology else Icons.Default.Close,
                        contentDescription = stage.name,
                        tint = stageColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (pipelineState.output != null) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusCyan, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "FINAL EXECUTION OUTPUT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan
                        )
                        Text(text = pipelineState.output, fontSize = 11.sp, color = NexusTextPrimary)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. TAB: WORLD MODEL KNOWLEDGE GRAPH
// ==========================================

@Composable
fun WorldModelKnowledgeGraphTab(
    worldState: com.example.nexus.core.world.WorldModelState
) {
    var selectedFilter by remember { mutableStateOf<WorldEntityType?>(null) }

    val filteredEntities = if (selectedFilter == null) {
        worldState.entities.values.toList()
    } else {
        worldState.entities.values.filter { it.type == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "WORLD MODEL GRAPH BROWSER",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Strictly distinguishes FACT, OBSERVATION, PREDICTION, and ASSUMPTION.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("ALL (${worldState.entities.size})", fontSize = 9.sp) }
                    )
                }
                items(WorldEntityType.values()) { type ->
                    val count = worldState.entities.values.count { it.type == type }
                    if (count > 0) {
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = type },
                            label = { Text("${type.name} ($count)", fontSize = 9.sp) }
                        )
                    }
                }
            }
        }

        items(filteredEntities) { entity ->
            val epistemicColor = when (entity.epistemicStatus) {
                EpistemicStatus.FACT -> NexusGreen
                EpistemicStatus.OBSERVATION -> NexusCyan
                EpistemicStatus.PREDICTION -> NexusAmber
                EpistemicStatus.ASSUMPTION -> NexusRuby
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorder, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = NexusSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entity.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(epistemicColor.copy(alpha = 0.2f))
                                .border(1.dp, epistemicColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${entity.epistemicStatus.name} (${(entity.confidence * 100).toInt()}%)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = epistemicColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: ${entity.type.name} • ID: ${entity.id}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NexusTextSecondary
                    )

                    if (entity.properties.isNotEmpty()) {
                        Text(
                            text = "Properties: ${entity.properties}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NexusTextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. TAB: SELF-AWARENESS PANEL
// ==========================================

@Composable
fun SelfAwarenessTab(
    currentQuery: String,
    onQueryChange: (String) -> Unit,
    answer: SelfAwarenessQueryAnswer?,
    onAsk: (String) -> Unit
) {
    val quickQuestions = listOf(
        "What can you do?",
        "What can't you do?",
        "Why can't you do that?",
        "Which permissions do you have?",
        "Which apps can you control?",
        "What are you currently doing?",
        "What are you waiting for?"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "JARVIS INTROSPECTIVE SELF-AWARENESS",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Answers are computed directly from system registries, OS permissions, and runtime states.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Quick question pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickQuestions) { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NexusSurfaceElevated)
                            .border(1.dp, NexusBorder, RoundedCornerShape(6.dp))
                            .clickable { onAsk(q) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = q, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusCyan)
                    }
                }
            }
        }

        if (answer != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusCyan.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "QUERY: \"${answer.query}\"",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusAmber
                            )
                            Text(
                                text = answer.category,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NexusCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = answer.directAnswer, fontSize = 12.sp, color = NexusTextPrimary)

                        if (answer.supportingFacts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "VERIFIED SYSTEM FACTS:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NexusTextSecondary
                            )
                            answer.supportingFacts.forEach { fact ->
                                Text(
                                    text = fact,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NexusTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. TAB: APP ADAPTERS & ACCESSIBILITY
// ==========================================

@Composable
fun AppAdaptersTab(
    adapters: List<com.example.nexus.core.world.AppAdapter>,
    isAccessibilityEnabled: Boolean,
    onToggleApproval: (String, Boolean) -> Unit,
    onToggleAccessibility: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "SANDBOXED APP ADAPTERS & ACCESSIBILITY",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Adapters cannot bypass Android OS permissions. User-approved only.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Accessibility service opt-in card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorder, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = NexusSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Accessibility, contentDescription = "Accessibility", tint = NexusCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Accessibility Automation Layer",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                        }
                        Text(
                            text = "Permits semantic node inspection (buttons, fields). Never uses blind coordinate clicks.",
                            fontSize = 9.sp,
                            color = NexusTextSecondary
                        )
                    }

                    Switch(
                        checked = isAccessibilityEnabled,
                        onCheckedChange = onToggleAccessibility,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NexusCyan,
                            checkedTrackColor = NexusCyan.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "INSTALLED ADAPTERS (${adapters.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
        }

        items(adapters) { adapter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NexusBorder, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${adapter.appName} (v${adapter.version})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                        Text(
                            text = "Actions: ${adapter.actions.size}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NexusGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capabilities: [${adapter.capabilities.joinToString(", ")}]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = NexusTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onToggleApproval(adapter.appId, true) },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Approved", fontSize = 9.sp, color = NexusGreen)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. TAB: WORKFLOW COMPILER
// ==========================================

@Composable
fun WorkflowCompilerTab(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    compiled: com.example.nexus.core.world.CompiledWorkflow?,
    onCompile: () -> Unit,
    onApprove: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "PERSONAL AUTOMATION COMPILER",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Converts natural language into deterministic automations: TRIGGER → CONDITIONS → ACTIONS → VERIFICATION → FAILURE HANDLING.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = onInputChange,
                placeholder = { Text("e.g. 'Whenever I start studying, prepare my study environment'...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NexusSurface,
                    unfocusedContainerColor = NexusSurface,
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusBorder
                ),
                trailingIcon = {
                    Button(
                        onClick = onCompile,
                        colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("Compile", fontSize = 10.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                    }
                },
                singleLine = false,
                maxLines = 2
            )
        }

        if (compiled != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusCyan, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COMPILED AUTOMATION: ${compiled.name}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("1. TRIGGER: [${compiled.trigger.type}] ${compiled.trigger.parameter}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextPrimary)
                        Text("2. CONDITIONS: ${if (compiled.conditions.isEmpty()) "None" else compiled.conditions.joinToString { "${it.signal} ${it.operator} ${it.expectedValue}" }}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                        Text("3. ACTIONS (${compiled.actions.size} steps):", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextPrimary)
                        compiled.actions.forEach { act ->
                            Text("   • Step ${act.stepIndex}: ${act.capability} on ${act.target} (${act.riskLevel.name})", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                        }
                        Text("4. VERIFICATION: [${compiled.verification.checkType}] ${compiled.verification.expectedOutcome}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusGreen)
                        Text("5. FAILURE HANDLING: [${compiled.failureHandling.strategy}] ${compiled.failureHandling.fallbackMessage}", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusAmber)

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Approve & Activate Automation", fontSize = 10.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. TAB: NOTIFICATION INTELLIGENCE
// ==========================================

@Composable
fun NotificationsIntelligenceTab(
    notifications: List<com.example.nexus.core.world.ClassifiedNotification>,
    onExecuteAction: (com.example.nexus.core.world.ClassifiedNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "LOCAL NOTIFICATION INTELLIGENCE",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Classified locally into IMPORTANT, ACTION_REQUIRED, INFORMATIONAL, PROMOTIONAL, and NOISE.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (notifications.isEmpty()) {
            item {
                Text(
                    text = "No notifications ingested yet in local intelligence buffer.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(notifications) { notif ->
                val notifColor = when (notif.classification) {
                    NotificationPriorityClassification.ACTION_REQUIRED -> NexusRuby
                    NotificationPriorityClassification.IMPORTANT -> NexusAmber
                    NotificationPriorityClassification.INFORMATIONAL -> NexusCyan
                    NotificationPriorityClassification.PROMOTIONAL -> NexusTextSecondary
                    NotificationPriorityClassification.NOISE -> NexusTextSecondary.copy(alpha = 0.5f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, notifColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(notifColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = notif.classification.name,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = notifColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = notif.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = notif.content, fontSize = 10.sp, color = NexusTextSecondary)
                        }

                        if (notif.suggestedActionTitle != null) {
                            Button(
                                onClick = { onExecuteAction(notif) },
                                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(notif.suggestedActionTitle, fontSize = 9.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

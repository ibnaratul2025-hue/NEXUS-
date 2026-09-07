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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.cognitive.plan.CognitivePlanStep
import com.example.nexus.core.cognitive.plan.ExecutionPlan
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.presence.AttentionMode
import com.example.nexus.core.presence.ContextSnapshot
import com.example.nexus.core.presence.ContextualRecommendation
import com.example.nexus.core.presence.InteractionTone
import com.example.nexus.core.presence.JarvisCommandResult
import com.example.nexus.core.presence.LogicalAgentRole
import com.example.nexus.core.presence.PreparedTaskProposal
import com.example.nexus.core.presence.SecondBrainAnswer
import com.example.nexus.data.database.entity.ActionHistoryEntity
import com.example.nexus.data.database.entity.MissionEntity
import com.example.nexus.data.database.entity.PersonalModelEntity
import com.example.nexus.data.database.entity.PredictionEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisPresenceScreen(
    container: NexusAppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // State from container
    var attentionMode by remember { mutableStateOf(container.attentionManager.getAttentionMode()) }
    val workingMemoryState by container.workingMemory.state.collectAsState()
    val activeMission by container.missionEngine.getActiveMissionFlow().collectAsState(initial = null)
    val voiceSettings by container.jarvisVoiceController.settings.collectAsState()
    val isSpeaking by container.jarvisVoiceController.isSpeaking.collectAsState()
    val allPersonalEntries by container.personalModel.getAllEntries().collectAsState(initial = emptyList())
    val activePredictions by container.predictionRepository.getActivePredictions().collectAsState(initial = emptyList())
    val actionHistory by container.actionHistoryRepository.getAllActions().collectAsState(initial = emptyList())

    // Dynamic Context Snapshot
    var contextSnapshot by remember { mutableStateOf(container.personalContextEngine.captureSnapshot()) }
    var interactionTone by remember {
        mutableStateOf(container.emotionalToneAdapter.detectTone("operational status", workingMemoryState.recentFailures.size))
    }
    var recommendations by remember { mutableStateOf<List<ContextualRecommendation>>(emptyList()) }
    var preparedTasks by remember { mutableStateOf<List<PreparedTaskProposal>>(emptyList()) }

    // Command console state
    var commandInput by remember { mutableStateOf("") }
    var commandResult by remember { mutableStateOf<JarvisCommandResult?>(null) }
    var secondBrainQuery by remember { mutableStateOf("") }
    var secondBrainAnswer by remember { mutableStateOf<SecondBrainAnswer?>(null) }
    var isSearchingSecondBrain by remember { mutableStateOf(false) }

    // New Mission Dialog
    var showNewMissionDialog by remember { mutableStateOf(false) }
    var newMissionGoal by remember { mutableStateOf("") }

    // Refresh context periodically or on tab change
    LaunchedEffect(selectedTab, activeMission) {
        contextSnapshot = container.personalContextEngine.captureSnapshot()
        interactionTone = container.emotionalToneAdapter.detectTone("status", workingMemoryState.recentFailures.size)
        recommendations = container.whatShouldIDoEngine.generateRecommendations(
            contextSnapshot,
            workingMemoryState,
            activeMission
        )
        val samplePrep = container.taskPreparationEngine.prepareProjectOrganizationPlan(contextSnapshot.currentProject ?: "Workspace")
        preparedTasks = listOf(samplePrep)
    }

    // Glowing Pulse Animation for Active Presence
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(16.dp)
    ) {
        // --- 1. JARVIS PRESENCE TOP HERO BAR ---
        JarvisPresenceHeader(
            attentionMode = attentionMode,
            tone = interactionTone,
            contextSnapshot = contextSnapshot,
            isSpeaking = isSpeaking,
            glowAlpha = glowAlpha,
            onModeSelected = { mode ->
                container.attentionManager.setAttentionMode(mode)
                attentionMode = mode
            },
            onTestVoice = {
                container.jarvisVoiceController.speak(
                    "JARVIS presence operational. Attention mode: ${attentionMode.name}. Context battery level: ${contextSnapshot.batteryLevel} percent."
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. COMMAND INPUT CONSOLE ---
        JarvisCommandBar(
            prompt = commandInput,
            onPromptChange = { commandInput = it },
            onExecute = {
                if (commandInput.isNotBlank()) {
                    scope.launch {
                        val result = container.jarvisCommandEngine.tryHandleCommand(commandInput)
                        commandResult = result
                        when (result) {
                            is JarvisCommandResult.Handled -> {
                                container.jarvisVoiceController.speak(result.message)
                                commandInput = ""
                            }
                            is JarvisCommandResult.ClarificationNeeded -> {
                                container.jarvisVoiceController.speak(result.question)
                            }
                            is JarvisCommandResult.Refused -> {
                                container.jarvisVoiceController.speak(result.reason)
                            }
                            JarvisCommandResult.NotACommand -> {
                                Toast.makeText(context, "Executing via Agent Kernel", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onQuickCommand = { cmd ->
                commandInput = cmd
                scope.launch {
                    val result = container.jarvisCommandEngine.tryHandleCommand(cmd)
                    commandResult = result
                    if (result is JarvisCommandResult.Handled) {
                        container.jarvisVoiceController.speak(result.message)
                        commandInput = ""
                    }
                }
            }
        )

        // Command feedback display
        AnimatedVisibility(visible = commandResult != null) {
            commandResult?.let { res ->
                JarvisCommandFeedbackCard(result = res, onDismiss = { commandResult = null })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. TAB NAVIGATION ---
        val tabs = listOf(
            "Missions",
            "Recommendations (${recommendations.size})",
            "Predictions & Prep",
            "Second Brain",
            "Digital Twin (${allPersonalEntries.size})",
            "Undo Log (${actionHistory.size})",
            "Multi-Agent Roles"
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
                0 -> MissionsTab(
                    activeMission = activeMission,
                    onPause = { id -> scope.launch { container.missionEngine.pauseMission(id) } },
                    onResume = { id -> scope.launch { container.missionEngine.resumeMission(id) } },
                    onAdvance = { id -> scope.launch { container.missionEngine.advanceStep(id, "Manually verified step") } },
                    onRollback = { id -> scope.launch { container.missionEngine.rollbackMission(id) } },
                    onCancel = { id -> scope.launch { container.missionEngine.cancelMission(id) } },
                    onStartNewMission = { showNewMissionDialog = true }
                )
                1 -> RecommendationsTab(
                    recommendations = recommendations,
                    onExecute = { rec ->
                        scope.launch {
                            val res = container.jarvisCommandEngine.tryHandleCommand(rec.actionCommand)
                            commandResult = res
                            Toast.makeText(context, "Executing: ${rec.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> PredictionsAndPrepTab(
                    predictions = activePredictions,
                    preparedTasks = preparedTasks,
                    onPrepareSample = {
                        scope.launch {
                            val sample = PredictionEntity(
                                prediction = "Device battery will require power connection in ~90 minutes",
                                confidence = 0.88f,
                                evidence = "Context battery at ${contextSnapshot.batteryLevel}%, normal drain rate",
                                expiration = System.currentTimeMillis() + (90 * 60 * 1000L),
                                status = "PENDING",
                                category = "RESOURCE"
                            )
                            container.predictionRepository.savePrediction(sample)
                        }
                    }
                )
                3 -> SecondBrainTab(
                    query = secondBrainQuery,
                    onQueryChange = { secondBrainQuery = it },
                    answer = secondBrainAnswer,
                    isSearching = isSearchingSecondBrain,
                    onSearch = {
                        if (secondBrainQuery.isNotBlank()) {
                            scope.launch {
                                isSearchingSecondBrain = true
                                secondBrainAnswer = container.secondBrainEngine.query(secondBrainQuery)
                                isSearchingSecondBrain = false
                            }
                        }
                    }
                )
                4 -> DigitalTwinTab(
                    entries = allPersonalEntries,
                    onAddSample = {
                        scope.launch {
                            container.personalModel.addOrUpdateEntry(
                                category = "GOALS",
                                title = "Autonomous System Stability",
                                detailsJson = "{\"target\": \"100% verified tool execution\", \"scope\": \"Daily\"}",
                                priority = 1
                            )
                        }
                    },
                    onDelete = { id -> scope.launch { container.personalModel.deleteEntry(id) } },
                    onExport = {
                        scope.launch {
                            val json = container.personalModel.exportJson()
                            Toast.makeText(context, "Exported ${allPersonalEntries.size} entries", Toast.LENGTH_LONG).show()
                        }
                    },
                    onReset = {
                        scope.launch { container.personalModel.resetAll() }
                    }
                )
                5 -> UndoLogTab(
                    history = actionHistory,
                    onUndoLast = {
                        scope.launch {
                            val res = container.universalUndoManager.undoLastAction()
                            Toast.makeText(context, res.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                6 -> MultiAgentRolesTab(
                    container = container,
                    activeMission = activeMission
                )
            }
        }
    }

    // New Mission Dialog
    if (showNewMissionDialog) {
        NewMissionDialog(
            goal = newMissionGoal,
            onGoalChange = { newMissionGoal = it },
            onDismiss = { showNewMissionDialog = false },
            onConfirm = {
                if (newMissionGoal.isNotBlank()) {
                    scope.launch {
                        val plan = ExecutionPlan(
                            goal = newMissionGoal,
                            steps = listOf(
                                CognitivePlanStep(
                                    stepNumber = 1,
                                    toolId = "system.info",
                                    description = "Inspect system health before start",
                                    riskLevel = RiskLevel.LOW,
                                    expectedResult = "System metrics"
                                ),
                                CognitivePlanStep(
                                    stepNumber = 2,
                                    toolId = "file.list",
                                    description = "Enumerate relevant application files",
                                    riskLevel = RiskLevel.LOW,
                                    expectedResult = "File list"
                                ),
                                CognitivePlanStep(
                                    stepNumber = 3,
                                    toolId = "memory.save",
                                    description = "Store mission milestones in persistent memory",
                                    riskLevel = RiskLevel.MEDIUM,
                                    expectedResult = "Saved memory receipt"
                                )
                            )
                        )
                        container.missionEngine.startMission(
                            title = newMissionGoal.take(30),
                            goal = newMissionGoal,
                            plan = plan
                        )
                        newMissionGoal = ""
                        showNewMissionDialog = false
                    }
                }
            }
        )
    }
}

// ==========================================
// 1. PRESENCE HEADER & STATUS RIBBON
// ==========================================

@Composable
fun JarvisPresenceHeader(
    attentionMode: AttentionMode,
    tone: InteractionTone,
    contextSnapshot: ContextSnapshot,
    isSpeaking: Boolean,
    glowAlpha: Float,
    onModeSelected: (AttentionMode) -> Unit,
    onTestVoice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NexusBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) NexusAmber else NexusCyan)
                            .alpha(glowAlpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JARVIS AUTONOMOUS PRESENCE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusTextPrimary
                    )
                }

                // Tone badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NexusSurfaceElevated)
                        .border(1.dp, NexusCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tone.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Context Signals Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ContextBadge(
                    label = "BATT",
                    value = "${contextSnapshot.batteryLevel}%",
                    color = if (contextSnapshot.isLowBattery) NexusRuby else NexusGreen
                )
                ContextBadge(
                    label = "NET",
                    value = contextSnapshot.networkState,
                    color = if (contextSnapshot.networkState == "DISCONNECTED") NexusAmber else NexusCyan
                )
                ContextBadge(
                    label = "STORAGE",
                    value = "${contextSnapshot.availableStorageMb}MB",
                    color = if (contextSnapshot.isLowStorage) NexusRuby else NexusTextSecondary
                )
                ContextBadge(
                    label = "WINDOW",
                    value = "${contextSnapshot.hourOfDay}:00",
                    color = NexusCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attention Mode Selector Chips & Voice Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(AttentionMode.values()) { mode ->
                        val isSelected = mode == attentionMode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onModeSelected(mode) },
                            label = {
                                Text(
                                    text = mode.name,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NexusCyan,
                                selectedLabelColor = Color(0xFF00382F),
                                containerColor = NexusSurfaceElevated,
                                labelColor = NexusTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = NexusBorder
                            ),
                            modifier = Modifier.testTag("chip_mode_${mode.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onTestVoice,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NexusSurfaceElevated)
                        .testTag("btn_test_voice")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = if (isSpeaking) NexusAmber else NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContextBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = NexusTextSecondary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ==========================================
// 2. COMMAND INPUT CONSOLE
// ==========================================

@Composable
fun JarvisCommandBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onExecute: () -> Unit,
    onQuickCommand: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            placeholder = {
                Text(
                    "Speak or type command (e.g. 'Undo that', 'Continue', 'What's next?')...",
                    fontSize = 11.sp,
                    color = NexusTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_jarvis_command"),
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
                IconButton(
                    onClick = onExecute,
                    modifier = Modifier.testTag("btn_execute_jarvis_command")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute",
                        tint = NexusCyan
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onExecute() }),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Command Pills
        val pills = listOf("Undo that", "Continue", "Do it", "What happened?", "What's next?", "Why did you do that?")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(pills) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NexusSurfaceElevated)
                        .border(1.dp, NexusBorder, RoundedCornerShape(6.dp))
                        .clickable { onQuickCommand(cmd) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NexusCyan
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisCommandFeedbackCard(
    result: JarvisCommandResult,
    onDismiss: () -> Unit
) {
    val (title, message, color) = when (result) {
        is JarvisCommandResult.Handled -> Triple("EXECUTED", result.message, NexusGreen)
        is JarvisCommandResult.ClarificationNeeded -> Triple("CONFIRMATION NEEDED", result.question, NexusAmber)
        is JarvisCommandResult.Refused -> Triple("REFUSED", result.reason, NexusRuby)
        JarvisCommandResult.NotACommand -> Triple("NON-REACTIVE", "Input forwarded to kernel", NexusTextSecondary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
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
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = NexusTextPrimary
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss",
                    tint = NexusTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. TAB: AUTONOMOUS MISSIONS
// ==========================================

@Composable
fun MissionsTab(
    activeMission: MissionEntity?,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onRollback: (String) -> Unit,
    onCancel: (String) -> Unit,
    onStartNewMission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTONOMOUS MISSIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan
                )
                Button(
                    onClick = onStartNewMission,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_new_mission")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New",
                        tint = Color(0xFF00382F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Mission", fontSize = 11.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (activeMission == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Idle",
                            tint = NexusTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Active Mission Running",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NexusTextSecondary
                        )
                        Text(
                            text = "Launch a multi-step objective with checkpoints and reversible rollback.",
                            fontSize = 11.sp,
                            color = NexusTextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            item {
                ActiveMissionCard(
                    mission = activeMission,
                    onPause = { onPause(activeMission.id) },
                    onResume = { onResume(activeMission.id) },
                    onAdvance = { onAdvance(activeMission.id) },
                    onRollback = { onRollback(activeMission.id) },
                    onCancel = { onCancel(activeMission.id) }
                )
            }
        }
    }
}

@Composable
fun ActiveMissionCard(
    mission: MissionEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAdvance: () -> Unit,
    onRollback: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (mission.status) {
        "RUNNING" -> NexusGreen
        "PAUSED" -> NexusAmber
        "COMPLETED" -> NexusCyan
        else -> NexusRuby
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mission.title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusTextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mission.status,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mission.goal,
                fontSize = 11.sp,
                color = NexusTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NexusCyan,
                trackColor = NexusSurfaceElevated
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Step ${mission.currentStep} of ${mission.totalSteps}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NexusTextSecondary
                )
                Text(
                    text = "${(mission.progress * 100).toInt()}% Progress",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checkpoints snippet
            Text(
                text = "CHECKPOINT LOG:",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Text(
                text = mission.checkpointsJson,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = NexusTextPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mission.status == "RUNNING") {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 10.sp)
                    }
                } else if (mission.status == "PAUSED") {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 10.sp, color = Color(0xFF00382F))
                    }
                }

                Button(
                    onClick = onAdvance,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Advance", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Advance", fontSize = 10.sp, color = Color(0xFF00382F))
                }

                OutlinedButton(
                    onClick = onRollback,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rollback", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rollback", fontSize = 10.sp)
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NexusSurfaceElevated)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = NexusRuby, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// 4. TAB: RECOMMENDATIONS ("What Should I Do?")
// ==========================================

@Composable
fun RecommendationsTab(
    recommendations: List<ContextualRecommendation>,
    onExecute: (ContextualRecommendation) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "CONTEXTUAL ACTION PRIORITIES",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Synthesized from device metrics, current time, active missions, and working memory.",
                fontSize = 10.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (recommendations.isEmpty()) {
            item {
                Text(
                    text = "No pending action recommendations for current context.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(recommendations) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NexusSurfaceElevated)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "~${rec.estimatedMinutes} min",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = NexusAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = rec.description,
                            fontSize = 11.sp,
                            color = NexusTextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Rationale: ${rec.rationale}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NexusCyan.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onExecute(rec) },
                            colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Execute Option", fontSize = 11.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. TAB: PREDICTIONS & PREP
// ==========================================

@Composable
fun PredictionsAndPrepTab(
    predictions: List<PredictionEntity>,
    preparedTasks: List<PreparedTaskProposal>,
    onPrepareSample: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE PREDICTIONS (${predictions.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan
                )
                OutlinedButton(
                    onClick = onPrepareSample,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+ Predict Sample", fontSize = 10.sp)
                }
            }
        }

        if (predictions.isEmpty()) {
            item {
                Text(
                    text = "No predictions cached. NEXUS anticipates actions only when verified patterns exist.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(predictions) { pred ->
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
                                text = pred.prediction,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(pred.confidence * 100).toInt()}% confidence",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusGreen
                            )
                        }
                        Text(
                            text = "Evidence: ${pred.evidence}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NexusTextSecondary
                        )
                        Text(
                            text = "Category: ${pred.category}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NexusCyan.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TASK PREPARATION STAGING (${preparedTasks.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Tasks pre-computed in background. Awaits 1-tap confirmation before execution.",
                fontSize = 10.sp,
                color = NexusTextSecondary
            )
        }

        if (preparedTasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks currently staged in preparation buffer.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(preparedTasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurfaceElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = task.title,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusCyan
                            )
                            Text(
                                text = "${task.estimatedSteps} steps",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NexusAmber
                            )
                        }
                        Text(
                            text = task.summary,
                            fontSize = 10.sp,
                            color = NexusTextPrimary
                        )
                        Text(
                            text = "Highest Risk: ${task.highestRisk.name}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = if (task.highestRisk == RiskLevel.LOW) NexusGreen else NexusRuby
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. TAB: SECOND BRAIN
// ==========================================

@Composable
fun SecondBrainTab(
    query: String,
    onQueryChange: (String) -> Unit,
    answer: SecondBrainAnswer?,
    isSearching: Boolean,
    onSearch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "UNIFIED SECOND BRAIN QUERY LAYER",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Fuses local persistent memories, digital twin, knowledge graph, and learned skills.",
                fontSize = 10.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Query your second brain (e.g. 'project', 'routine', 'preferences')...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NexusSurface,
                    unfocusedContainerColor = NexusSurface,
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusBorder,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Query", tint = NexusCyan)
                    }
                },
                singleLine = true
            )
        }

        if (isSearching) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NexusCyan, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (answer != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NexusCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = NexusSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SYNTHESIZED RESULT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusCyan
                            )
                            Text(
                                text = "${(answer.confidence * 100).toInt()}% confidence",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NexusGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = answer.summary, fontSize = 11.sp, color = NexusTextPrimary)

                        if (answer.relevantMemories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("MEMORIES:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                            answer.relevantMemories.forEach { mem ->
                                Text("• $mem", fontSize = 10.sp, color = NexusTextSecondary)
                            }
                        }

                        if (answer.relevantPersonalModel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("DIGITAL TWIN RECORDS:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                            answer.relevantPersonalModel.forEach { rec ->
                                Text("• $rec", fontSize = 10.sp, color = NexusTextSecondary)
                            }
                        }

                        if (answer.relevantSkills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("SKILLS:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                            answer.relevantSkills.forEach { skill ->
                                Text("• $skill", fontSize = 10.sp, color = NexusTextSecondary)
                            }
                        }

                        if (answer.relevantKnowledgeNodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("KNOWLEDGE GRAPH NODES:", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = NexusTextSecondary)
                            answer.relevantKnowledgeNodes.forEach { node ->
                                Text("• $node", fontSize = 10.sp, color = NexusTextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. TAB: DIGITAL TWIN (PERSONAL MODEL)
// ==========================================

@Composable
fun DigitalTwinTab(
    entries: List<PersonalModelEntity>,
    onAddSample: () -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> Unit,
    onReset: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIGITAL TWIN MODEL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan
                    )
                    Text(
                        text = "100% transparent. User can edit, export, and reset anytime.",
                        fontSize = 9.sp,
                        color = NexusTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onExport, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "Export", tint = NexusCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onReset, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset", tint = NexusRuby, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onAddSample, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = NexusGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Text(
                    text = "No digital twin entries yet. Tap '+' to create your first goal or routine model.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(entries) { item ->
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
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NexusSurfaceElevated)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NexusCyan
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NexusTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.detailsJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NexusTextSecondary
                            )
                        }
                        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NexusRuby, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. TAB: UNIVERSAL UNDO LOG
// ==========================================

@Composable
fun UndoLogTab(
    history: List<ActionHistoryEntity>,
    onUndoLast: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNIVERSAL ACTION LOG & UNDO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan
                )
                Button(
                    onClick = onUndoLast,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusAmber),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_universal_undo")
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Undo", tint = Color(0xFF00382F), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo Last Action", fontSize = 10.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Every state-altering operation is logged with reverse instructions. Irreversible actions are flagged.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (history.isEmpty()) {
            item {
                Text(
                    text = "No actions recorded in current session history.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            items(history) { act ->
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
                                text = act.actionName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusCyan
                            )
                            Text(
                                text = if (act.isReversible) "REVERSIBLE" else "NOT REVERSIBLE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (act.isReversible) NexusGreen else NexusRuby
                            )
                        }
                        Text(text = act.description, fontSize = 10.sp, color = NexusTextPrimary)
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(act.executedAt)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = NexusTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. TAB: MULTI-AGENT INTERNAL ROLES
// ==========================================

@Composable
fun MultiAgentRolesTab(
    container: NexusAppContainer,
    activeMission: MissionEntity?
) {
    val samplePlan = ExecutionPlan(
        goal = activeMission?.goal ?: "General autonomous assistance & security verification",
        steps = listOf(
            CognitivePlanStep(
                stepNumber = 1,
                toolId = "system.info",
                description = "Telemetry query",
                riskLevel = RiskLevel.LOW,
                expectedResult = "System telemetry"
            ),
            CognitivePlanStep(
                stepNumber = 2,
                toolId = "file.list",
                description = "File directory scan",
                riskLevel = RiskLevel.LOW,
                expectedResult = "File listing"
            )
        )
    )

    val roleTasks = container.multiAgentCoordinator.coordinate(
        goal = samplePlan.goal,
        plan = samplePlan
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "MULTI-AGENT COORDINATION & VERIFIER",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Text(
                text = "Specialized internal logical roles. Verifier and SecurityAgent are authoritative.",
                fontSize = 9.sp,
                color = NexusTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(roleTasks) { task ->
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
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.role.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (task.role) {
                                LogicalAgentRole.VERIFIER -> NexusCyan
                                LogicalAgentRole.SECURITY_AGENT -> NexusAmber
                                LogicalAgentRole.PLANNER -> NexusGreen
                                else -> NexusTextPrimary
                            }
                        )
                        Text(text = task.description, fontSize = 10.sp, color = NexusTextSecondary)
                    }

                    Icon(
                        imageVector = if (task.verifierAuthoritativePass) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = "Status",
                        tint = if (task.verifierAuthoritativePass) NexusGreen else NexusRuby,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 10. DIALOG: NEW MISSION
// ==========================================

@Composable
fun NewMissionDialog(
    goal: String,
    onGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, NexusCyan, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NexusSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "START AUTONOMOUS MISSION",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NexusCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter the high-level objective. NEXUS will construct a verified plan with checkpoints and rollback protection.",
                fontSize = 11.sp,
                color = NexusTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = goal,
                onValueChange = onGoalChange,
                placeholder = { Text("e.g. 'Audit files, backup configurations, and summarize memory'", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NexusSurfaceElevated,
                    unfocusedContainerColor = NexusSurfaceElevated,
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusBorder
                ),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
                    Text("Cancel", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Launch Mission", fontSize = 11.sp, color = Color(0xFF00382F), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

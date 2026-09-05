package com.example.nexus.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.cognitive.capability.CapabilityState
import com.example.nexus.core.cognitive.capability.DeviceCapability
import com.example.nexus.core.cognitive.model.BenchmarkResult
import com.example.nexus.core.cognitive.skill.SkillModel
import com.example.nexus.core.cognitive.skill.SkillState
import com.example.nexus.core.cognitive.skill.SkillStep
import com.example.nexus.data.database.entity.KnowledgeNodeEntity
import com.example.nexus.data.database.entity.LearningRecordEntity
import com.example.nexus.data.database.entity.ProactiveSuggestionEntity
import com.example.nexus.data.database.entity.SkillEntity
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
fun CognitiveDashboardScreen(
    container: NexusAppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // State flows from repositories
    val skills by container.skillRepository.allSkills.collectAsState(initial = emptyList())
    val learningRecords by container.learningRepository.allRecords.collectAsState(initial = emptyList())
    val knowledgeNodes by container.knowledgeGraphRepository.allNodes.collectAsState(initial = emptyList())
    val suggestions by container.proactiveRepository.pendingSuggestions.collectAsState(initial = emptyList())
    val proactiveEnabled by container.proactiveEngine.isProactiveModeEnabled.collectAsState()

    var capabilities by remember { mutableStateOf<List<DeviceCapability>>(emptyList()) }
    var benchmarkResult by remember { mutableStateOf<BenchmarkResult?>(null) }
    var isBenchmarking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        capabilities = container.liveCapabilityRegistry.getCapabilities()
        if (skills.isEmpty()) {
            container.skillEngine.seedInitialSkills()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COGNITIVE EVOLUTION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NexusCyan
                )
                Text(
                    text = "Local-first • Truth-first • Verifiable",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (proactiveEnabled) "PROACTIVE" else "REACTIVE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (proactiveEnabled) NexusGreen else NexusTextSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Switch(
                    checked = proactiveEnabled,
                    onCheckedChange = { container.proactiveEngine.setProactiveMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NexusCyan,
                        checkedTrackColor = NexusSurfaceElevated,
                        uncheckedThumbColor = NexusTextSecondary,
                        uncheckedTrackColor = NexusSurface
                    ),
                    modifier = Modifier.testTag("switch_proactive_mode")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        val tabTitles = listOf(
            "State & Caps",
            "Skills (${skills.size})",
            "Learning (${learningRecords.size})",
            "Knowledge Graph",
            "Proactive (${suggestions.size})"
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
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NexusCyan else NexusTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> StateAndCapabilitiesTab(
                    capabilities = capabilities,
                    benchmarkResult = benchmarkResult,
                    isBenchmarking = isBenchmarking,
                    onRunBenchmark = {
                        scope.launch {
                            isBenchmarking = true
                            benchmarkResult = container.modelBenchmarkLab.runBenchmark()
                            isBenchmarking = false
                        }
                    },
                    container = container
                )
                1 -> SkillsTab(
                    skills = skills,
                    onTestSkill = { skill ->
                        scope.launch {
                            val res = container.skillEngine.testSkill(skill.id)
                            val msg = if (res.isPassed) "Skill test passed!" else "Failed: ${res.issues.joinToString()}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onApproveSkill = { skill ->
                        scope.launch {
                            container.skillEngine.approveAndActivateSkill(skill.id)
                            Toast.makeText(context, "Skill '${skill.name}' activated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCreateSampleSkill = {
                        scope.launch {
                            container.skillEngine.discoverSkill(
                                name = "Device Quick Audit",
                                description = "Checks system specs and internal storage safely",
                                triggerIntent = "COMMAND",
                                steps = listOf(SkillStep(1, "system.info", "Read device telemetry"))
                            )
                            Toast.makeText(context, "Skill draft created", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> LearningTab(
                    records = learningRecords,
                    onResetLearning = {
                        scope.launch {
                            container.learningRepository.clearAll()
                            Toast.makeText(context, "Learning records cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                3 -> KnowledgeGraphTab(
                    nodes = knowledgeNodes,
                    onPruneStale = {
                        scope.launch {
                            val count = container.cognitiveMemoryEngine.pruneStale(0.25f)
                            Toast.makeText(context, "Pruned $count stale memories", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                4 -> ProactiveTab(
                    suggestions = suggestions,
                    onScan = {
                        scope.launch {
                            val list = container.proactiveEngine.scanAndGenerateSuggestions()
                            Toast.makeText(context, "Generated ${list.size} suggestions", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onApprove = { s ->
                        scope.launch {
                            container.proactiveEngine.approveSuggestion(s.id)
                            Toast.makeText(context, "Approved: ${s.title}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { s ->
                        scope.launch {
                            container.proactiveEngine.dismissSuggestion(s.id)
                            Toast.makeText(context, "Dismissed: ${s.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StateAndCapabilitiesTab(
    capabilities: List<DeviceCapability>,
    benchmarkResult: BenchmarkResult?,
    isBenchmarking: Boolean,
    onRunBenchmark: () -> Unit,
    container: NexusAppContainer
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MODEL BENCHMARK LAB",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NexusCyan
                        )
                        Button(
                            onClick = onRunBenchmark,
                            enabled = !isBenchmarking,
                            colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_run_benchmark")
                        ) {
                            if (isBenchmarking) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = "Run Benchmark", modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Benchmark", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    if (benchmarkResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "First Token: ${benchmarkResult.firstTokenLatencyMs}ms | Throughput: ${String.format("%.1f", benchmarkResult.tokensPerSecond)} tok/s",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusGreen
                        )
                        Text(
                            text = "RAM Peak: ${benchmarkResult.peakMemoryMb}MB | JSON Reliability: ${(benchmarkResult.jsonToolReliabilityScore * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusTextSecondary
                        )
                        Text(
                            text = "Cancellation Response: ${benchmarkResult.cancellationResponseMs}ms",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusTextSecondary
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Profile: Q4_K_M • Llama-3/Gemma-2 • Context 4096 • Offline C++ llama.cpp",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusTextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "DEVICE CAPABILITIES & LIVE SENSORS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        items(capabilities) { cap ->
            val color = when (cap.state) {
                CapabilityState.AVAILABLE -> NexusGreen
                CapabilityState.REQUIRES_PERMISSION -> NexusAmber
                CapabilityState.REQUIRES_SERVICE -> NexusAmber
                CapabilityState.DEVICE_DEPENDENT -> NexusTextSecondary
                CapabilityState.UNAVAILABLE -> NexusRuby
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cap.name,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                        Text(
                            text = cap.description,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusTextSecondary
                        )
                    }
                    Text(
                        text = cap.state.name,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = color
                    )
                }
            }
        }

        item {
            Text(
                text = "INVARIANT BOUNDARIES & LIMITATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }

        items(container.limitationRegistry.getAllLimitations()) { lim ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NexusSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NexusRuby, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lim.summary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NexusTextPrimary
                        )
                    }
                    Text(
                        text = lim.detailedReason,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NexusTextSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SkillsTab(
    skills: List<SkillEntity>,
    onTestSkill: (SkillEntity) -> Unit,
    onApproveSkill: (SkillEntity) -> Unit,
    onCreateSampleSkill: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEARNED SKILLS REGISTRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan
            )
            Button(
                onClick = onCreateSampleSkill,
                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_create_skill")
            ) {
                Text("+ New Skill", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No skills registered yet.\nSkills are discovered from recurring successful tasks.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(skills) { skill ->
                    val state = try { SkillState.valueOf(skill.state) } catch (e: Exception) { SkillState.DRAFT }
                    val stateColor = when (state) {
                        SkillState.ACTIVE, SkillState.USER_APPROVED -> NexusGreen
                        SkillState.VERIFIED -> NexusCyan
                        SkillState.TESTING -> NexusAmber
                        SkillState.DRAFT, SkillState.DISCOVERED -> NexusTextSecondary
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NexusSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = skill.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NexusTextPrimary
                                )
                                Text(
                                    text = "[${state.name}]",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = stateColor
                                )
                            }
                            Text(
                                text = skill.description,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Tools: ${skill.requiredToolsJson}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusCyan
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onTestSkill(skill) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_test_skill_${skill.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp), tint = NexusCyan)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Sandbox", fontSize = 10.sp, color = NexusCyan, fontFamily = FontFamily.Monospace)
                                }

                                if (state != SkillState.ACTIVE && state != SkillState.USER_APPROVED) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onApproveSkill(skill) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("btn_approve_skill_${skill.id}")
                                    ) {
                                        Text("Approve & Activate", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningTab(
    records: List<LearningRecordEntity>,
    onResetLearning: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRUTH-FIRST LEARNING EVENTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan
            )
            IconButton(
                onClick = onResetLearning,
                modifier = Modifier.size(24.dp).testTag("btn_clear_learning")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = NexusRuby, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No learning records logged.\nNEXUS logs only verified real receipts and user corrections.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(records) { rec ->
                    val color = when (rec.eventType) {
                        "TOOL_RECEIPT" -> NexusGreen
                        "USER_CORRECTION" -> NexusCyan
                        "EXPLICIT_PREFERENCE" -> NexusAmber
                        "COMPLETED_WORKFLOW" -> NexusCyan
                        else -> NexusRuby
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NexusSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = rec.eventType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                                if (rec.failureClassification != null) {
                                    Text(
                                        text = "Class: ${rec.failureClassification}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = NexusRuby
                                    )
                                }
                            }
                            Text(
                                text = rec.sourceSummary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextPrimary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Text(
                                text = "Insight: ${rec.insight}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeGraphTab(
    nodes: List<KnowledgeNodeEntity>,
    onPruneStale: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KNOWLEDGE GRAPH (${nodes.size} NODES)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan
            )
            Button(
                onClick = onPruneStale,
                colors = ButtonDefaults.buttonColors(containerColor = NexusSurfaceElevated),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_prune_stale")
            ) {
                Text("Prune Stale (<0.25)", fontSize = 10.sp, color = NexusCyan, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (nodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Knowledge graph empty.\nNodes are indexed as entities, facts, and tools are used.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NexusTextSecondary
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(nodes) { node ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NexusSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${node.name} [${node.entityType}]",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NexusTextPrimary
                                )
                                Text(
                                    text = "Confidence: ${(node.confidence * 100).toInt()}% • Source: ${node.source}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
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

@Composable
fun ProactiveTab(
    suggestions: List<ProactiveSuggestionEntity>,
    onScan: () -> Unit,
    onApprove: (ProactiveSuggestionEntity) -> Unit,
    onDismiss: (ProactiveSuggestionEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROACTIVE INTELLIGENCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NexusCyan
            )
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_scan_proactive")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Patterns", fontSize = 11.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (suggestions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No pending proactive suggestions.\nTap 'Scan Patterns' to inspect background telemetry.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NexusTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { s ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NexusSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NexusTextPrimary
                                )
                                Text(
                                    text = s.type,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NexusCyan
                                )
                            }
                            Text(
                                text = s.description,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Action: ${s.proposedActionJson}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NexusTextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onDismiss(s) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_dismiss_${s.id}")
                                ) {
                                    Text("Dismiss", fontSize = 10.sp, color = NexusRuby, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onApprove(s) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NexusGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("btn_approve_${s.id}")
                                ) {
                                    Text("Approve & Run", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

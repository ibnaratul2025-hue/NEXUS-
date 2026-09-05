package com.example.nexus.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nexus.di.NexusAppContainer
import com.example.nexus.ui.navigation.NexusNavRoute
import com.example.nexus.ui.screens.AgentScreen
import com.example.nexus.ui.screens.CognitiveDashboardScreen
import com.example.nexus.ui.screens.DashboardScreen
import com.example.nexus.ui.screens.MemoryScreen
import com.example.nexus.ui.screens.ModelManagerScreen
import com.example.nexus.ui.screens.SettingsScreen
import com.example.nexus.ui.viewmodel.ModelManagerViewModel
import com.example.nexus.ui.viewmodel.NexusDashboardViewModel
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextSecondary

@Composable
fun NexusApp(
    container: NexusAppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val dashboardViewModel: NexusDashboardViewModel = viewModel(
        factory = NexusDashboardViewModel.Factory(
            container.modelManager,
            container.agentKernel,
            container.auditLogRepository,
            container.systemMetricsRepository,
            container.speechInput
        )
    )

    val modelManagerViewModel: ModelManagerViewModel = viewModel(
        factory = ModelManagerViewModel.Factory(
            container.modelManager,
            container.modelRepository,
            container.systemMetricsRepository
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NexusBackground,
        bottomBar = {
            NavigationBar(
                containerColor = NexusSurface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NexusNavRoute.items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00382F),
                            selectedTextColor = NexusCyan,
                            indicatorColor = NexusCyan,
                            unselectedIconColor = NexusTextSecondary,
                            unselectedTextColor = NexusTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NexusNavRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NexusNavRoute.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(NexusNavRoute.Cognitive.route) {
                CognitiveDashboardScreen(container = container)
            }
            composable(NexusNavRoute.Models.route) {
                ModelManagerScreen(viewModel = modelManagerViewModel)
            }
            composable(NexusNavRoute.Agent.route) {
                AgentScreen(
                    agentKernel = container.agentKernel,
                    auditLogRepository = container.auditLogRepository,
                    capabilityRegistry = container.capabilityRegistry
                )
            }
            composable(NexusNavRoute.Memory.route) {
                MemoryScreen(memoryRepository = container.memoryRepository)
            }
            composable(NexusNavRoute.Settings.route) {
                SettingsScreen(modelManager = container.modelManager)
            }
        }
    }
}

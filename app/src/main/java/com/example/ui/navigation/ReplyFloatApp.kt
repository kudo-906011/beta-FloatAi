package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainAssistantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedBg
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.ReplyFloatViewModel

@Composable
fun ReplyFloatApp(
    viewModel: ReplyFloatViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val simulationState by viewModel.simulationState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userNotice) {
        state.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(
                message = notice,
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkRedBg,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_navigation"),
                containerColor = DarkRedSurfaceElevated,
                tonalElevation = 6.dp
            ) {
                // Assistant Tab
                val isAssistantSelected = state.currentTab == NavigationTab.ASSISTANT
                NavigationBarItem(
                    selected = isAssistantSelected,
                    onClick = { viewModel.selectTab(NavigationTab.ASSISTANT) },
                    icon = {
                        Icon(
                            imageVector = if (isAssistantSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Assistant"
                        )
                    },
                    label = {
                        Text(
                            text = "Assistant",
                            fontWeight = if (isAssistantSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_assistant"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandRedLight,
                        selectedTextColor = BrandRedText,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandRedSurface
                    )
                )

                // History Tab
                val isHistorySelected = state.currentTab == NavigationTab.HISTORY
                NavigationBarItem(
                    selected = isHistorySelected,
                    onClick = { viewModel.selectTab(NavigationTab.HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (isHistorySelected) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = {
                        Text(
                            text = if (state.historyList.isNotEmpty()) "History (${state.historyList.size})" else "History",
                            fontWeight = if (isHistorySelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_history"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandRedLight,
                        selectedTextColor = BrandRedText,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandRedSurface
                    )
                )

                // Settings Tab
                val isSettingsSelected = state.currentTab == NavigationTab.SETTINGS
                NavigationBarItem(
                    selected = isSettingsSelected,
                    onClick = { viewModel.selectTab(NavigationTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandRedLight,
                        selectedTextColor = BrandRedText,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = BrandRedSurface
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = state.currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.ASSISTANT -> {
                        MainAssistantScreen(
                            state = state,
                            simulationState = simulationState,
                            onToggleOverlay = { viewModel.toggleOverlayService(context) },
                            onTriggerAnalyze = { viewModel.triggerAnalyzeScreen() },
                            onTogglePassThrough = { viewModel.togglePassThroughMode() },
                            onSetPassThrough = { viewModel.setPassThroughMode(it) },
                            onSelectScenario = { viewModel.selectSimulationScenario(it) },
                            onSimulationReplyCopy = { ctx, reply -> viewModel.copySimulationReply(ctx, reply) },
                            onSimulationViewAllToggle = { viewModel.toggleSimulationViewAll() },
                            onSimulationToneFilterSelect = { viewModel.selectSimulationToneFilter(it) },
                            onSimulationPassThroughToggle = { viewModel.toggleSimulationPassThrough() },
                            onSimulationScreenAnalysisToggle = { viewModel.toggleSimulationScreenAnalysis() },
                            onSimulationResponseModeSelect = { viewModel.selectSimulationResponseMode(it) },
                            onSimulationMinimizeClick = { viewModel.toggleSimulationMinimize() },
                            onSimulationExpandClick = { viewModel.expandSimulationBar() },
                            onSimulationCloseClick = { viewModel.toggleSimulationVisibility() },
                            onSimulationRestoreClick = { viewModel.toggleSimulationVisibility() },
                            onSimulationLanguageBarToggle = { viewModel.toggleSimulationLanguageBar() },
                            onSimulationCopyText = { ctx, text, label -> viewModel.copySimulationText(ctx, text, label) },
                            onClearNotice = { viewModel.clearNotice() }
                        )
                    }

                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            historyList = state.historyList,
                            onClearHistory = { viewModel.clearHistory() },
                            onDeleteEntry = { viewModel.deleteHistoryEntry(it) }
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        val configuredBots by viewModel.configuredBots.collectAsStateWithLifecycle()
                        val activeBotId by viewModel.activeBotId.collectAsStateWithLifecycle()

                        SettingsScreen(
                            settings = state.settings,
                            configuredBots = configuredBots,
                            activeBotId = activeBotId,
                            onSelectActiveBot = { botId -> viewModel.selectActiveBot(botId, context) },
                            onAddBot = { newBot -> viewModel.addBot(newBot, context) },
                            onDeleteBot = { botId -> viewModel.deleteBot(botId, context) },
                            onPurgeNow = { viewModel.purgeTemporaryData() },
                            onUpdateSettings = { transform -> viewModel.updateSettings(context, transform) },
                            onClearHistory = { viewModel.clearHistory() },
                            onRequestOverlayPermission = { viewModel.handleOverlayPermissionToggle(context) },
                            onRequestAccessibilityPermission = { viewModel.handleAccessibilityPermissionToggle(context) },
                            onRequestNotificationPermission = { viewModel.handleNotificationPermissionToggle(context) }
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalysisStatus
import com.example.model.PassThroughState
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.ResponseMode
import com.example.model.SampleMessageScenario
import com.example.ui.components.AnalyzeScreenControl
import com.example.ui.components.BulletNotificationIndicator
import com.example.ui.components.FloatingReplyBar
import com.example.ui.components.PassThroughControl
import com.example.ui.viewmodel.ReplyFloatUiState
import com.example.ui.viewmodel.SimulationState
import com.example.ui.viewmodel.sampleScenarios
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedBg
import com.example.ui.theme.DarkRedSurface
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceCard
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.DarkRedSurfaceHover
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun MainAssistantScreen(
    state: ReplyFloatUiState,
    simulationState: SimulationState,
    onToggleOverlay: () -> Unit,
    onTriggerAnalyze: () -> Unit,
    onTogglePassThrough: () -> Unit,
    onSetPassThrough: (PassThroughState) -> Unit,
    onSelectScenario: (SampleMessageScenario) -> Unit,
    onSimulationReplyCopy: (android.content.Context, ReplySuggestion) -> Unit,
    onSimulationViewAllToggle: () -> Unit,
    onSimulationToneFilterSelect: (ReplyTone?) -> Unit,
    onSimulationPassThroughToggle: () -> Unit,
    onSimulationScreenAnalysisToggle: () -> Unit,
    onSimulationResponseModeSelect: (ResponseMode) -> Unit,
    onSimulationMinimizeClick: () -> Unit,
    onSimulationExpandClick: () -> Unit,
    onSimulationCloseClick: () -> Unit,
    onSimulationRestoreClick: () -> Unit,
    onSimulationLanguageBarToggle: () -> Unit = {},
    onSimulationCopyText: (android.content.Context, String, String) -> Unit = { _, _, _ -> },
    onClearNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkRedBg)
            .verticalScroll(scrollState)
            .padding(horizontal = ReplyFloatDimens.space16, vertical = ReplyFloatDimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space16)
    ) {
        // App Branding Header with Real Overlay Status
        HeaderSection(
            isOverlayActive = state.isOverlayActive,
            assistantStatus = state.assistantStatus,
            replyCount = state.totalReplyCount,
            onToggleOverlay = onToggleOverlay
        )

        // Live Floating Assistant Interactive Preview Canvas (Strictly Simulation State)
        InteractiveFloatingPreviewSection(
            simulationState = simulationState,
            onReplyCopy = { onSimulationReplyCopy(context, it) },
            onViewAllToggle = onSimulationViewAllToggle,
            onToneFilterSelect = onSimulationToneFilterSelect,
            onPassThroughToggle = onSimulationPassThroughToggle,
            onScreenAnalysisToggle = onSimulationScreenAnalysisToggle,
            onResponseModeSelect = onSimulationResponseModeSelect,
            onLanguageBarToggle = onSimulationLanguageBarToggle,
            onCopyText = { text, label -> onSimulationCopyText(context, text, label) },
            onMinimizeClick = onSimulationMinimizeClick,
            onExpandClick = onSimulationExpandClick,
            onCloseClick = onSimulationCloseClick,
            onRestoreClick = onSimulationRestoreClick
        )

        // Analyze Screen Control Card (Production Trigger)
        AnalyzeScreenControl(
            status = state.analysisStatus,
            replyCount = state.totalReplyCount,
            onAnalyzeClick = onTriggerAnalyze
        )

        // Pass-Through Mode Control Card (Production)
        PassThroughControl(
            passThroughState = state.passThroughState,
            onToggle = onTogglePassThrough,
            onSetState = onSetPassThrough
        )

        // UI Test Scenarios (Preview / Simulation Only)
        ScenarioTestSection(
            currentDetectedMessage = simulationState.detectedMessage,
            onSelectScenario = onSelectScenario
        )

        Spacer(modifier = Modifier.height(ReplyFloatDimens.space24))
    }
}

@Composable
private fun HeaderSection(
    isOverlayActive: Boolean,
    assistantStatus: com.example.model.AssistantStatus,
    replyCount: Int,
    onToggleOverlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ReplyFloatDimens.standardCardWidthMax),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(containerColor = DarkRedSurfaceElevated),
        border = BorderStroke(1.dp, DarkRedSurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                        .background(BrandRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "ReplyFloat AI Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ReplyFloat AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = TextPrimary
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                            color = BrandRedSurface,
                            border = BorderStroke(1.dp, BrandRedBorder)
                        ) {
                            Text(
                                text = com.example.ai.AiBotManager.getActiveBot().name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRedText
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    BulletNotificationIndicator(
                        status = assistantStatus,
                        replyCount = replyCount,
                        showLabel = true
                    )
                }
            }

            // Master Overlay Switch (Dark Red branding)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = { onToggleOverlay() },
                    modifier = Modifier.testTag("master_overlay_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandRed,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkRedSurfaceCard
                    )
                )
                Text(
                    text = if (isOverlayActive) "ACTIVE" else "PAUSED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = if (isOverlayActive) StatusSuccess else TextMuted
                    )
                )
            }
        }
    }
}

@Composable
private fun InteractiveFloatingPreviewSection(
    simulationState: SimulationState,
    onReplyCopy: (com.example.model.ReplySuggestion) -> Unit,
    onViewAllToggle: () -> Unit,
    onToneFilterSelect: (ReplyTone?) -> Unit,
    onPassThroughToggle: () -> Unit,
    onScreenAnalysisToggle: () -> Unit,
    onResponseModeSelect: (ResponseMode) -> Unit,
    onLanguageBarToggle: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onMinimizeClick: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ReplyFloatDimens.standardCardWidthMax)
            .testTag("floating_preview_section"),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(
            containerColor = DarkRedSurfaceElevated
        ),
        border = BorderStroke(1.dp, DarkRedSurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Interactive Assistant Preview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Live preview of floating bar over simulated screen",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                if (!simulationState.isVisible) {
                    OutlinedButton(
                        onClick = onRestoreClick,
                        modifier = Modifier.testTag("restore_floating_bar_button"),
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        border = BorderStroke(1.dp, BrandRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = BrandRedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Bar", color = BrandRedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Simulated App Canvas with Overlay Floating Bar (Dark Red Canvas)
            Surface(
                shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
                color = DarkRedSurfaceCard,
                border = BorderStroke(1.dp, DarkRedSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ReplyFloatDimens.space12),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (simulationState.isVisible) {
                        FloatingReplyBar(
                            status = if (simulationState.replies.isEmpty()) com.example.model.AssistantStatus.IDLE else com.example.model.AssistantStatus.READY,
                            detectedMessage = simulationState.detectedMessage,
                            detectedSender = simulationState.detectedSender,
                            detectedSourceApp = simulationState.detectedSourceApp,
                            replies = simulationState.replies,
                            visibleReplies = simulationState.visibleReplies,
                            totalReplyCount = simulationState.totalReplyCount,
                            isExpanded = simulationState.isViewAllExpanded,
                            isMinimized = simulationState.isMinimized,
                            passThroughState = simulationState.passThroughState,
                            activeToneFilter = simulationState.activeToneFilter,
                            lastCopiedId = simulationState.lastCopiedId,
                            isScreenAnalysisOn = simulationState.isScreenAnalysisOn,
                            responseMode = simulationState.responseMode,
                            recentResults = emptyList(),
                            isLanguageBarActive = simulationState.isLanguageBarActive,
                            languageData = simulationState.languageData,
                            onReplyCopy = onReplyCopy,
                            onViewAllToggle = onViewAllToggle,
                            onToneFilterSelect = onToneFilterSelect,
                            onPassThroughToggle = onPassThroughToggle,
                            onScreenAnalysisToggle = onScreenAnalysisToggle,
                            onResponseModeSelect = onResponseModeSelect,
                            onDeleteRecentResult = {},
                            onLanguageBarToggle = onLanguageBarToggle,
                            onCopyText = onCopyText,
                            onMinimizeClick = onMinimizeClick,
                            onExpandClick = onExpandClick,
                            onCloseClick = onCloseClick
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Floating preview is currently dismissed",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            )
                            Button(
                                onClick = onRestoreClick,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                                shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium)
                            ) {
                                Text("Display Floating Bar", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioTestSection(
    currentDetectedMessage: String,
    onSelectScenario: (SampleMessageScenario) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ReplyFloatDimens.standardCardWidthMax)
            .testTag("scenario_test_section"),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(containerColor = DarkRedSurfaceElevated),
        border = BorderStroke(1.dp, DarkRedSurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space8)
        ) {
            Text(
                text = "Context Test Scenarios",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Test floating reply card layout under different text lengths and tones:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sampleScenarios.forEach { scenario ->
                    val isSelected = scenario.message == currentDetectedMessage
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge)),
                        onClick = { onSelectScenario(scenario) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scenario.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandRedText else TextPrimary
                                    )
                                )
                                Text(
                                    text = if (scenario.message.isNotBlank()) "\"${scenario.message}\"" else "(No incoming text)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) BrandRedLight else TextSecondary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                color = if (isSelected) BrandRed else DarkRedSurfaceHover
                            ) {
                                Text(
                                    text = "${scenario.suggestions.size} replies",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White else TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

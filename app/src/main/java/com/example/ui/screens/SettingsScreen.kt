package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiBotManager
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.model.ResponseMode
import com.example.model.AiLatencyMode
import com.example.model.AppSettings
import com.example.model.BotConfig
import com.example.model.BotProviderType
import com.example.model.DockPosition
import com.example.model.PurgeDuration
import com.example.model.ReplyTone
import com.example.model.UiColorPreset
import com.example.ui.theme.resolveDynamicTheme
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
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessBorder
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.StatusWarningBorder
import com.example.ui.theme.StatusWarningLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.UUID

@Composable
fun SettingsScreen(
    settings: AppSettings,
    configuredBots: List<BotConfig> = AiBotManager.configuredBots.value,
    activeBotId: String = settings.activeBotId,
    onSelectActiveBot: (String) -> Unit = {},
    onAddBot: (BotConfig) -> Unit = {},
    onDeleteBot: (String) -> Unit = {},
    onPurgeNow: () -> Unit = {},
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onClearHistory: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onRequestAccessibilityPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAddBotDialog by remember { mutableStateOf(false) }

    // Dialog: Add Custom Bot
    if (showAddBotDialog) {
        AddBotDialog(
            onDismiss = { showAddBotDialog = false },
            onAdd = { newBot ->
                onAddBot(newBot)
                showAddBotDialog = false
            }
        )
    }

    // Dialog: Clear History
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = DarkRedSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear All History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all archived reply records?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkRedBg)
            .verticalScroll(scrollState)
            .padding(horizontal = ReplyFloatDimens.space16, vertical = ReplyFloatDimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space16)
    ) {
        // Top Header
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                        .background(BrandRedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = BrandRedText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Preferences & AI Engine",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Multi-bot selection, auto-purge timer, latency & permissions",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // =========================================================================
        // SECTION: SCREEN ANALYSIS & REAL-TIME OBSERVER
        // =========================================================================
        SettingsSectionCard(
            title = "Screen Analysis (Battery & Resource Saver)",
            description = "Pause or enable real-time screen content extraction and question detection"
        ) {
            SettingToggleRow(
                title = if (settings.isScreenAnalysisOn) "Screen Analysis Active (Scanning)" else "Screen Analysis Paused",
                description = if (settings.isScreenAnalysisOn) "Accessibility service continuously checks for new chat questions in foreground apps." else "Accessibility parser is paused. No background scanning or AI triggers will occur.",
                isChecked = settings.isScreenAnalysisOn,
                onCheckedChange = { isChecked ->
                    onUpdateSettings { it.copy(isScreenAnalysisOn = isChecked) }
                }
            )
        }

        // =========================================================================
        // SECTION: RESPONSE MODE SELECTION
        // =========================================================================
        SettingsSectionCard(
            title = "Default Response Mode",
            description = "Select the default AI output format and persona style for suggestions"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResponseMode.values().forEach { mode ->
                    val isSelected = settings.responseMode == mode
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .clickable {
                                onUpdateSettings { it.copy(responseMode = mode) }
                            }
                            .testTag("response_mode_option_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BrandRedText else TextPrimary
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                        color = BrandRedSurface,
                                        border = BorderStroke(0.5.dp, BrandRedBorder)
                                    ) {
                                        Text(
                                            text = mode.badge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandRedText
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) BrandRedLight else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateSettings { it.copy(responseMode = mode) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandRed,
                                    unselectedColor = TextTertiary
                                )
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION: MULTI-BOT & PROVIDER MANAGER
        // =========================================================================
        SettingsSectionCard(
            title = "AI Bots & Providers",
            description = "Select the active AI provider or add additional custom bot profiles"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                configuredBots.forEach { bot ->
                    val isSelected = bot.id == activeBotId
                    val isAvailable = AiBotManager.isBotAvailable(bot)
                    val statusText = AiBotManager.getBotStatus(bot)

                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .clickable {
                                onSelectActiveBot(bot.id)
                            }
                            .testTag("bot_item_${bot.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectActiveBot(bot.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BrandRed,
                                        unselectedColor = TextTertiary
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = bot.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) BrandRedText else TextPrimary
                                            )
                                        )

                                        // Status Pill (Real availability indicator)
                                        Surface(
                                            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                            color = if (isAvailable) StatusSuccessBg else StatusWarningBg,
                                            border = BorderStroke(1.dp, if (isAvailable) StatusSuccessBorder else StatusWarningBorder)
                                        ) {
                                            Text(
                                                text = if (isAvailable) "Available" else "Disabled",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAvailable) StatusSuccess else StatusWarningLight
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelected) BrandRedLight else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )

                                    if (bot.systemPrompt.isNotBlank()) {
                                        Text(
                                            text = "“${bot.systemPrompt}”",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextTertiary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (bot.isCustom) {
                                IconButton(
                                    onClick = { onDeleteBot(bot.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Bot",
                                        tint = StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Custom Bot Button
                OutlinedButton(
                    onClick = { showAddBotDialog = true },
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    border = BorderStroke(1.dp, BrandRedBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("add_bot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = BrandRedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Custom AI Bot",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandRedText
                        )
                    )
                }
            }
        }

        // =========================================================================
        // SECTION: AUTO-PURGE TIMER SETTINGS
        // =========================================================================
        SettingsSectionCard(
            title = "Auto-Purge & Memory Management",
            description = "Automatically clear live temporary replies and analysis context after inactivity"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Purge Inactivity Timeout",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                // Selectable choices
                val purgeOptions = listOf(
                    PurgeDuration.TWO_MINUTES,
                    PurgeDuration.FIVE_MINUTES,
                    PurgeDuration.TEN_MINUTES,
                    PurgeDuration.FIFTEEN_MINUTES,
                    PurgeDuration.THIRTY_MINUTES,
                    PurgeDuration.ONE_HOUR,
                    PurgeDuration.NEVER,
                    PurgeDuration.CUSTOM
                )

                // 2-column or wrapping flow
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    purgeOptions.chunked(2).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { duration ->
                                val isSelected = settings.purgeDuration == duration
                                Surface(
                                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                                    color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                                        .clickable {
                                            onUpdateSettings { it.copy(purgeDuration = duration) }
                                        }
                                        .testTag("purge_option_${duration.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = duration.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) BrandRedText else TextPrimary
                                            )
                                        )

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onUpdateSettings { it.copy(purgeDuration = duration) } },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = BrandRed,
                                                unselectedColor = TextTertiary
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom minutes slider if CUSTOM selected
                if (settings.purgeDuration == PurgeDuration.CUSTOM) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkRedSurfaceCard, RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Purge Timer:",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Text(
                                text = "${settings.customPurgeMinutes} minutes",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BrandRedText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Slider(
                            value = settings.customPurgeMinutes.toFloat(),
                            onValueChange = { value ->
                                onUpdateSettings { it.copy(customPurgeMinutes = value.toInt()) }
                            },
                            valueRange = 1f..120f,
                            steps = 119,
                            colors = SliderDefaults.colors(
                                thumbColor = BrandRed,
                                activeTrackColor = BrandRed,
                                inactiveTrackColor = DarkRedSurfaceBorder
                            )
                        )
                    }
                }

                HorizontalDivider(color = DarkRedSurfaceBorder)

                // Purge Now Manual Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Purge Temporary Data Now",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Clears live detected text, active replies, and screen context immediately. Saved settings & bots remain safe.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onPurgeNow,
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        border = BorderStroke(1.dp, StatusWarningBorder),
                        modifier = Modifier.testTag("purge_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = StatusWarningLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Purge Now",
                            color = StatusWarningLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // SECTION: AI LATENCY CONTROL
        // =========================================================================
        SettingsSectionCard(
            title = "AI Latency & Response Behavior",
            description = "Adjust request timeout, debounce delay, and message detection timing"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiLatencyMode.values().forEach { mode ->
                    val isSelected = settings.latencyMode == mode
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .clickable {
                                onUpdateSettings { it.copy(latencyMode = mode) }
                            }
                            .testTag("latency_mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandRedText else TextPrimary
                                    )
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) BrandRedLight else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateSettings { it.copy(latencyMode = mode) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandRed,
                                    unselectedColor = TextTertiary
                                )
                            )
                        }
                    }
                }

                // Custom latency sliders if CUSTOM selected
                if (settings.latencyMode == AiLatencyMode.CUSTOM) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkRedSurfaceCard, RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Debounce delay slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Debounce Wait Delay:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = "${settings.customDebounceMs} ms",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandRedText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Slider(
                                value = settings.customDebounceMs.toFloat(),
                                onValueChange = { value ->
                                    onUpdateSettings { it.copy(customDebounceMs = value.toLong()) }
                                },
                                valueRange = 100f..2500f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = BrandRed,
                                    activeTrackColor = BrandRed,
                                    inactiveTrackColor = DarkRedSurfaceBorder
                                )
                            )
                        }

                        // Timeout slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Request Timeout:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                                Text(
                                    text = "${settings.customTimeoutSeconds} seconds",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandRedText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Slider(
                                value = settings.customTimeoutSeconds.toFloat(),
                                onValueChange = { value ->
                                    onUpdateSettings { it.copy(customTimeoutSeconds = value.toInt()) }
                                },
                                valueRange = 3f..30f,
                                steps = 26,
                                colors = SliderDefaults.colors(
                                    thumbColor = BrandRed,
                                    activeTrackColor = BrandRed,
                                    inactiveTrackColor = DarkRedSurfaceBorder
                                )
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION: SYSTEM PERMISSIONS
        // =========================================================================
        SettingsSectionCard(
            title = "System Permissions",
            description = "Android services enabling floating overlay & screen parsing"
        ) {
            PermissionStatusRow(
                title = "Draw Over Other Apps (Overlay)",
                description = "Required to float reply suggestions over chat applications",
                isGranted = settings.overlayPermissionGranted,
                onToggle = onRequestOverlayPermission
            )

            HorizontalDivider(color = DarkRedSurfaceBorder)

            PermissionStatusRow(
                title = "Accessibility Screen Reader",
                description = "Enables automatic message detection in active conversation windows",
                isGranted = settings.accessibilityPermissionGranted,
                onToggle = onRequestAccessibilityPermission
            )

            HorizontalDivider(color = DarkRedSurfaceBorder)

            PermissionStatusRow(
                title = "Bullet Notifications",
                description = "Alerts and foreground assistant controls in notification bar",
                isGranted = settings.notificationPermissionGranted,
                onToggle = onRequestNotificationPermission
            )
        }

        // =========================================================================
        // SECTION: UI COLOR & FLOATING BAR THEME CUSTOMIZER
        // =========================================================================
        SettingsSectionCard(
            title = "Floating UI Color & Appearance",
            description = "Customize the theme color, custom hex tone, and opacity of all floating bars"
        ) {
            val resolvedTheme = resolveDynamicTheme(
                preset = settings.uiColorPreset,
                customHex = settings.customUiColorHex,
                opacity = settings.overlayOpacity
            )
            var customHexInput by remember(settings.customUiColorHex) {
                mutableStateOf(settings.customUiColorHex)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color Presets Grid
                Text(
                    text = "Theme Presets",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                val presetList = listOf(
                    UiColorPreset.DARK_RED to "Dark Red (Default)",
                    UiColorPreset.RED to "Crimson Red",
                    UiColorPreset.BLUE to "Electric Blue",
                    UiColorPreset.PURPLE to "Royal Purple",
                    UiColorPreset.GREEN to "Emerald Green",
                    UiColorPreset.ORANGE to "Sunset Orange",
                    UiColorPreset.CYAN to "Vibrant Cyan",
                    UiColorPreset.PINK to "Neon Pink"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetList.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (preset, label) ->
                                val isSelected = settings.uiColorPreset == preset
                                val presetTheme = resolveDynamicTheme(preset, "#8B0000", 1.0f)
                                Surface(
                                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                                    color = if (isSelected) presetTheme.brandSurface else DarkRedSurfaceCard,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) presetTheme.brandPrimary else DarkRedSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                                        .clickable {
                                            onUpdateSettings { it.copy(uiColorPreset = preset) }
                                        }
                                        .testTag("ui_color_preset_${preset.name}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(presetTheme.brandPrimary)
                                                .then(
                                                    if (isSelected) Modifier.background(presetTheme.brandPrimary)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) presetTheme.brandText else TextPrimary,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Custom Color Hex Option Card
                val isCustomSelected = settings.uiColorPreset == UiColorPreset.CUSTOM
                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    color = if (isCustomSelected) resolvedTheme.brandSurface else DarkRedSurfaceCard,
                    border = BorderStroke(
                        if (isCustomSelected) 1.5.dp else 1.dp,
                        if (isCustomSelected) resolvedTheme.brandPrimary else DarkRedSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                        .clickable {
                            onUpdateSettings { it.copy(uiColorPreset = UiColorPreset.CUSTOM) }
                        }
                        .testTag("ui_color_preset_CUSTOM")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(resolvedTheme.brandPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCustomSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Custom Hex Tone",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCustomSelected) resolvedTheme.brandText else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Enter any 6-digit hex code or choose quick swatches",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            RadioButton(
                                selected = isCustomSelected,
                                onClick = { onUpdateSettings { it.copy(uiColorPreset = UiColorPreset.CUSTOM) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = resolvedTheme.brandPrimary,
                                    unselectedColor = TextTertiary
                                )
                            )
                        }

                        // Hex Input Row & Quick Palette
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customHexInput,
                                onValueChange = { input ->
                                    val sanitized = input.take(7)
                                    customHexInput = sanitized
                                    if (sanitized.startsWith("#") && (sanitized.length == 7 || sanitized.length == 9)) {
                                        onUpdateSettings { it.copy(uiColorPreset = UiColorPreset.CUSTOM, customUiColorHex = sanitized) }
                                    } else if (!sanitized.startsWith("#") && sanitized.length == 6) {
                                        val withHash = "#$sanitized"
                                        onUpdateSettings { it.copy(uiColorPreset = UiColorPreset.CUSTOM, customUiColorHex = withHash) }
                                    }
                                },
                                label = { Text("Color Hex (e.g. #FF5722)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_color_hex_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = resolvedTheme.brandPrimary,
                                    unfocusedBorderColor = DarkRedSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = resolvedTheme.brandPrimary
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                                    .background(resolvedTheme.brandPrimary)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Current Color",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick Palette Swatches
                        val quickSwatches = listOf(
                            "#FF1744", "#D500F9", "#2979FF", "#00E5FF",
                            "#00E676", "#FFEA00", "#FF6D00", "#795548"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            quickSwatches.forEach { hex ->
                                val swatchColor = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) {
                                    BrandRed
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .clickable {
                                            customHexInput = hex
                                            onUpdateSettings {
                                                it.copy(
                                                    uiColorPreset = UiColorPreset.CUSTOM,
                                                    customUiColorHex = hex
                                                )
                                            }
                                        }
                                        .then(
                                            if (settings.customUiColorHex.equals(hex, ignoreCase = true) && isCustomSelected) {
                                                Modifier.background(swatchColor)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (settings.customUiColorHex.equals(hex, ignoreCase = true) && isCustomSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkRedSurfaceBorder)

                // Overlay Opacity Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = null,
                                tint = resolvedTheme.brandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Floating Bar Opacity",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                            color = resolvedTheme.brandSurface,
                            border = BorderStroke(0.5.dp, resolvedTheme.brandBorder)
                        ) {
                            Text(
                                text = "${(settings.overlayOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = resolvedTheme.brandText
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Adjust background transparency of Bar 1 (Mini-Bubble), Bar 2 (Assistant Card), and Bar 3 (Translation Card)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )

                    Slider(
                        value = settings.overlayOpacity,
                        onValueChange = { newOpacity ->
                            onUpdateSettings { it.copy(overlayOpacity = newOpacity) }
                        },
                        valueRange = 0.40f..1.00f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = resolvedTheme.brandPrimary,
                            activeTrackColor = resolvedTheme.brandPrimary,
                            inactiveTrackColor = DarkRedSurfaceBorder
                        ),
                        modifier = Modifier.testTag("overlay_opacity_slider")
                    )

                    // Quick Opacity Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.50f to "50%", 0.75f to "75%", 0.90f to "90%", 1.00f to "100% (Solid)").forEach { (value, label) ->
                            val isOpSelected = (settings.overlayOpacity - value).let { kotlin.math.abs(it) < 0.04f }
                            Surface(
                                shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                color = if (isOpSelected) resolvedTheme.brandSurface else DarkRedSurfaceCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (isOpSelected) resolvedTheme.brandBorder else DarkRedSurfaceBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                                    .clickable {
                                        onUpdateSettings { it.copy(overlayOpacity = value) }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isOpSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isOpSelected) resolvedTheme.brandText else TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DarkRedSurfaceBorder)

                // Live Floating Bar Mini Preview
                Text(
                    text = "Live Theme Preview",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    color = resolvedTheme.surfaceElevated,
                    border = BorderStroke(1.dp, resolvedTheme.surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Preview Bar 1: Mini Bubble
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                color = resolvedTheme.brandPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "3 Replies",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Bar 1 • Floating Bubble",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        // Preview Bar 2: Card Header & Sample Reply Pill
                        Surface(
                            shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
                            color = resolvedTheme.surfaceCard,
                            border = BorderStroke(1.dp, resolvedTheme.surfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(resolvedTheme.brandPrimary)
                                    )
                                    Text(
                                        text = "Bar 2 • Assistant Header",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                                    color = resolvedTheme.brandSurface,
                                    border = BorderStroke(0.5.dp, resolvedTheme.brandBorder)
                                ) {
                                    Text(
                                        text = "Smart Reply",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = resolvedTheme.brandText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Reset Theme Button
                OutlinedButton(
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                uiColorPreset = UiColorPreset.DARK_RED,
                                customUiColorHex = "#8B0000",
                                overlayOpacity = 1.0f
                            )
                        }
                    },
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    border = BorderStroke(1.dp, DarkRedSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset Theme to Dark Red Default",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // =========================================================================
        // SECTION: FLOATING ASSISTANT BEHAVIOR
        // =========================================================================
        SettingsSectionCard(
            title = "Floating Assistant Behavior",
            description = "Control overlay positioning and touch responsiveness"
        ) {
            SettingToggleRow(
                title = "Auto-Minimize on Copy",
                description = "Collapse floating bar to a compact bubble immediately after copying a reply",
                isChecked = settings.autoMinimizeOnCopy,
                onCheckedChange = { checked ->
                    onUpdateSettings { it.copy(autoMinimizeOnCopy = checked) }
                }
            )

            HorizontalDivider(color = DarkRedSurfaceBorder)

            SettingToggleRow(
                title = "Pass-Through Touch by Default",
                description = "Allow background gestures unless floating bar is explicitly focused",
                isChecked = settings.passThroughDefault,
                onCheckedChange = { checked ->
                    onUpdateSettings { it.copy(passThroughDefault = checked) }
                }
            )

            HorizontalDivider(color = DarkRedSurfaceBorder)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Floating Dock Position",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                DockPosition.values().forEach { position ->
                    val isSelected = settings.dockPosition == position
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                            .clickable {
                                onUpdateSettings { it.copy(dockPosition = position) }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = position.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandRedText else TextPrimary
                                    )
                                )
                                Text(
                                    text = position.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) BrandRedLight else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateSettings { it.copy(dockPosition = position) } },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BrandRed,
                                    unselectedColor = TextTertiary
                                )
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION: AI REPLY PREFERENCES (Tone & Suggestion Count)
        // =========================================================================
        SettingsSectionCard(
            title = "AI Reply Preferences",
            description = "Customize tone presets and initial suggestions count"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Primary Tone Preset",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReplyTone.values().take(4).forEach { tone ->
                        val isSelected = settings.defaultTone == tone
                        val toneColor = Color(tone.badgeColorHex)
                        Surface(
                            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                            color = if (isSelected) toneColor.copy(alpha = 0.20f) else DarkRedSurfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) toneColor else DarkRedSurfaceBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                                .clickable {
                                    onUpdateSettings { it.copy(defaultTone = tone) }
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tone.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) toneColor else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkRedSurfaceBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Initial Suggestions Limit",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Expanded via 'View All' on floating bar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4, 5).forEach { count ->
                        val isSelected = settings.maxSuggestionsCount == count
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) BrandRed else DarkRedSurfaceCard,
                            border = BorderStroke(1.dp, if (isSelected) BrandRedBorder else DarkRedSurfaceBorder),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onUpdateSettings { it.copy(maxSuggestionsCount = count) }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION: DATA & PRIVACY
        // =========================================================================
        SettingsSectionCard(
            title = "Data & Privacy",
            description = "Local on-device storage management"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Clear Local History",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Remove all archived reply records from device storage",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    border = BorderStroke(1.dp, StatusErrorBorder)
                ) {
                    Text("Clear", color = StatusError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // About Footer
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "ReplyFloat AI • Version 1.1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            )
            Text(
                text = "Native Android Multi-Bot Floating Reply Engine",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            )
        }

        Spacer(modifier = Modifier.height(ReplyFloatDimens.space24))
    }
}

@Composable
private fun AddBotDialog(
    onDismiss: () -> Unit,
    onAdd: (BotConfig) -> Unit
) {
    var botName by remember { mutableStateOf("") }
    var selectedProviderType by remember { mutableStateOf(BotProviderType.GEMINI_FLASH) }
    var modelName by remember { mutableStateOf("gemini-2.5-flash") }
    var systemPrompt by remember { mutableStateOf("") }
    var timeoutSeconds by remember { mutableIntStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkRedSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Add Custom AI Bot",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure a custom bot profile. Future requests will route to this provider when selected.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                // Bot Name
                OutlinedTextField(
                    value = botName,
                    onValueChange = { botName = it },
                    label = { Text("Bot Name", color = TextSecondary) },
                    placeholder = { Text("e.g., Support Specialist", color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandRed,
                        unfocusedBorderColor = DarkRedSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider Type
                Text(
                    text = "Provider Architecture",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                BotProviderType.values().forEach { provider ->
                    val isSelected = selectedProviderType == provider
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
                        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
                        border = BorderStroke(1.dp, if (isSelected) BrandRedBorder else DarkRedSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedProviderType = provider
                                modelName = provider.defaultModel
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandRedText else TextPrimary
                                    )
                                )
                                Text(
                                    text = provider.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedProviderType = provider
                                    modelName = provider.defaultModel
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandRed)
                            )
                        }
                    }
                }

                // Model identifier
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Model Identifier", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandRed,
                        unfocusedBorderColor = DarkRedSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // System Prompt / Specialty
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Persona / Focus (Optional)", color = TextSecondary) },
                    placeholder = { Text("e.g. Always respond with concise executive summaries", color = TextTertiary) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandRed,
                        unfocusedBorderColor = DarkRedSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = botName.ifBlank { "Custom Bot ${System.currentTimeMillis() % 1000}" }
                    val newBot = BotConfig(
                        id = "bot_custom_${UUID.randomUUID()}",
                        name = finalName,
                        providerId = selectedProviderType.id,
                        modelName = modelName,
                        systemPrompt = systemPrompt,
                        timeoutSeconds = timeoutSeconds,
                        isEnabled = true,
                        isConfigured = true,
                        isCustom = true
                    )
                    onAdd(newBot)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) {
                Text("Save Bot", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space12)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

            content()
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                    color = if (isGranted) StatusSuccessBg else StatusWarningBg,
                    border = BorderStroke(1.dp, if (isGranted) StatusSuccessBorder else StatusWarningBorder)
                ) {
                    Text(
                        text = if (isGranted) "Enabled" else "Required",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) StatusSuccess else StatusWarningLight
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }

        Switch(
            checked = isGranted,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StatusSuccess,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkRedSurfaceCard
            )
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandRed,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkRedSurfaceCard
            )
        )
    }
}

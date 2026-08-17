package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantStatus
import com.example.model.PassThroughState
import com.example.model.RecentResultItem
import com.example.model.ReplySuggestion
import com.example.model.ReplyTone
import com.example.model.ResponseMode
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedSurface
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceCard
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.DarkRedSurfaceHover
import com.example.ui.theme.ReplyFloatDimens
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

@Composable
fun FloatingReplyBar(
    status: AssistantStatus,
    detectedMessage: String,
    detectedSender: String,
    detectedSourceApp: String,
    replies: List<ReplySuggestion>,
    visibleReplies: List<ReplySuggestion>,
    totalReplyCount: Int,
    isExpanded: Boolean,
    isMinimized: Boolean,
    passThroughState: PassThroughState,
    activeToneFilter: ReplyTone?,
    lastCopiedId: String?,
    isScreenAnalysisOn: Boolean = true,
    responseMode: ResponseMode = ResponseMode.PASSIVE,
    recentResults: List<RecentResultItem> = emptyList(),
    onReplyCopy: (ReplySuggestion) -> Unit,
    onViewAllToggle: () -> Unit,
    onToneFilterSelect: (ReplyTone?) -> Unit,
    onPassThroughToggle: () -> Unit,
    onScreenAnalysisToggle: () -> Unit = {},
    onResponseModeSelect: (ResponseMode) -> Unit = {},
    onDeleteRecentResult: (String) -> Unit = {},
    onMinimizeClick: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDrag: ((Float, Float) -> Unit)? = null,
    onResize: ((Float, Float) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .testTag("floating_reply_bar_container")
            .widthIn(max = ReplyFloatDimens.floatingBarMaxWidth)
    ) {
        if (isMinimized) {
            // Compact Floating Bubble / Pill State (Bar 1)
            CompactFloatingBubble(
                status = status,
                replyCount = totalReplyCount,
                passThroughState = passThroughState,
                isScreenAnalysisOn = isScreenAnalysisOn,
                responseMode = responseMode,
                onClick = onExpandClick,
                onClose = onCloseClick,
                onDrag = onDrag
            )
        } else {
            // Full Floating Assistant Card (Bar 2)
            ExpandedFloatingCard(
                status = status,
                detectedMessage = detectedMessage,
                detectedSender = detectedSender,
                detectedSourceApp = detectedSourceApp,
                replies = replies,
                visibleReplies = visibleReplies,
                totalReplyCount = totalReplyCount,
                isExpanded = isExpanded,
                passThroughState = passThroughState,
                activeToneFilter = activeToneFilter,
                lastCopiedId = lastCopiedId,
                isScreenAnalysisOn = isScreenAnalysisOn,
                responseMode = responseMode,
                recentResults = recentResults,
                onReplyCopy = onReplyCopy,
                onViewAllToggle = onViewAllToggle,
                onToneFilterSelect = onToneFilterSelect,
                onPassThroughToggle = onPassThroughToggle,
                onScreenAnalysisToggle = onScreenAnalysisToggle,
                onResponseModeSelect = onResponseModeSelect,
                onDeleteRecentResult = onDeleteRecentResult,
                onMinimizeClick = onMinimizeClick,
                onCloseClick = onCloseClick,
                onDrag = onDrag,
                onResize = onResize
            )
        }
    }
}

@Composable
private fun CompactFloatingBubble(
    status: AssistantStatus,
    replyCount: Int,
    passThroughState: PassThroughState,
    isScreenAnalysisOn: Boolean,
    responseMode: ResponseMode,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onDrag: ((Float, Float) -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .testTag("compact_floating_bubble")
            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
            .clickable(onClick = onClick)
            .shadow(
                elevation = ReplyFloatDimens.elevationFloating,
                shape = RoundedCornerShape(ReplyFloatDimens.radiusPill)
            ),
        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
        color = DarkRedSurfaceElevated,
        border = BorderStroke(1.5.dp, if (isScreenAnalysisOn) BrandRedBorder else DarkRedSurfaceBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Drag grip indicator
            if (onDrag != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DarkRedSurfaceHover)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .testTag("compact_drag_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to move bubble",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Glowing Sparkle Icon (Dark Red brand)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isScreenAnalysisOn) BrandRed else DarkRedSurfaceHover),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isScreenAnalysisOn) Color.White else TextTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ReplyFloat",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (replyCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                                .background(StatusSuccess)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$replyCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isScreenAnalysisOn) responseMode.badge else "ANALYSIS OFF",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isScreenAnalysisOn) BrandRedText else TextTertiary
                        )
                    )
                    if (passThroughState == PassThroughState.ENABLED) {
                        Text(
                            text = "• Pass-Thru",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                color = StatusWarningLight
                            )
                        )
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close floating bar",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedFloatingCard(
    status: AssistantStatus,
    detectedMessage: String,
    detectedSender: String,
    detectedSourceApp: String,
    replies: List<ReplySuggestion>,
    visibleReplies: List<ReplySuggestion>,
    totalReplyCount: Int,
    isExpanded: Boolean,
    passThroughState: PassThroughState,
    activeToneFilter: ReplyTone?,
    lastCopiedId: String?,
    isScreenAnalysisOn: Boolean,
    responseMode: ResponseMode,
    recentResults: List<RecentResultItem>,
    onReplyCopy: (ReplySuggestion) -> Unit,
    onViewAllToggle: () -> Unit,
    onToneFilterSelect: (ReplyTone?) -> Unit,
    onPassThroughToggle: () -> Unit,
    onScreenAnalysisToggle: () -> Unit,
    onResponseModeSelect: (ResponseMode) -> Unit,
    onDeleteRecentResult: (String) -> Unit,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
    onDrag: ((Float, Float) -> Unit)? = null,
    onResize: ((Float, Float) -> Unit)? = null
) {
    var showRecentHistory by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("floating_reply_card")
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(ReplyFloatDimens.radius3XLarge)
            ),
        shape = RoundedCornerShape(ReplyFloatDimens.radius3XLarge),
        color = DarkRedSurfaceElevated,
        border = BorderStroke(1.dp, DarkRedSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space14)
        ) {
            // Dedicated Top Drag Handle Pill
            if (onDrag != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .testTag("expanded_drag_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                            .background(DarkRedSurfaceBorder)
                    )
                }
            }

            // Header: Title, Analysis Switch, Window Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusSmall))
                            .background(if (isScreenAnalysisOn) BrandRed else DarkRedSurfaceHover),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isScreenAnalysisOn) Color.White else TextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ReplyFloat AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp,
                                color = TextPrimary
                            )
                        )
                    }

                    BulletNotificationIndicator(
                        status = status,
                        replyCount = totalReplyCount,
                        showLabel = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Screen Analysis ON/OFF Fast Toggle Button
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        color = if (isScreenAnalysisOn) BrandRedSurface else DarkRedSurfaceHover,
                        border = BorderStroke(1.dp, if (isScreenAnalysisOn) BrandRedBorder else DarkRedSurfaceBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                            .clickable(onClick = onScreenAnalysisToggle)
                            .testTag("floating_bar_screen_analysis_toggle")
                            .semantics {
                                contentDescription = if (isScreenAnalysisOn) "Screen analysis ON. Tap to pause." else "Screen analysis OFF. Tap to enable."
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isScreenAnalysisOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (isScreenAnalysisOn) BrandRedText else TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isScreenAnalysisOn) "SCAN ON" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = if (isScreenAnalysisOn) BrandRedText else TextTertiary
                                )
                            )
                        }
                    }

                    // Minimize button (switches to Bar 1)
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onMinimizeClick)
                            .testTag("minimize_floating_bar_button")
                            .semantics { contentDescription = "Minimize floating bar" },
                        shape = CircleShape,
                        color = DarkRedSurfaceHover
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Close button
                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCloseClick)
                            .testTag("close_floating_bar_button")
                            .semantics { contentDescription = "Close floating bar" },
                        shape = CircleShape,
                        color = DarkRedSurfaceHover
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReplyFloatDimens.space10))

            // Response Mode Switcher (Horizontal Scroll of 8 modes: 1-Line, 2-Line, Word, Debate, Funny, Arrogant, Lord, Passive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                ResponseMode.values().forEach { mode ->
                    val isSelected = responseMode == mode
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        color = if (isSelected) BrandRed else DarkRedSurfaceCard,
                        border = BorderStroke(1.dp, if (isSelected) BrandRedBorder else DarkRedSurfaceBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                            .clickable { onResponseModeSelect(mode) }
                            .testTag("mode_pill_${mode.name}")
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else TextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReplyFloatDimens.space10))

            // Detected Context / Message Preview Banner (Dark-Red Surface)
            if (detectedMessage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    color = DarkRedSurfaceCard,
                    border = BorderStroke(1.dp, DarkRedSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE QUESTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.6.sp,
                                    color = BrandRedText
                                )
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = TextTertiary
                                )
                            )
                            Text(
                                text = "$detectedSourceApp ($detectedSender)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            )
                        }

                        Text(
                            text = "\"$detectedMessage\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Italic,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(ReplyFloatDimens.space10))
            }

            // Tone Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tone:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                TonePill(
                    label = "All",
                    isSelected = activeToneFilter == null,
                    color = BrandRed,
                    onClick = { onToneFilterSelect(null) }
                )

                ReplyTone.values().forEach { tone ->
                    TonePill(
                        label = tone.label,
                        isSelected = activeToneFilter == tone,
                        color = Color(tone.badgeColorHex),
                        onClick = { onToneFilterSelect(tone) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReplyFloatDimens.space10))

            // Suggestions List with Controlled Max Height
            if (visibleReplies.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                    color = DarkRedSurfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (!isScreenAnalysisOn) "Screen Analysis is Paused" else "No replies ready for active question",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        )
                        Text(
                            text = if (!isScreenAnalysisOn) "Tap 'SCAN ON' to observe incoming questions" else "Tap 'All' tone or wait for incoming message",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (isExpanded) 280.dp else 180.dp),
                    verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space8),
                    contentPadding = PaddingValues(bottom = 2.dp)
                ) {
                    items(
                        items = visibleReplies,
                        key = { it.id }
                    ) { suggestion ->
                        ReplySuggestionItem(
                            suggestion = suggestion,
                            isCopied = lastCopiedId == suggestion.id,
                            onCopyClick = { onReplyCopy(suggestion) }
                        )
                    }
                }
            }

            // Accordion for Previous Answers (Recent Results with 2m retention timer)
            if (recentResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ReplyFloatDimens.space8))
                Surface(
                    shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
                    color = DarkRedSurfaceCard,
                    border = BorderStroke(0.5.dp, DarkRedSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                        .clickable { showRecentHistory = !showRecentHistory }
                        .testTag("recent_results_accordion_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = BrandRedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Previous Answers (${recentResults.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            )
                        }
                        Icon(
                            imageVector = if (showRecentHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showRecentHistory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentResults.forEach { item ->
                            RecentResultHistoryCard(
                                item = item,
                                onCopyReply = { replyText, tone ->
                                    onReplyCopy(
                                        ReplySuggestion(
                                            id = "recent_${item.id}",
                                            text = replyText,
                                            tone = tone
                                        )
                                    )
                                },
                                onDelete = { onDeleteRecentResult(item.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReplyFloatDimens.space8))
            HorizontalDivider(
                color = DarkRedSurfaceBorder,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(ReplyFloatDimens.space8))

            // Action Footer: View All / Show Less + Pass-Through Quick Toggle + Corner Resize Grip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View All / Show Less Reversible Button (displays real count)
                if (totalReplyCount > 2) {
                    TextButton(
                        onClick = onViewAllToggle,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("view_all_replies_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = BrandRedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "SHOW LESS" else "VIEW ALL ($totalReplyCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = BrandRedText
                            )
                        )
                    }
                } else {
                    Text(
                        text = "$totalReplyCount ${if (totalReplyCount == 1) "reply" else "replies"} ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pass-Through Quick Status Indicator / Toggle (Dark-Red Surface)
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        color = if (passThroughState == PassThroughState.ENABLED) StatusWarningBg else DarkRedSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (passThroughState == PassThroughState.ENABLED) StatusWarningBorder else DarkRedSurfaceBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                            .clickable(onClick = onPassThroughToggle)
                            .testTag("floating_bar_pass_through_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (passThroughState == PassThroughState.ENABLED) Icons.Default.LayersClear else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (passThroughState == PassThroughState.ENABLED) StatusWarningLight else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (passThroughState == PassThroughState.ENABLED) "PASS-THRU ON" else "TOUCH ON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.4.sp,
                                    color = if (passThroughState == PassThroughState.ENABLED) StatusWarningLight else TextSecondary
                                )
                            )
                        }
                    }

                    // Window Resize Handle
                    if (onResize != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkRedSurfaceHover)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onResize(dragAmount.x, dragAmount.y)
                                    }
                                }
                                .testTag("window_resize_handle")
                                .semantics { contentDescription = "Drag corner to resize overlay window" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentResultHistoryCard(
    item: RecentResultItem,
    onCopyReply: (String, ReplyTone) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
        color = DarkRedSurfaceHover,
        border = BorderStroke(0.5.dp, DarkRedSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q: \"${item.question}\"",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete recent item",
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            item.suggestions.take(2).forEach { reply ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${reply.text}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onCopyReply(reply.text, reply.tone) },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy answer",
                            tint = BrandRedText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TonePill(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
        color = if (isSelected) BrandRedSurface else DarkRedSurfaceCard,
        border = BorderStroke(
            1.dp,
            if (isSelected) BrandRedBorder else DarkRedSurfaceBorder
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp,
                color = if (isSelected) BrandRedText else TextSecondary
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

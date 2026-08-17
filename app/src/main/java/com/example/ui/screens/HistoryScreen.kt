package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistoryEntry
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
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<HistoryEntry>,
    onClearHistory: () -> Unit,
    onDeleteEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var copiedEntryId by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(text: String, id: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ReplyFloat AI History", text)
        clipboard.setPrimaryClip(clip)
        copiedEntryId = id
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = DarkRedSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear Reply History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all saved reply suggestions and context records from the on-device database.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkRedBg)
            .padding(horizontal = ReplyFloatDimens.space16, vertical = ReplyFloatDimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header with item count & Clear Action
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                            .background(BrandRedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = BrandRedText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Reply History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${historyList.size} ${if (historyList.size == 1) "entry" else "entries"} recorded",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_history_button"),
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        border = BorderStroke(1.dp, StatusErrorBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(ReplyFloatDimens.space12))

        // History Content (Empty State or List)
        if (historyList.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ReplyFloatDimens.standardCardWidthMax)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space12),
                contentPadding = PaddingValues(bottom = ReplyFloatDimens.space24)
            ) {
                items(
                    items = historyList,
                    key = { it.id }
                ) { entry ->
                    HistoryItemCard(
                        entry = entry,
                        isCopied = copiedEntryId == entry.id,
                        onCopy = { copyToClipboard(entry.selectedReply, entry.id) },
                        onDelete = { onDeleteEntry(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntry,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(entry.timestamp) { dateFormatter.format(Date(entry.timestamp)) }
    val toneColor = Color(entry.tone.badgeColorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${entry.id}"),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(containerColor = DarkRedSurfaceElevated),
        border = BorderStroke(1.dp, DarkRedSurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space10)
        ) {
            // Header: Tone, Source App, Timestamp, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tone Badge
                    Surface(
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                        color = toneColor.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, toneColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = entry.tone.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = toneColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${entry.sourceApp}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete entry",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Original Detected Message Context
            Surface(
                shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
                color = DarkRedSurfaceCard,
                border = BorderStroke(1.dp, DarkRedSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "ORIGINAL MESSAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "\"${entry.originalMessage}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            // Copied / Used AI Reply
            Surface(
                shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                color = BrandRedSurface,
                border = BorderStroke(1.dp, BrandRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.selectedReply,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .semantics { contentDescription = "Copy reply to clipboard" }
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isCopied) StatusSuccess else BrandRedText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
                    .background(DarkRedSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "No Reply History Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Text(
                text = "Whenever you copy an AI-suggested reply from the floating bar, it will be automatically archived here for quick reference.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    lineHeight = 19.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

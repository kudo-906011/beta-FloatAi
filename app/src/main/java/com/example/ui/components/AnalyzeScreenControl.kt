package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalysisStatus
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceCard
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.DarkRedSurfaceHover
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessBorder
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AnalyzeScreenControl(
    status: AnalysisStatus,
    replyCount: Int,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnalyzing = status == AnalysisStatus.ANALYZING

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analyze_screen_control_card"),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(
            containerColor = DarkRedSurfaceElevated
        ),
        border = BorderStroke(
            1.dp,
            when (status) {
                AnalysisStatus.ANALYZING -> BrandRed
                AnalysisStatus.COMPLETED -> StatusSuccessBorder
                AnalysisStatus.ERROR -> StatusErrorBorder
                else -> DarkRedSurfaceBorder
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space12)
        ) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.FindInPage,
                            contentDescription = null,
                            tint = BrandRedText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Analyze Active Screen",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Smart conversation parsing & reply generation",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Current State Visual Feedback Pill (Dark Red Theme)
            Surface(
                shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                color = when (status) {
                    AnalysisStatus.COMPLETED -> StatusSuccessBg
                    AnalysisStatus.ANALYZING -> BrandRedSurface
                    AnalysisStatus.ERROR -> StatusErrorBg
                    else -> DarkRedSurfaceCard
                },
                border = BorderStroke(
                    1.dp,
                    when (status) {
                        AnalysisStatus.COMPLETED -> StatusSuccessBorder
                        AnalysisStatus.ANALYZING -> BrandRedBorder
                        AnalysisStatus.ERROR -> StatusErrorBorder
                        else -> DarkRedSurfaceBorder
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (status) {
                        AnalysisStatus.READY -> {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandRedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Ready to Scan",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Tap 'Trigger Screen Analysis' to scan current messages",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        AnalysisStatus.ANALYZING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BrandRedText
                            )
                            Column {
                                Text(
                                    text = "Analyzing View Context...",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandRedText
                                    )
                                )
                                Text(
                                    text = "Extracting messages & generating tone variants",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        AnalysisStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Analysis Ready ($replyCount suggestions)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess
                                    )
                                )
                                Text(
                                    text = "Floating reply bar updated with latest options",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        AnalysisStatus.NO_CONTENT -> {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = StatusWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "No New Message Detected",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Ensure a chat or email app is currently open",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }

                        AnalysisStatus.ERROR -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Analysis Error",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusError
                                    )
                                )
                                Text(
                                    text = "Check accessibility permission in Settings",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Action Button (Dark Red brand)
            Button(
                onClick = onAnalyzeClick,
                enabled = !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("trigger_analyze_screen_button")
                    .semantics { contentDescription = "Trigger analyze screen" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRed,
                    contentColor = Color.White,
                    disabledContainerColor = DarkRedSurfaceCard,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning Active Screen...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = if (status == AnalysisStatus.COMPLETED) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status == AnalysisStatus.COMPLETED) "Re-Analyze Screen" else "Trigger Screen Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

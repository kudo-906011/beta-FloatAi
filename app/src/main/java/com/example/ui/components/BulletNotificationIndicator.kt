package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantStatus
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorBorder
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary

@Composable
fun BulletNotificationIndicator(
    status: AssistantStatus,
    replyCount: Int = 0,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bullet_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val bulletColor by animateColorAsState(
        targetValue = when (status) {
            AssistantStatus.IDLE -> TextMuted
            AssistantStatus.DETECTING -> BrandRedLight
            AssistantStatus.ANALYZING -> BrandRed
            AssistantStatus.READY -> StatusSuccess
            AssistantStatus.ERROR -> StatusError
        },
        label = "bullet_color"
    )

    val isPulsing = status == AssistantStatus.ANALYZING || status == AssistantStatus.DETECTING

    val rootModifier = modifier
        .testTag("bullet_notification_indicator")
        .semantics {
            contentDescription = "Status: ${status.label}${if (status == AssistantStatus.READY) ", $replyCount replies available" else ""}"
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    if (showLabel) {
        val (bgColor, textColor, borderColor) = when (status) {
            AssistantStatus.READY -> Triple(StatusSuccessBg, StatusSuccess, StatusSuccessBorder)
            AssistantStatus.ANALYZING, AssistantStatus.DETECTING -> Triple(BrandRedSurface, BrandRedText, BrandRedBorder)
            AssistantStatus.ERROR -> Triple(StatusErrorBg, StatusError, StatusErrorBorder)
            AssistantStatus.IDLE -> Triple(DarkRedSurfaceElevated, TextTertiary, DarkRedSurfaceBorder)
        }

        Surface(
            modifier = rootModifier,
            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
            color = bgColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(10.dp)
                ) {
                    // Pulse halo
                    if (isPulsing) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(bulletColor.copy(alpha = 0.35f))
                        )
                    }
                    // Core dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(bulletColor)
                    )
                }

                Text(
                    text = if (status == AssistantStatus.READY && replyCount > 0) {
                        "$replyCount REPLIES READY"
                    } else {
                        status.label.uppercase()
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = textColor
                )
            }
        }
    } else {
        // Compact pure bullet dot / badge
        Box(
            modifier = rootModifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPulsing) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(bulletColor.copy(alpha = 0.35f))
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(bulletColor)
            )
        }
    }
}

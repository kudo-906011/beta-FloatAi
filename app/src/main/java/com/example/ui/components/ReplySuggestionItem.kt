package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReplySuggestion
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedSurface
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ReplySuggestionItem(
    suggestion: ReplySuggestion,
    isCopied: Boolean,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toneColor = Color(suggestion.tone.badgeColorHex)

    val containerBg = if (isCopied) StatusSuccessBg else DarkRedSurfaceElevated
    val borderColor = if (isCopied) StatusSuccessBorder else DarkRedSurfaceBorder
    val textColor = TextPrimary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reply_suggestion_item_${suggestion.id}")
            .semantics {
                contentDescription = "${suggestion.tone.label} reply: ${suggestion.text}"
            }
            .clip(RoundedCornerShape(ReplyFloatDimens.radiusLarge))
            .clickable(onClick = onCopyClick),
        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
        color = containerBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tone Badge & Mode Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(ReplyFloatDimens.radiusPill))
                            .background(toneColor.copy(alpha = 0.20f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(toneColor)
                        )
                        Text(
                            text = suggestion.tone.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = toneColor
                        )
                    }

                    if (suggestion.mode != null) {
                        Surface(
                            shape = RoundedCornerShape(ReplyFloatDimens.radiusPill),
                            color = BrandRedSurface,
                            border = BorderStroke(0.5.dp, BrandRedBorder)
                        ) {
                            Text(
                                text = suggestion.mode.badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = BrandRedText
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Reply Text Content
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sleek Action Copy Box (Dark Red accent)
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(ReplyFloatDimens.radiusMedium))
                    .shadow(
                        elevation = if (isCopied) 0.dp else 1.dp,
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium)
                    ),
                shape = RoundedCornerShape(ReplyFloatDimens.radiusMedium),
                color = if (isCopied) StatusSuccess else BrandRedSurface,
                border = BorderStroke(1.dp, if (isCopied) StatusSuccessBorder else BrandRedBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_reply_button_${suggestion.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (isCopied) "Copied" else "Copy reply",
                        tint = if (isCopied) Color.White else BrandRedText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

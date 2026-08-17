package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PassThroughState
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBorder
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.BrandRedLight
import com.example.ui.theme.BrandRedSurface
import com.example.ui.theme.BrandRedText
import com.example.ui.theme.DarkRedSurfaceBorder
import com.example.ui.theme.DarkRedSurfaceCard
import com.example.ui.theme.DarkRedSurfaceElevated
import com.example.ui.theme.ReplyFloatDimens
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.StatusWarningBorder
import com.example.ui.theme.StatusWarningLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PassThroughControl(
    passThroughState: PassThroughState,
    onToggle: () -> Unit,
    onSetState: (PassThroughState) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = passThroughState == PassThroughState.ENABLED

    val cardBorderColor by animateColorAsState(
        targetValue = if (isEnabled) StatusWarningBorder else DarkRedSurfaceBorder,
        label = "pass_through_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pass_through_control_card"),
        shape = RoundedCornerShape(ReplyFloatDimens.radius2XLarge),
        colors = CardDefaults.cardColors(
            containerColor = DarkRedSurfaceElevated
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReplyFloatDimens.space16),
            verticalArrangement = Arrangement.spacedBy(ReplyFloatDimens.space12)
        ) {
            // Header Row: Icon, Title, Switch
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
                            .background(
                                if (isEnabled) StatusWarningBg else DarkRedSurfaceCard
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.LayersClear else Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isEnabled) StatusWarningLight else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Pass-Through Mode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isEnabled) "ACTIVE • Gestures pass to background" else "OFF • Overlay captures taps",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEnabled) StatusWarningLight else TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("pass_through_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StatusWarning,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkRedSurfaceCard
                    )
                )
            }

            // Status Explanation
            Surface(
                shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                color = if (isEnabled) StatusWarningBg else DarkRedSurfaceCard,
                border = BorderStroke(1.dp, if (isEnabled) StatusWarningBorder else DarkRedSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isEnabled) StatusWarningLight else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnabled) {
                            "Pass-Through is ON. Taps over empty floating regions pass through to chat applications below (WhatsApp, Telegram, etc.)."
                        } else {
                            "Pass-Through is OFF. The floating assistant directly captures touch gestures for copying replies and changing tones."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            color = if (isEnabled) TextPrimary else TextSecondary
                        )
                    )
                }
            }

            // Quick State Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEnabled) {
                    Button(
                        onClick = { onSetState(PassThroughState.DISABLED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("turn_off_pass_through_button")
                            .semantics { contentDescription = "Turn off pass through mode" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusWarning,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Turn OFF Pass-Through", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSetState(PassThroughState.ENABLED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("turn_on_pass_through_button"),
                        shape = RoundedCornerShape(ReplyFloatDimens.radiusLarge),
                        border = BorderStroke(1.dp, BrandRedBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LayersClear,
                            contentDescription = null,
                            tint = BrandRedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enable Pass-Through", color = BrandRedText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

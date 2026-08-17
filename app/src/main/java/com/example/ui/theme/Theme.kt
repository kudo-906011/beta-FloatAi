package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ReplyFloat AI Dark Red Color System
private val DarkRedColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRedSurface,
    onPrimaryContainer = BrandRedText,
    secondary = BrandRedLight,
    onSecondary = Color.White,
    secondaryContainer = BrandRedDark,
    onSecondaryContainer = Color(0xFFFFD6DA),
    tertiary = BrandRedBright,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF381216),
    onTertiaryContainer = Color(0xFFFFD6DA),
    background = DarkRedBg,
    onBackground = TextPrimary,
    surface = DarkRedSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkRedSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkRedSurfaceBorder,
    outlineVariant = Color(0xFF381519),
    error = StatusError,
    onError = Color.White
)

@Composable
fun ReplyFloatTheme(
    darkTheme: Boolean = true, // Permanent dark red identity
    dynamicColor: Boolean = false, // Keep intentional ReplyFloat brand identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkRedColorScheme,
        typography = ReplyFloatTypography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    ReplyFloatTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

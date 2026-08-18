package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.UiColorPreset

val LocalDynamicTheme = compositionLocalOf {
    resolveDynamicTheme(UiColorPreset.DARK_RED, 0xFF7A0F16L, 0.98f)
}

fun createDynamicColorScheme(dynamicTheme: DynamicUiTheme) = darkColorScheme(
    primary = dynamicTheme.primary,
    onPrimary = dynamicTheme.onPrimary,
    primaryContainer = dynamicTheme.surface,
    onPrimaryContainer = dynamicTheme.accentText,
    secondary = dynamicTheme.primaryLight,
    onSecondary = Color.White,
    secondaryContainer = dynamicTheme.primaryDark,
    onSecondaryContainer = dynamicTheme.accentText,
    tertiary = dynamicTheme.primaryBright,
    onTertiary = Color.White,
    tertiaryContainer = dynamicTheme.surfaceElevated,
    onTertiaryContainer = dynamicTheme.accentText,
    background = DarkRedBg,
    onBackground = TextPrimary,
    surface = dynamicTheme.surface,
    onSurface = TextPrimary,
    surfaceVariant = dynamicTheme.surfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = dynamicTheme.surfaceBorder,
    outlineVariant = dynamicTheme.surfaceHover,
    error = StatusError,
    onError = Color.White
)

@Composable
fun ReplyFloatTheme(
    preset: UiColorPreset = UiColorPreset.DARK_RED,
    customHex: Long = 0xFF7A0F16L,
    opacity: Float = 0.98f,
    theme: DynamicUiTheme? = null,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamicTheme = theme ?: resolveDynamicTheme(preset, customHex, opacity)
    val colorScheme = createDynamicColorScheme(dynamicTheme)

    CompositionLocalProvider(LocalDynamicTheme provides dynamicTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ReplyFloatTypography,
            content = content
        )
    }
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


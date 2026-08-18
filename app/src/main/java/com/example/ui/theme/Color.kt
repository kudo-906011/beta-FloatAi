package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// ReplyFloat AI Dark Red UI Color System
// Centralized visual identity based on deep/dark red with dark surfaces
// =========================================================================

// Primary Brand Colors (Dark Red Palette)
val BrandRed = Color(0xFF7A0F16)               // Primary deep dark red (~#7A0F16)
val BrandRedDark = Color(0xFF52090E)           // Primary darker (~#52090E)
val BrandRedLight = Color(0xFFA51D26)          // Primary lighter/accent (~#A51D26)
val BrandRedBright = Color(0xFFC72834)         // High-visibility accent
val BrandRedSurface = Color(0xFF2E0F12)        // Soft dark red surface / selection
val BrandRedBorder = Color(0xFF521419)         // Subtle dark red border
val BrandRedText = Color(0xFFFFD6DA)           // High-contrast readable light red text

// Secondary & Accent Tones
val AccentCrimson = Color(0xFF8B121B)
val AccentCrimsonLight = Color(0xFFBD2230)
val AccentRose = Color(0xFFE11D48)

// Neutral Dark Surfaces (Very dark neutral/red-tinted surfaces)
val DarkRedBg = Color(0xFF12090A)              // Background (~#12090A)
val DarkRedSurface = Color(0xFF1D0C0E)         // Surface (~#1D0C0E)
val DarkRedSurfaceElevated = Color(0xFF281012) // Elevated surface (~#281012)
val DarkRedSurfaceBorder = Color(0xFF3D161A)   // Surface border (~#3D161A)
val DarkRedSurfaceCard = Color(0xFF220D0F)     // Card background
val DarkRedSurfaceHover = Color(0xFF331418)    // Hover/interactive container

// Text Hierarchy (Optimized for high contrast and readability on dark red surfaces)
val TextPrimary = Color(0xFFF9ECEE)            // Primary text: near-white
val TextSecondary = Color(0xFFD4B8BB)          // Secondary text: muted warm gray
val TextTertiary = Color(0xFF9E7E82)           // Tertiary text: muted warm slate
val TextMuted = Color(0xFF73565A)              // Inactive/muted text

// Status & Semantic Colors
val StatusSuccess = Color(0xFF16A34A)          // Restrained green
val StatusSuccessDark = Color(0xFF15803D)
val StatusSuccessBg = Color(0xFF14291B)
val StatusSuccessBorder = Color(0xFF1F4A2C)

val StatusWarning = Color(0xFFD97706)          // Restrained amber/orange
val StatusWarningLight = Color(0xFFF59E0B)
val StatusWarningBg = Color(0xFF33200D)
val StatusWarningBorder = Color(0xFF593716)

val StatusError = Color(0xFFDC2626)            // Distinct error red
val StatusErrorLight = Color(0xFFEF4444)
val StatusErrorBg = Color(0xFF3B1214)
val StatusErrorBorder = Color(0xFF661D22)

// Gradients
val BrandRedGradient = Brush.linearGradient(
    colors = listOf(BrandRedDark, BrandRed, BrandRedLight)
)

val BrandRedAccentGradient = Brush.horizontalGradient(
    colors = listOf(BrandRedDark, BrandRedLight)
)

val DarkRedCardGradient = Brush.verticalGradient(
    colors = listOf(DarkRedSurfaceElevated, DarkRedSurface)
)

// Legacy alias compatibility layer
val BrandIndigo = BrandRed
val BrandIndigoDark = BrandRed
val BrandIndigoLight = BrandRedLight
val BrandIndigoSurface = BrandRedSurface
val BrandIndigoBorder = BrandRedBorder
val BrandIndigoText = BrandRedText
val BrandViolet = AccentCrimson
val BrandVioletLight = AccentCrimsonLight
val BrandCyan = BrandRedLight
val BrandCyanLight = BrandRedBright

val Slate950 = Color(0xFF0C0607)
val Slate900 = TextPrimary
val Slate800 = Color(0xFFF0DEE0)
val Slate700 = TextSecondary
val Slate600 = TextSecondary
val Slate500 = TextTertiary
val Slate400 = TextTertiary
val Slate300 = Color(0xFF5C3C40)
val Slate200 = DarkRedSurfaceBorder
val Slate100 = DarkRedSurfaceElevated
val Slate50 = DarkRedSurface

val DarkBg = DarkRedBg
val DarkSurface = DarkRedSurface
val DarkSurfaceElevated = DarkRedSurfaceElevated
val DarkSurfaceBorder = DarkRedSurfaceBorder
val DarkTextPrimary = TextPrimary
val DarkTextSecondary = TextSecondary
val DarkTextTertiary = TextTertiary

val LightBg = DarkRedBg
val LightSurface = DarkRedSurface
val LightSurfaceElevated = DarkRedSurfaceElevated
val LightSurfaceBorder = DarkRedSurfaceBorder
val LightTextPrimary = TextPrimary
val LightTextSecondary = TextSecondary
val LightTextTertiary = TextTertiary

val StatusEmerald = StatusSuccess
val StatusEmeraldDark = StatusSuccessDark
val StatusEmeraldBg = StatusSuccessBg
val StatusEmeraldBorder = StatusSuccessBorder

val StatusOrange = StatusWarning
val StatusOrangeLight = StatusWarningLight
val StatusOrangeBg = StatusWarningBg
val StatusOrangeBorder = StatusWarningBorder

val StatusAmber = StatusWarning
val StatusAmberLight = StatusWarningLight
val StatusAmberDark = StatusWarning
val StatusAmberBg = StatusWarningBg
val StatusAmberBorder = StatusWarningBorder

val StatusRose = StatusError
val StatusRoseLight = StatusErrorLight

val BrandGradient = BrandRedGradient
val BrandAccentGradient = BrandRedAccentGradient
val SleekCardGradient = DarkRedCardGradient

// =========================================================================
// Dynamic UI Theme Data Class & Resolver
// =========================================================================

data class DynamicUiTheme(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val primaryBright: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val surfaceCard: Color,
    val surfaceHover: Color,
    val surfaceElevated: Color,
    val accentText: Color,
    val onPrimary: Color,
    val gradient: Brush,
    val cardGradient: Brush,
    val opacity: Float = 0.98f
)

fun resolveDynamicTheme(
    preset: com.example.model.UiColorPreset,
    customHex: Long,
    opacity: Float = 0.98f
): DynamicUiTheme {
    val (primary, primaryDark, primaryLight, surface, border, text) = if (preset == com.example.model.UiColorPreset.CUSTOM) {
        val baseColor = Color(customHex.toULong())
        val r = baseColor.red
        val g = baseColor.green
        val b = baseColor.blue

        val dark = Color((r * 0.65f), (g * 0.65f), (b * 0.65f), 1.0f)
        val light = Color(
            (r + (1f - r) * 0.35f).coerceIn(0f, 1f),
            (g + (1f - g) * 0.35f).coerceIn(0f, 1f),
            (b + (1f - b) * 0.35f).coerceIn(0f, 1f),
            1.0f
        )
        val surf = Color(
            (0.06f + r * 0.12f).coerceIn(0f, 1f),
            (0.04f + g * 0.12f).coerceIn(0f, 1f),
            (0.05f + b * 0.12f).coerceIn(0f, 1f),
            1.0f
        )
        val bord = Color(
            (0.12f + r * 0.22f).coerceIn(0f, 1f),
            (0.08f + g * 0.22f).coerceIn(0f, 1f),
            (0.10f + b * 0.22f).coerceIn(0f, 1f),
            1.0f
        )
        val txt = Color(
            (0.85f + r * 0.15f).coerceIn(0f, 1f),
            (0.80f + g * 0.15f).coerceIn(0f, 1f),
            (0.82f + b * 0.15f).coerceIn(0f, 1f),
            1.0f
        )
        listOf(baseColor, dark, light, surf, bord, txt)
    } else {
        listOf(
            Color(preset.primaryHex),
            Color(preset.darkHex),
            Color(preset.lightHex),
            Color(preset.surfaceHex),
            Color(preset.borderHex),
            Color(preset.textHex)
        )
    }

    val primaryBright = primaryLight
    val surfaceElevated = Color(
        (surface.red * 1.3f).coerceIn(0f, 1f),
        (surface.green * 1.3f).coerceIn(0f, 1f),
        (surface.blue * 1.3f).coerceIn(0f, 1f),
        1.0f
    )
    val surfaceCard = Color(
        (surface.red * 0.85f).coerceIn(0f, 1f),
        (surface.green * 0.85f).coerceIn(0f, 1f),
        (surface.blue * 0.85f).coerceIn(0f, 1f),
        1.0f
    )
    val surfaceHover = Color(
        (surface.red * 1.5f).coerceIn(0f, 1f),
        (surface.green * 1.5f).coerceIn(0f, 1f),
        (surface.blue * 1.5f).coerceIn(0f, 1f),
        1.0f
    )

    return DynamicUiTheme(
        primary = primary,
        primaryDark = primaryDark,
        primaryLight = primaryLight,
        primaryBright = primaryBright,
        surface = surface,
        surfaceBorder = border,
        surfaceCard = surfaceCard,
        surfaceHover = surfaceHover,
        surfaceElevated = surfaceElevated,
        accentText = text,
        onPrimary = Color.White,
        gradient = Brush.linearGradient(listOf(primaryDark, primary, primaryLight)),
        cardGradient = Brush.verticalGradient(listOf(surfaceElevated, surfaceCard)),
        opacity = opacity.coerceIn(0.40f, 1.0f)
    )
}


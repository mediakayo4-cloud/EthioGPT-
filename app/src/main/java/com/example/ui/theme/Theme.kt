package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val EthioLightColorScheme = lightColorScheme(
    primary = EthioGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = EthioGreenContainer,
    onPrimaryContainer = EthioOnGreenContainer,
    secondary = EthioGoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = EthioGoldContainer,
    onSecondaryContainer = EthioOnGoldContainer,
    tertiary = EthioCrimsonTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EthioCrimsonContainer,
    onTertiaryContainer = EthioOnCrimsonContainer,
    background = WarmAlabasterBg,
    onBackground = InkObsidian,
    surface = WarmAlabasterBg,
    onSurface = InkObsidian,
    surfaceVariant = WarmCreamSurface,
    onSurfaceVariant = MutedOlive,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmCreamSurface,
    surfaceContainer = WarmCreamSurface,
    surfaceContainerHigh = WarmSurfaceHigh,
    outline = WarmOutline,
    error = EthioCrimsonTertiary,
    onError = Color.White
)

private val EthioDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkGoldSecondary,
    onSecondary = DarkOnGold,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = DarkOnGoldContainer,
    tertiary = DarkCrimsonTertiary,
    onTertiary = DarkOnCrimson,
    tertiaryContainer = DarkCrimsonContainer,
    onTertiaryContainer = DarkOnCrimsonContainer,
    background = ObsidianBg,
    onBackground = InkMist,
    surface = ObsidianSurface,
    onSurface = InkMist,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = MutedSage,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = DarkOutline,
    error = DarkCrimsonTertiary,
    onError = DarkOnCrimson
)

val EthioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EthioGptTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (isDark) EthioDarkColorScheme else EthioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EthioTypography,
        shapes = EthioShapes,
        content = content
    )
}

package com.expensevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinimalColorScheme = lightColorScheme(
    primary = HeroBlack,
    onPrimary = NeutralBackground,
    primaryContainer = HeroBlack,
    onPrimaryContainer = NeutralBackground,
    secondary = InkSecondary,
    onSecondary = NeutralCard,
    secondaryContainer = NeutralSurfaceMuted,
    onSecondaryContainer = InkPrimary,
    tertiary = MutedSage,
    onTertiary = NeutralBackground,
    tertiaryContainer = MutedSageLight,
    onTertiaryContainer = MutedSage,
    background = NeutralBackground,
    onBackground = InkPrimary,
    surface = NeutralCard,
    onSurface = InkPrimary,
    surfaceVariant = NeutralSurfaceMuted,
    onSurfaceVariant = InkSecondary,
    surfaceTint = Color.Transparent,
    outline = DividerHairline,
    outlineVariant = DividerHairline,
    error = MutedClay,
    onError = NeutralBackground,
    errorContainer = MutedClayLight,
    onErrorContainer = MutedClay
)

@Composable
fun KiteTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinimalColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ExpenseVaultTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KiteTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

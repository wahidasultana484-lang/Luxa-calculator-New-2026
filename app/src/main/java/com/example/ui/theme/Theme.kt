package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Immutable
data class LuxaCalculatorColors(
    val displayBackground: Color,
    val digitKeyBackground: Color,
    val digitKeyContent: Color,
    val operatorKeyBackground: Color,
    val operatorKeyContent: Color,
    val equalsKeyBackground: Color,
    val equalsKeyContent: Color,
    val clearKeyBackground: Color,
    val clearKeyContent: Color,
    val scientificKeyBackground: Color,
    val scientificKeyContent: Color,
    val memoryKeyBackground: Color,
    val memoryKeyContent: Color,
    val resultPreviewText: Color,
)

val LocalLuxaCalculatorColors = staticCompositionLocalOf {
    LuxaCalculatorColors(
        displayBackground = LuxaSurfaceContainerLowLight,
        digitKeyBackground = LuxaSurfaceContainerHighLight,
        digitKeyContent = LuxaOnSurfaceLight,
        operatorKeyBackground = LuxaSecondaryContainerLight,
        operatorKeyContent = LuxaOnSecondaryContainerLight,
        equalsKeyBackground = LuxaBluePrimaryContainerLight,
        equalsKeyContent = LuxaOnBluePrimaryContainerLight,
        clearKeyBackground = LuxaTertiaryContainerLight,
        clearKeyContent = LuxaOnTertiaryContainerLight,
        scientificKeyBackground = LuxaSurfaceContainerLight,
        scientificKeyContent = LuxaOnSurfaceVariantLight,
        memoryKeyBackground = LuxaSurfaceContainerLight,
        memoryKeyContent = LuxaOnSurfaceVariantLight,
        resultPreviewText = LuxaOnSurfaceVariantLight,
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = LuxaBluePrimaryDark,
    onPrimary = LuxaOnBluePrimaryDark,
    primaryContainer = LuxaBluePrimaryContainerDark,
    onPrimaryContainer = LuxaOnBluePrimaryContainerDark,
    secondary = LuxaSecondaryDark,
    onSecondary = LuxaOnSecondaryDark,
    secondaryContainer = LuxaSecondaryContainerDark,
    onSecondaryContainer = LuxaOnSecondaryContainerDark,
    tertiary = LuxaTertiaryDark,
    onTertiary = LuxaOnTertiaryDark,
    tertiaryContainer = LuxaTertiaryContainerDark,
    onTertiaryContainer = LuxaOnTertiaryContainerDark,
    background = LuxaBackgroundDark,
    onBackground = LuxaOnBackgroundDark,
    surface = LuxaSurfaceDark,
    onSurface = LuxaOnSurfaceDark,
    surfaceVariant = LuxaSurfaceVariantDark,
    onSurfaceVariant = LuxaOnSurfaceVariantDark,
    surfaceContainerLow = LuxaSurfaceContainerLowDark,
    surfaceContainer = LuxaSurfaceContainerDark,
    surfaceContainerHigh = LuxaSurfaceContainerHighDark,
    surfaceContainerHighest = LuxaSurfaceContainerHighestDark,
    outline = LuxaOutlineDark,
    outlineVariant = LuxaOutlineVariantDark,
    error = LuxaErrorDark,
    onError = LuxaOnErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = LuxaBluePrimaryLight,
    onPrimary = LuxaOnBluePrimaryLight,
    primaryContainer = LuxaBluePrimaryContainerLight,
    onPrimaryContainer = LuxaOnBluePrimaryContainerLight,
    secondary = LuxaSecondaryLight,
    onSecondary = LuxaOnSecondaryLight,
    secondaryContainer = LuxaSecondaryContainerLight,
    onSecondaryContainer = LuxaOnSecondaryContainerLight,
    tertiary = LuxaTertiaryLight,
    onTertiary = LuxaOnTertiaryLight,
    tertiaryContainer = LuxaTertiaryContainerLight,
    onTertiaryContainer = LuxaOnTertiaryContainerLight,
    background = LuxaBackgroundLight,
    onBackground = LuxaOnBackgroundLight,
    surface = LuxaSurfaceLight,
    onSurface = LuxaOnSurfaceLight,
    surfaceVariant = LuxaSurfaceVariantLight,
    onSurfaceVariant = LuxaOnSurfaceVariantLight,
    surfaceContainerLow = LuxaSurfaceContainerLowLight,
    surfaceContainer = LuxaSurfaceContainerLight,
    surfaceContainerHigh = LuxaSurfaceContainerHighLight,
    surfaceContainerHighest = LuxaSurfaceContainerHighestLight,
    outline = LuxaOutlineLight,
    outlineVariant = LuxaOutlineVariantLight,
    error = LuxaErrorLight,
    onError = LuxaOnErrorLight,
)

@Composable
fun LuxaCalculatorTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val calculatorColors = androidx.compose.runtime.remember(colorScheme) {
        LuxaCalculatorColors(
            displayBackground = colorScheme.surfaceContainer,
            digitKeyBackground = colorScheme.surfaceContainerHigh,
            digitKeyContent = colorScheme.onSurface,
            operatorKeyBackground = colorScheme.secondaryContainer,
            operatorKeyContent = colorScheme.onSecondaryContainer,
            equalsKeyBackground = colorScheme.primaryContainer,
            equalsKeyContent = colorScheme.onPrimaryContainer,
            clearKeyBackground = colorScheme.tertiaryContainer,
            clearKeyContent = colorScheme.onTertiaryContainer,
            scientificKeyBackground = colorScheme.surfaceContainer,
            scientificKeyContent = colorScheme.onSurfaceVariant,
            memoryKeyBackground = colorScheme.surfaceContainer,
            memoryKeyContent = colorScheme.onSurfaceVariant,
            resultPreviewText = colorScheme.onSurfaceVariant,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLuxaCalculatorColors provides calculatorColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

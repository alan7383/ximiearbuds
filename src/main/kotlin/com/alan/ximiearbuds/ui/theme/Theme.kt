package com.alan.ximiearbuds.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = XiaomiCyan,
    onPrimary = XiaomiPageBg,
    primaryContainer = XiaomiCardHover,
    onPrimaryContainer = XiaomiTextPrimary,
    secondary = XiaomiBlue,
    onSecondary = XiaomiPageBg,
    tertiary = XiaomiOrange,
    background = XiaomiPageBg,
    onBackground = XiaomiTextPrimary,
    surface = XiaomiCardBg,
    onSurface = XiaomiTextPrimary,
    surfaceVariant = XiaomiCardHover,
    onSurfaceVariant = XiaomiTextSecondary,
    outline = XiaomiCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = XiaomiCyan,
    onPrimary = XiaomiLightPageBg,
    primaryContainer = XiaomiLightCardHover,
    onPrimaryContainer = XiaomiLightTextPrimary,
    secondary = XiaomiBlue,
    onSecondary = XiaomiLightPageBg,
    tertiary = XiaomiOrange,
    background = XiaomiLightPageBg,
    onBackground = XiaomiLightTextPrimary,
    surface = XiaomiLightCardBg,
    onSurface = XiaomiLightTextPrimary,
    surfaceVariant = XiaomiLightCardHover,
    onSurfaceVariant = XiaomiLightTextSecondary,
    outline = XiaomiLightCardBorder
)

// Xiaomi official 16dp card corners, 24dp for pills/capsules
val XiaomiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun XimiEarbudsTheme(
    darkTheme: Boolean = true,
    language: AppLanguage = AppLanguage.FR,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val strings = StringsManager.getStrings(language)

    CompositionLocalProvider(LocalStrings provides strings) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = XiaomiShapes,
            content = content
        )
    }
}

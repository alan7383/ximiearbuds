package com.alan.ximiearbuds.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import com.alan.ximiearbuds.ximiearbuds.generated.resources.Res
import com.alan.ximiearbuds.ximiearbuds.generated.resources.misanslatin_bold
import com.alan.ximiearbuds.ximiearbuds.generated.resources.misanslatin_medium
import com.alan.ximiearbuds.ximiearbuds.generated.resources.misanslatin_regular
import com.alan.ximiearbuds.ximiearbuds.generated.resources.misanslatin_semibold

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


// Vraies polices de l'app officielle (res/font/) :
// FontRegular/FontMedium -> mipro (= MiSans système) ; FontLatin* -> misanslatin_* bundlés.
// Sur desktop on charge les OTF MiSans Latin officiels via composeResources.
@Composable
fun rememberMiSans(): FontFamily {
    val regular = Font(Res.font.misanslatin_regular, FontWeight.Normal)
    val medium = Font(Res.font.misanslatin_medium, FontWeight.Medium)
    val semibold = Font(Res.font.misanslatin_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.misanslatin_bold, FontWeight.Bold)
    return FontFamily(regular, medium, semibold, bold)
}

@Composable
private fun miSansTypography(miSans: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = miSans),
        displayMedium = base.displayMedium.copy(fontFamily = miSans),
        displaySmall = base.displaySmall.copy(fontFamily = miSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = miSans),
        headlineMedium = base.headlineMedium.copy(fontFamily = miSans),
        headlineSmall = base.headlineSmall.copy(fontFamily = miSans),
        titleLarge = base.titleLarge.copy(fontFamily = miSans),
        titleMedium = base.titleMedium.copy(fontFamily = miSans),
        titleSmall = base.titleSmall.copy(fontFamily = miSans),
        bodyLarge = base.bodyLarge.copy(fontFamily = miSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = miSans),
        bodySmall = base.bodySmall.copy(fontFamily = miSans),
        labelLarge = base.labelLarge.copy(fontFamily = miSans),
        labelMedium = base.labelMedium.copy(fontFamily = miSans),
        labelSmall = base.labelSmall.copy(fontFamily = miSans)
    )
}

@Composable
fun XimiEarbudsTheme(
    darkTheme: Boolean = true,
    language: AppLanguage = AppLanguage.FR,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val strings = StringsManager.getStrings(language)
    val miSans = rememberMiSans()
    val typography = miSansTypography(miSans)

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalTextStyle provides TextStyle(fontFamily = miSans)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = XiaomiShapes,
            typography = typography
        ) {
            ProvideTextStyle(TextStyle(fontFamily = miSans)) {
                content()
            }
        }
    }
}

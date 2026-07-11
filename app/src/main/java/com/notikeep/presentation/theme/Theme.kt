package com.notikeep.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.notikeep.domain.model.ThemeMode

/**
 * One brand palette for the whole app: primaries and accents come from the
 * launcher-icon blues (Color.kt), so every screen matches the icon. Screens must
 * take colors from MaterialTheme.colorScheme, never hardcode them.
 */
private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = BrandOnBlue,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = BrandBlueDeep,
    secondary = BrandBlueBright,
    onSecondary = BrandOnBlue,
    tertiary = TealSecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueBright,
    onPrimary = BrandOnBlue,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = BrandOnBlue,
    secondary = BrandBlueBright,
    onSecondary = BrandOnBlue,
    tertiary = TealSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

@Composable
fun NotikeepTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}

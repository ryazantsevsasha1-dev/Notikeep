package com.notikeep.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.notikeep.domain.model.ThemeMode

/**
 * One brand palette for the whole app: primaries and accents come from the
 * launcher-icon blues (Color.kt), so every screen matches the icon. Screens must
 * take colors from MaterialTheme.colorScheme, never hardcode them.
 *
 * Two-accent system, mirroring the logo (blue shield + teal bookmark):
 *  - primary  (blue) = actions & navigation
 *  - tertiary (teal) = "saved / worth keeping" — favorites, unread badges, kept cues
 * Reach for the teal accent via MaterialTheme.colorScheme.tertiary.
 */
private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = BrandOnBlue,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = BrandBlueDeep,
    secondary = BrandBlueBright,
    onSecondary = BrandOnBlue,
    tertiary = SavedAccent,
    onTertiary = OnSavedAccent,
    tertiaryContainer = Color(0xFFB9F1E9),
    onTertiaryContainer = Color(0xFF04322D),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = BrandOnBlue,
    secondary = BrandBlueLighter,
    onSecondary = Color.Black,
    tertiary = SavedAccentDark,
    onTertiary = OnSavedAccent,
    tertiaryContainer = Color(0xFF12564D),
    onTertiaryContainer = Color(0xFFB9F1E9),
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

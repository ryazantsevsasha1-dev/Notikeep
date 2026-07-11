package com.notikeep.presentation.theme

import androidx.compose.ui.graphics.Color

// Notikeep brand — premium blues matched to the launcher icon so every screen
// and the icon read as one product. Single source of truth for all UI colors.

// Primary Palette (Light)
val BrandBlue = Color(0xFF2E44BE)        // icon shield core; primary actions
val BrandBlueBright = Color(0xFF4C5FD5)  // icon shield body; secondary emphasis
val BrandBlueDeep = Color(0xFF2440B3)    // icon background base; dark accents

// Primary Palette (Dark Theme Optimization)
val BrandBlueLight = Color(0xFF7C8CF5)   // Lighter indigo for better legibility on dark
val BrandBlueLighter = Color(0xFFA0ACF8) // Soft blue for secondary dark accents

val BrandOnBlue = Color(0xFFFFFFFF)

// --- Semantic accent system ---------------------------------------------------
// Blue = actions & navigation (primary). Teal = "saved / worth keeping", echoing
// the bookmark on the launcher icon: favorites, unread badges, kept-content cues.
val TealSecondary = Color(0xFF1FB6A6)    // bookmark/star accent — the "saved" hue
val SavedAccent = TealSecondary          // semantic alias: use for saved/kept UI
val SavedAccentDark = Color(0xFF34D0BF)  // brighter teal for legibility on dark surfaces
val OnSavedAccent = Color(0xFF04322D)    // text/icon on a filled teal surface

// Background & Surfaces
val LightBackground = Color(0xFFF6F7FD)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EBF8)

val DarkBackground = Color(0xFF10131D)
val DarkSurface = Color(0xFF1A1E2C)
val DarkSurfaceVariant = Color(0xFF262B3D)

// Semantic extras
val SilencedAmber = Color(0xFFE0A100)
val RuleIgnoreRed = Color(0xFFE53935)
val RuleShadeGreen = Color(0xFF43A047)

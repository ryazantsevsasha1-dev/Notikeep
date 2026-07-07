package com.notikeep.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level routes. Onboarding lives outside the bottom bar. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val ARCHIVE = "archive"
    const val RULES = "rules"
    const val SETTINGS = "settings"

    /** Per-app notification list, pushed from the archive. */
    const val APP_NOTIFICATIONS = "archive/app/{packageName}"

    fun appNotifications(packageName: String) = "archive/app/$packageName"
}

/** The three tabs shown in the bottom bar, in order. Search lives inside Archive now. */
enum class TopTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    ARCHIVE(Routes.ARCHIVE, com.notikeep.R.string.nav_archive, Icons.AutoMirrored.Filled.List),
    RULES(Routes.RULES, com.notikeep.R.string.nav_rules, Icons.Filled.Apps),
    SETTINGS(Routes.SETTINGS, com.notikeep.R.string.nav_settings, Icons.Filled.Settings),
}

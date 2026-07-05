package com.notikeep.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level routes. Onboarding lives outside the bottom bar. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val ARCHIVE = "archive"
    const val SEARCH = "search"
    const val RULES = "rules"
    const val SETTINGS = "settings"
}

/** The four tabs shown in the bottom bar, in order. */
enum class TopTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    ARCHIVE(Routes.ARCHIVE, com.notikeep.R.string.nav_archive, Icons.AutoMirrored.Filled.List),
    SEARCH(Routes.SEARCH, com.notikeep.R.string.nav_search, Icons.Filled.Search),
    RULES(Routes.RULES, com.notikeep.R.string.nav_rules, Icons.Filled.Apps),
    SETTINGS(Routes.SETTINGS, com.notikeep.R.string.nav_settings, Icons.Filled.Settings),
}

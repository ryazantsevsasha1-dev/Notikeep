package com.notikeep.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.notikeep.presentation.appdetail.AppNotificationsScreen
import com.notikeep.presentation.archive.ArchiveScreen
import com.notikeep.presentation.onboarding.OnboardingScreen
import com.notikeep.presentation.rules.RulesScreen
import com.notikeep.presentation.settings.SettingsScreen

/**
 * Top-level navigation. When onboarding is not done, it is the only destination;
 * once finished, the app switches to the three-tab shell (search lives in Archive).
 */
@Composable
fun NotikeepNavHost(onboardingCompleted: Boolean) {
    val navController = rememberNavController()

    if (!onboardingCompleted) {
        OnboardingScreen(onFinished = { /* Root recomposes via settings flow */ })
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopTab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ARCHIVE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ARCHIVE) {
                ArchiveScreen(
                    onOpenApp = { pkg -> navController.navigate(Routes.appNotifications(pkg)) },
                )
            }
            composable(
                route = Routes.APP_NOTIFICATIONS,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
            ) {
                AppNotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.RULES) { RulesScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

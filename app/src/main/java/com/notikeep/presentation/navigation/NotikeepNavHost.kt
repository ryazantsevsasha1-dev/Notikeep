package com.notikeep.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.notikeep.presentation.favorites.FavoritesScreen
import com.notikeep.presentation.onboarding.OnboardingScreen
import com.notikeep.presentation.rules.RulesScreen
import com.notikeep.presentation.settings.SettingsScreen

/**
 * Top-level navigation. When onboarding is not done, it is the only destination;
 * once finished, the app switches to the three-tab shell (search lives in Archive).
 *
 * [onNaturalBreak] fires on natural pauses in the flow (leaving a per-app
 * notification list) — the interstitial ad hook; whether an ad actually shows
 * is decided remotely (see InterstitialAdManager).
 */
@Composable
fun NotikeepNavHost(
    onboardingCompleted: Boolean,
    onNaturalBreak: () -> Unit = {},
) {
    val navController = rememberNavController()

    if (!onboardingCompleted) {
        OnboardingScreen(onFinished = { /* Root recomposes via settings flow */ })
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NotikeepBottomBar(
                tabs = TopTab.entries,
                isSelected = { tab ->
                    currentDestination?.hierarchy?.any { it.route == tab.route } == true
                },
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ARCHIVE,
            modifier = Modifier.padding(innerPadding),
            // Gentle cross-fade between tabs; detail screens slide in from the side.
            enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
            exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
        ) {
            composable(Routes.ARCHIVE) {
                ArchiveScreen(
                    onOpenApp = { pkg -> navController.navigate(Routes.appNotifications(pkg)) },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onOpenApp = { pkg ->
                        navController.navigate(Routes.appNotifications(pkg, favoritesOnly = true))
                    },
                )
            }
            composable(
                route = Routes.APP_NOTIFICATIONS,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType },
                    navArgument("favoritesOnly") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DETAIL_SLIDE_MS))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(DETAIL_SLIDE_MS))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DETAIL_SLIDE_MS))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(DETAIL_SLIDE_MS))
                },
            ) {
                AppNotificationsScreen(
                    onBack = {
                        navController.popBackStack()
                        onNaturalBreak()
                    },
                )
            }
            composable(Routes.RULES) { RulesScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

private const val TAB_FADE_MS = 220
private const val DETAIL_SLIDE_MS = 300

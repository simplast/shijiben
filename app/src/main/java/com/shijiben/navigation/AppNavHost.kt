package com.shijiben.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shijiben.feature.heatmap.HeatmapScreen
import com.shijiben.feature.notes.NotesScreen
import com.shijiben.feature.search.SearchScreen
import com.shijiben.feature.settings.AboutScreen
import com.shijiben.feature.settings.SettingsScreen
import com.shijiben.feature.timeviz.TimeVizScreen
import com.shijiben.feature.timeline.TimelineScreen

object Routes {
    const val TIMELINE = "timeline"
    const val NOTES = "notes"
    const val TIMEVIZ = "timeviz"
    const val HEATMAP = "heatmap"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.TIMELINE
    ) {
        composable(Routes.TIMELINE) { entry ->
            var pendingDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
            LaunchedEffect(entry) {
                entry.savedStateHandle
                    .getStateFlow<Triple<Int, Int, Int>?>("heatmap_target_date", null)
                    .collect { d ->
                        if (d != null) {
                            pendingDate = d
                            entry.savedStateHandle.remove<Triple<Int, Int, Int>>("heatmap_target_date")
                        }
                    }
            }
            TimelineScreen(
                onNotesClick = { navController.navigate(Routes.NOTES) },
                onTimeVizClick = { navController.navigate(Routes.TIMEVIZ) },
                onHeatmapClick = { navController.navigate(Routes.HEATMAP) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                targetDate = pendingDate,
                onDateApplied = { pendingDate = null }
            )
        }
        composable(Routes.NOTES) {
            NotesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TIMEVIZ) {
            TimeVizScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HEATMAP) {
            HeatmapScreen(
                onBack = { navController.popBackStack() },
                onDateClick = { (y, m, d) ->
                    navController.getBackStackEntry(Routes.TIMELINE)
                        .savedStateHandle["heatmap_target_date"] = Triple(y, m, d)
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAboutClick = { navController.navigate(Routes.ABOUT) },
                onPrivacyClick = { navController.navigate(Routes.PRIVACY) }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRIVACY) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                scrollToPrivacy = true
            )
        }
    }
}

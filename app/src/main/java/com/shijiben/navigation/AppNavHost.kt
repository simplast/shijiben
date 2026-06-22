package com.shijiben.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shijiben.feature.notes.NotesScreen
import com.shijiben.feature.tags.TagsScreen
import com.shijiben.feature.timeline.TimelineScreen

object Routes {
    const val TIMELINE = "timeline"
    const val TAGS = "tags"
    const val NOTES = "notes"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.TIMELINE
    ) {
        composable(Routes.TIMELINE) {
            TimelineScreen(
                onTagsClick = { navController.navigate(Routes.TAGS) },
                onNotesClick = { navController.navigate(Routes.NOTES) }
            )
        }
        composable(Routes.TAGS) {
            TagsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTES) {
            NotesScreen(onBack = { navController.popBackStack() })
        }
    }
}

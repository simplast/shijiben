package com.doer.shijiben.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
}

@Composable
fun ShijibenNavHost(
    navController: NavHostController,
    viewModel: EventViewModel,
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onAddEvent = {
                    // Handled inside HomeScreen via BottomSheet now
                },
                onOpenEvent = { id ->
                    // Handled inside HomeScreen via BottomSheet now
                },
            )
        }
    }
}

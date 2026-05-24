package com.doer.shijiben.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.screens.HomeScreen
import com.doer.shijiben.ui.screens.MonthlyReviewScreen
import com.doer.shijiben.ui.screens.WeeklyReviewScreen

object Routes {
    const val HOME = "home"
    const val REVIEW = "review"
    const val MONTHLY_REVIEW = "monthly_review"
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
                onOpenReview = {
                    navController.navigate(Routes.REVIEW)
                }
            )
        }
        composable(Routes.REVIEW) {
            WeeklyReviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenMonthlyReview = { navController.navigate(Routes.MONTHLY_REVIEW) }
            )
        }
        composable(Routes.MONTHLY_REVIEW) {
            MonthlyReviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

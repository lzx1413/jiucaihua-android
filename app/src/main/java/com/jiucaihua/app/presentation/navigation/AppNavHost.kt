package com.jiucaihua.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jiucaihua.app.presentation.ai.AiChatScreen
import com.jiucaihua.app.presentation.alerts.AlertsScreen
import com.jiucaihua.app.presentation.article.ArticleDetailScreen
import com.jiucaihua.app.presentation.detail.DetailScreen
import com.jiucaihua.app.presentation.holdings.AddEditHoldingScreen
import com.jiucaihua.app.presentation.portfolio.PortfolioScreen
import com.jiucaihua.app.presentation.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Portfolio.route
    ) {
        composable(Screen.Portfolio.route) {
            PortfolioScreen(
                onAddHolding = {
                    navController.navigate(Screen.AddEditHolding.createRoute())
                },
                onEditHolding = { holdingId ->
                    navController.navigate(Screen.AddEditHolding.createRoute(holdingId))
                },
                onHoldingClick = { code ->
                    navController.navigate(Screen.Detail.createRoute(code))
                },
                onArticleClick = { article ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("articleTitle", article.title)
                    navController.currentBackStackEntry?.savedStateHandle?.set("articleSummary", article.summary)
                    navController.currentBackStackEntry?.savedStateHandle?.set("articleContent", article.content)
                    navController.currentBackStackEntry?.savedStateHandle?.set("articleSource", article.source)
                    navController.currentBackStackEntry?.savedStateHandle?.set("articleTime", article.time)
                    navController.navigate(Screen.ArticleDetail.route)
                },
                onNavigateToAlerts = {
                    navController.navigate(Screen.Alerts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
            )
        }

        composable(
            route = "add_edit_holding?holdingId={holdingId}",
            arguments = listOf(
                navArgument("holdingId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            AddEditHoldingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{code}",
            arguments = listOf(
                navArgument("code") {
                    type = NavType.StringType
                }
            )
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ArticleDetail.route) {
            val previousStateHandle = remember(navController.previousBackStackEntry) {
                navController.previousBackStackEntry?.savedStateHandle
            }
            ArticleDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                articleTitle = previousStateHandle?.get<String>("articleTitle").orEmpty(),
                articleSummary = previousStateHandle?.get<String>("articleSummary").orEmpty(),
                articleContent = previousStateHandle?.get<String>("articleContent").orEmpty(),
                articleSource = previousStateHandle?.get<String>("articleSource").orEmpty(),
                articleTime = previousStateHandle?.get<String>("articleTime").orEmpty(),
            )
        }

        composable(Screen.Alerts.route) {
            AlertsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiChat.route) {
            AiChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

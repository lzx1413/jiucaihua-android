package com.jiucaihua.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.presentation.ai.AiChatScreen
import com.jiucaihua.app.presentation.alerts.AlertsScreen
import com.jiucaihua.app.presentation.article.ArticleDetailScreen
import com.jiucaihua.app.presentation.detail.DetailScreen
import com.jiucaihua.app.presentation.holdings.AddEditHoldingScreen
import com.jiucaihua.app.presentation.market.MarketScreen
import com.jiucaihua.app.presentation.portfolio.PortfolioScreen
import com.jiucaihua.app.presentation.settings.SettingsScreen

private fun NavController.navigateToArticle(article: NewsFlash) {
    currentBackStackEntry?.savedStateHandle?.apply {
        set("articleTitle", article.title)
        set("articleSummary", article.summary)
        set("articleContent", article.content)
        set("articleSource", article.source)
        set("articleTime", article.time)
    }
    navigate(Screen.ArticleDetail.route)
}

@Composable
fun AppNavHost(initialDestination: String? = null) {
    val navController = rememberNavController()

    val startDestination = if (initialDestination != null && initialDestination.startsWith("detail/")) {
        initialDestination
    } else {
        Screen.Portfolio.route
    }

    LaunchedEffect(initialDestination) {
        if (initialDestination != null && initialDestination.startsWith("detail/")) {
            navController.navigate(initialDestination) {
                popUpTo(Screen.Portfolio.route) { inclusive = false }
            }
        }
    }

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
                    navController.navigateToArticle(article)
                },
                onNavigateToAlerts = {
                    navController.navigate(Screen.Alerts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToMarket = {
                    navController.navigate(Screen.Market.route)
                },
            )
        }

        composable(Screen.Market.route) {
            MarketScreen(
                onNavigateBack = { navController.popBackStack() },
                onIndexClick = { index ->
                    navController.navigate(Screen.Detail.createRoute(index.code))
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
                onNavigateBack = { navController.popBackStack() },
                onArticleClick = { article ->
                    navController.navigateToArticle(article)
                },
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
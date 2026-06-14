package com.jiucaihua.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Portfolio : Screen("portfolio")
    data object Market : Screen("market")
    data object AddEditHolding : Screen("add_edit_holding?holdingId={holdingId}") {
        fun createRoute(holdingId: Long? = null): String {
            return if (holdingId != null) "add_edit_holding?holdingId=$holdingId"
            else "add_edit_holding"
        }
    }
    data object Detail : Screen("detail/{code}") {
        fun createRoute(code: String): String = "detail/$code"
    }
    data object ArticleDetail : Screen("article_detail")
    data object Alerts : Screen("alerts")
    data object Settings : Screen("settings")
    data object AiChat : Screen("ai_chat")
}

object NavExtras {
    const val EXTRA_TARGET_ROUTE = "target_route"
    const val EXTRA_TARGET_CODE = "target_code"
    const val EXTRA_TARGET_ID = "target_id"
}

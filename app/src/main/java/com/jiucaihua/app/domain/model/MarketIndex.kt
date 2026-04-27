package com.jiucaihua.app.domain.model

data class MarketIndex(
    val code: String,
    val name: String,
    val price: Double,
    val yestClose: Double,
    val changePercent: Double,
    val changeAmount: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val volume: Double = 0.0,
    val amount: Double = 0.0,
    val time: String = "",
    val marketType: MarketType,
)

enum class MarketTab(val label: String) {
    A_STOCK("A股"),
    HK_STOCK("港股"),
    US_STOCK("美股"),
}

object MarketIndexCodes {
    val A_STOCK_INDICES = listOf(
        "sh000001",  // 上证指数
        "sz399001",  // 深证成指
        "sz399006",  // 创业板指
        "sh000688",  // 科创板指
        "sh000300",  // 沪深300
        "sh000016",  // 上证50
    )

    val HK_STOCK_INDICES = listOf(
        "hkHSI",     // 恒生指数
        "hkHSCEI",   // 恒生国企指数
        "hkHSTECH",  // 恒生科技指数
    )

    val US_STOCK_INDICES = listOf(
        "usr_dji",   // 道琼斯
        "usr_ixic",  // 纳斯达克
        "usr_inx",   // 标普500
    )

    val A_STOCK_NAMES = mapOf(
        "sh000001" to "上证指数",
        "sz399001" to "深证成指",
        "sz399006" to "创业板指",
        "sh000688" to "科创板指",
        "sh000300" to "沪深300",
        "sh000016" to "上证50",
    )

    val HK_STOCK_NAMES = mapOf(
        "hkHSI" to "恒生指数",
        "hkHSCEI" to "国企指数",
        "hkHSTECH" to "科技指数",
    )

    val US_STOCK_NAMES = mapOf(
        "usr_dji" to "道琼斯",
        "usr_ixic" to "纳斯达克",
        "usr_inx" to "标普500",
    )
}
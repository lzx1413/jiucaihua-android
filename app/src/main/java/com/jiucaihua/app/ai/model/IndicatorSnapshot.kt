package com.jiucaihua.app.ai.model

data class IndicatorSnapshot(
    // 基本信息
    val code: String,
    val name: String,
    val price: Double,
    val date: String,

    // 均线判定
    val ma5: Double? = null,
    val ma20: Double? = null,
    val ma60: Double? = null,
    val ma120: Double? = null,
    val vs_ma20: String? = null,
    val vs_ma60: String? = null,
    val vs_ma120: String? = null,
    val ma20_slope: String? = null,
    val distance_to_ma20_pct: Double? = null,
    val distance_to_ma60_pct: Double? = null,

    // 量价指标
    val volume_ratio: Double? = null,
    val change_10d_pct: Double? = null,

    // 布林线
    val boll_position: String? = null,
    val boll_upper: Double? = null,
    val boll_middle: Double? = null,
    val boll_lower: Double? = null,

    // MACD
    val macd_status: String? = null,

    // 大盘环境
    val hs300_vs_ma20: String? = null,

    // 持仓相关
    val current_pnl_pct: Double? = null,
    val peak_pnl_pct: Double? = null,
    val pnl_drawdown_from_peak: Double? = null,
    val stop_loss_price: Double? = null,

    // 预警距离
    val distance_to_stop_loss_pct: Double? = null,
    val distance_to_pnl_30_pct: Double? = null,
    val distance_to_pnl_50_pct: Double? = null,
)

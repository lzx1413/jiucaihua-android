package com.jiucaihua.app.domain.model

data class FundFlowData(
    val updateTime: String = "",
    val northFlow: NorthFlowData = NorthFlowData(),
    val southFlow: SouthFlowData = SouthFlowData(),
)

data class NorthFlowData(
    val hgtNetInflow: Double = 0.0,       // 沪股通净流入（万元）
    val hgtNetBuy: Double = 0.0,          // 沪股通净买入（万元）
    val hgtRemain: Double = 0.0,          // 沪股通余额（万元）
    val sgtNetInflow: Double = 0.0,       // 深股通净流入（万元）
    val sgtNetBuy: Double = 0.0,          // 深股通净买入（万元）
    val sgtRemain: Double = 0.0,          // 深股通余额（万元）
    val totalNetInflow: Double = 0.0,     // 北向资金合计净流入（万元）
)

data class SouthFlowData(
    val ggtShNetInflow: Double = 0.0,     // 港股通(沪)净流入（万元）
    val ggtShNetBuy: Double = 0.0,        // 港股通(沪)净买入（万元）
    val ggtShRemain: Double = 0.0,        // 港股通(沪)余额（万元）
    val ggtSzNetInflow: Double = 0.0,     // 港股通(深)净流入（万元）
    val ggtSzNetBuy: Double = 0.0,        // 港股通(深)净买入（万元）
    val ggtSzRemain: Double = 0.0,        // 港股通(深)余额（万元）
    val totalNetInflow: Double = 0.0,     // 南向资金合计净流入（万元）
)

data class HsgtDetail(
    val type: String,           // 沪港通/深港通
    val channel: String,        // 沪股通/港股通(沪)等
    val direction: String,      // 北向/南向
    val status: String,         // 交易状态
    val netInflow: Double,      // 资金净流入（亿元）
    val netBuy: Double,         // 成交净买额（亿元）
    val remain: Double,         // 当日余额（亿元）
    val upCount: Int,           // 上涨数
    val flatCount: Int,         // 持平数
    val downCount: Int,         // 下跌数
    val relatedIndex: String,   // 相关指数名称
    val indexChange: Double,    // 指数涨跌幅
)
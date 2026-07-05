package com.jiucaihua.app.presentation.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.AlertType
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.KLinePeriod
import com.jiucaihua.app.domain.model.MarketSession
import com.jiucaihua.app.domain.model.MarketTab
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.SortOrder
import com.jiucaihua.app.presentation.holdings.HoldingTradeAction

@Composable
fun MarketType.localizedLabel(): String = stringResource(
    when (this) {
        MarketType.A_STOCK -> R.string.market_type_a_stock
        MarketType.HK_STOCK -> R.string.market_type_hk_stock
        MarketType.US_STOCK -> R.string.market_type_us_stock
        MarketType.FUND -> R.string.market_type_fund
        MarketType.GOLD -> R.string.market_type_gold
    }
)

@Composable
fun MarketSession.localizedLabel(): String = stringResource(
    when (this) {
        MarketSession.TRADING -> R.string.market_session_trading
        MarketSession.LUNCH_BREAK -> R.string.market_session_lunch_break
        MarketSession.CLOSED -> R.string.market_session_closed
        MarketSession.HOLIDAY -> R.string.market_session_holiday
    }
)

@Composable
fun MarketTab.localizedLabel(): String = stringResource(
    when (this) {
        MarketTab.A_STOCK -> R.string.market_type_a_stock
        MarketTab.HK_STOCK -> R.string.market_type_hk_stock
        MarketTab.US_STOCK -> R.string.market_type_us_stock
        MarketTab.GOLD -> R.string.market_type_gold
    }
)

@Composable
fun SortOrder.localizedLabel(): String = stringResource(
    when (this) {
        SortOrder.DEFAULT -> R.string.sort_default
        SortOrder.CHANGE_PERCENT_DESC -> R.string.sort_change_percent_desc
        SortOrder.CHANGE_PERCENT_ASC -> R.string.sort_change_percent_asc
        SortOrder.EARNINGS_DESC -> R.string.sort_earnings_desc
        SortOrder.EARNINGS_ASC -> R.string.sort_earnings_asc
        SortOrder.MARKET_VALUE_DESC -> R.string.sort_market_value_desc
        SortOrder.MARKET_VALUE_ASC -> R.string.sort_market_value_asc
    }
)

@Composable
fun KLinePeriod.localizedLabel(): String = stringResource(
    when (this) {
        KLinePeriod.DAILY -> R.string.kline_daily
        KLinePeriod.WEEKLY -> R.string.kline_weekly
        KLinePeriod.MONTHLY -> R.string.kline_monthly
    }
)

@Composable
fun ChartRange.localizedLabel(): String = stringResource(
    when (this) {
        ChartRange.SEVEN_DAYS -> R.string.range_7_days
        ChartRange.THIRTY_DAYS -> R.string.range_30_days
        ChartRange.NINETY_DAYS -> R.string.range_90_days
        ChartRange.ALL -> R.string.range_all
    }
)

@Composable
fun AlertType.localizedLabel(): String = stringResource(
    when (this) {
        AlertType.PRICE_ABOVE -> R.string.alert_type_price_above
        AlertType.PRICE_BELOW -> R.string.alert_type_price_below
        AlertType.CHANGE_ABOVE -> R.string.alert_type_change_above
        AlertType.CHANGE_BELOW -> R.string.alert_type_change_below
        AlertType.VOLUME_ABOVE -> R.string.alert_type_volume_above
        AlertType.NEW_HIGH -> R.string.alert_type_new_high
        AlertType.NEW_LOW -> R.string.alert_type_new_low
        AlertType.MA_CROSS_ABOVE -> R.string.alert_type_ma_cross_above
        AlertType.MA_CROSS_BELOW -> R.string.alert_type_ma_cross_below
    }
)

@Composable
fun HoldingTradeAction.localizedLabel(): String = stringResource(
    when (this) {
        HoldingTradeAction.BUY -> R.string.trade_buy
        HoldingTradeAction.SELL -> R.string.trade_sell
    }
)

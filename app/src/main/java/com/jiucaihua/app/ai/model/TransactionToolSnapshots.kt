package com.jiucaihua.app.ai.model

import com.jiucaihua.app.domain.model.TransactionSummary

data class TransactionToolItem(
    val id: Long,
    val code: String?,
    val name: String?,
    val marketType: String?,
    val type: String,
    val tradeDate: Long,
    val quantity: Double,
    val price: Double,
    val amount: Double,
    val fee: Double,
    val tax: Double,
    val currency: String,
    val exchangeRate: Double,
    val amountCny: Double,
    val note: String?,
)

data class TransactionsToolSnapshot(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val transactions: List<TransactionToolItem>,
)

data class TransactionSummaryToolSnapshot(
    val buyAmountCny: Double,
    val sellAmountCny: Double,
    val netInvestmentCny: Double,
    val realizedPnlCny: Double,
    val dividendIncomeCny: Double,
    val feesCny: Double,
    val taxesCny: Double,
    val cashInCny: Double,
    val cashOutCny: Double,
    val tradeCount: Int,
    val buyCount: Int,
    val sellCount: Int,
    val firstTradeDate: Long?,
    val lastTradeDate: Long?,
)

data class HoldingTransactionHistorySnapshot(
    val code: String,
    val name: String,
    val marketType: String?,
    val currentHoldingShares: Double,
    val avgCostPrice: Double,
    val realizedPnlCny: Double,
    val unrealizedPnlCny: Double,
    val totalPnlCny: Double,
    val transactions: List<TransactionToolItem>,
)

data class PortfolioPerformanceToolSnapshot(
    val startDate: Long,
    val endDate: Long,
    val startValue: Double,
    val endValue: Double,
    val externalCashIn: Double,
    val externalCashOut: Double,
    val netExternalFlow: Double,
    val absoluteReturn: Double,
    val twr: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double,
    val dividendIncome: Double,
    val fees: Double,
    val taxes: Double,
    val cash: Double,
    val holdingsMarketValue: Double,
)

fun TransactionSummary.toToolSnapshot(): TransactionSummaryToolSnapshot {
    return TransactionSummaryToolSnapshot(
        buyAmountCny = buyAmountCny,
        sellAmountCny = sellAmountCny,
        netInvestmentCny = netInvestmentCny,
        realizedPnlCny = realizedPnlCny,
        dividendIncomeCny = dividendIncomeCny,
        feesCny = feesCny,
        taxesCny = taxesCny,
        cashInCny = cashInCny,
        cashOutCny = cashOutCny,
        tradeCount = tradeCount,
        buyCount = buyCount,
        sellCount = sellCount,
        firstTradeDate = firstTradeDate,
        lastTradeDate = lastTradeDate,
    )
}

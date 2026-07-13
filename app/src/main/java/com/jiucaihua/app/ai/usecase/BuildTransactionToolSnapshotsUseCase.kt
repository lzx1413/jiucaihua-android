package com.jiucaihua.app.ai.usecase

import com.jiucaihua.app.ai.model.HoldingTransactionHistorySnapshot
import com.jiucaihua.app.ai.model.PortfolioPerformanceToolSnapshot
import com.jiucaihua.app.ai.model.TransactionToolItem
import com.jiucaihua.app.ai.model.TransactionsToolSnapshot
import com.jiucaihua.app.ai.model.toToolSnapshot
import com.jiucaihua.app.domain.model.InvestmentTransaction
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.TransactionQuery
import com.jiucaihua.app.domain.model.TransactionType
import com.jiucaihua.app.domain.usecase.GetPortfolioPerformanceUseCase
import com.jiucaihua.app.domain.usecase.GetPortfolioUseCase
import com.jiucaihua.app.domain.usecase.GetTransactionSummaryUseCase
import com.jiucaihua.app.domain.usecase.GetTransactionsUseCase
import javax.inject.Inject

class BuildTransactionsToolSnapshotUseCase @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
) {
    suspend operator fun invoke(query: TransactionQuery): TransactionsToolSnapshot {
        val boundedQuery = query.copy(
            limit = query.limit.coerceIn(1, MAX_LIMIT),
            offset = query.offset.coerceAtLeast(0),
        )
        val (total, transactions) = getTransactionsUseCase(boundedQuery)
        return TransactionsToolSnapshot(
            total = total,
            limit = boundedQuery.limit,
            offset = boundedQuery.offset,
            transactions = transactions.map { it.toToolItem() },
        )
    }

    private companion object {
        const val MAX_LIMIT = 200
    }
}

class BuildTransactionSummaryToolSnapshotUseCase @Inject constructor(
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
) {
    suspend operator fun invoke(query: TransactionQuery) =
        getTransactionSummaryUseCase(query).toToolSnapshot()
}

class BuildHoldingTransactionHistorySnapshotUseCase @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getTransactionSummaryUseCase: GetTransactionSummaryUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
) {
    suspend operator fun invoke(code: String, marketType: MarketType?, limit: Int): HoldingTransactionHistorySnapshot {
        val portfolio = getPortfolioUseCase.getPortfolioWithQuotes()
        val holding = portfolio.holdings.firstOrNull {
            it.code == code && (marketType == null || it.marketType == marketType)
        }
        val effectiveMarketType = marketType ?: holding?.marketType
        val query = TransactionQuery(
            code = code,
            marketType = effectiveMarketType,
            limit = limit.coerceIn(1, MAX_LIMIT),
        )
        val (_, transactions) = getTransactionsUseCase(query)
        val transactionSummary = getTransactionSummaryUseCase(
            query.copy(limit = Int.MAX_VALUE, offset = 0),
        )
        val unrealizedPnl = holding?.earningsCNY ?: 0.0

        return HoldingTransactionHistorySnapshot(
            code = code,
            name = holding?.name ?: transactions.firstOrNull()?.name ?: code,
            marketType = effectiveMarketType?.name,
            currentHoldingShares = holding?.holdingShares ?: 0.0,
            avgCostPrice = holding?.costPrice ?: 0.0,
            realizedPnlCny = transactionSummary.realizedPnlCny,
            unrealizedPnlCny = unrealizedPnl,
            totalPnlCny = transactionSummary.realizedPnlCny + unrealizedPnl + transactionSummary.dividendIncomeCny - transactionSummary.feesCny - transactionSummary.taxesCny,
            transactions = transactions.map { it.toToolItem() },
        )
    }

    private companion object {
        const val MAX_LIMIT = 200
    }
}

class BuildPortfolioPerformanceToolSnapshotUseCase @Inject constructor(
    private val getPortfolioPerformanceUseCase: GetPortfolioPerformanceUseCase,
) {
    suspend operator fun invoke(from: Long?, to: Long?): PortfolioPerformanceToolSnapshot {
        val performance = getPortfolioPerformanceUseCase(from, to)
        return PortfolioPerformanceToolSnapshot(
            startDate = performance.startDate,
            endDate = performance.endDate,
            startValue = performance.startValue,
            endValue = performance.endValue,
            externalCashIn = performance.externalCashIn,
            externalCashOut = performance.externalCashOut,
            netExternalFlow = performance.netExternalFlow,
            absoluteReturn = performance.absoluteReturn,
            twr = performance.twr,
            realizedPnl = performance.realizedPnl,
            unrealizedPnl = performance.unrealizedPnl,
            dividendIncome = performance.dividendIncome,
            fees = performance.fees,
            taxes = performance.taxes,
            cash = performance.cash,
            holdingsMarketValue = performance.holdingsMarketValue,
        )
    }
}

fun InvestmentTransaction.toToolItem(): TransactionToolItem {
    return TransactionToolItem(
        id = id,
        code = code,
        name = name,
        marketType = marketType?.name,
        type = type.name,
        tradeDate = tradeDate,
        quantity = quantity,
        price = price,
        amount = amount,
        fee = fee,
        tax = tax,
        currency = currency,
        exchangeRate = exchangeRate,
        amountCny = amountCny,
        note = note,
    )
}

fun parseTransactionQuery(arguments: Map<String, Any?>): TransactionQuery {
    return TransactionQuery(
        code = (arguments["code"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        marketType = (arguments["market_type"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            MarketType.valueOf(it)
        },
        type = (arguments["type"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            TransactionType.valueOf(it)
        },
        from = (arguments["from"] as? Number)?.toLong(),
        to = (arguments["to"] as? Number)?.toLong(),
        limit = (arguments["limit"] as? Number)?.toInt() ?: 50,
        offset = (arguments["offset"] as? Number)?.toInt() ?: 0,
    )
}

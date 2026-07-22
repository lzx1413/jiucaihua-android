package com.jiucaihua.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReturnPeriod {
    DAY,
    MONTH,
    YEAR,
}

data class PortfolioPeriodReturn(
    val period: ReturnPeriod,
    val earnings: Double,
    val earningsPercent: Double,
)

/** Calculates period returns from daily asset snapshots while excluding external cash flows. */
object PortfolioPeriodReturnCalculator {
    private val shanghaiZone: ZoneId = ZoneId.of("Asia/Shanghai")

    fun calculate(
        snapshots: List<PortfolioSnapshot>,
        currentAssetValue: Double,
        transactions: List<InvestmentTransaction>,
        now: ZonedDateTime = ZonedDateTime.now(shanghaiZone),
    ): List<PortfolioPeriodReturn?> {
        val sortedSnapshots = snapshots.sortedBy { it.timestamp }
        return ReturnPeriod.entries.map { period ->
            val baseline = sortedSnapshots.lastOrNull { it.timestamp < periodStart(period, now) } ?: return@map null
            val baselineAssetValue = baseline.totalMarketValue + baseline.cash
            if (baselineAssetValue <= 0.0) return@map null

            val netExternalCashFlow = transactions.sumOf { transaction ->
                if (transaction.tradeDate <= baseline.timestamp || transaction.tradeDate > now.toInstant().toEpochMilli()) {
                    0.0
                } else {
                    when (transaction.type) {
                        TransactionType.CASH_IN -> transaction.amountCny
                        TransactionType.CASH_OUT -> -transaction.amountCny
                        else -> 0.0
                    }
                }
            }
            val earnings = currentAssetValue - baselineAssetValue - netExternalCashFlow
            PortfolioPeriodReturn(
                period = period,
                earnings = earnings,
                earningsPercent = earnings / baselineAssetValue * 100.0,
            )
        }
    }

    private fun periodStart(period: ReturnPeriod, now: ZonedDateTime): Long {
        val date = now.toLocalDate()
        val startDate = when (period) {
            ReturnPeriod.DAY -> date
            ReturnPeriod.MONTH -> date.withDayOfMonth(1)
            ReturnPeriod.YEAR -> date.withDayOfYear(1)
        }
        return startDate.atStartOfDay(shanghaiZone).toInstant().toEpochMilli()
    }
}

enum class ReturnHistoryType {
    DAILY,
    MONTHLY,
    YEARLY,
}

data class ReturnHistoryItem(
    val label: String,
    val earnings: Double,
    val earningsPercent: Double,
)

data class ReturnHistoryResult(
    val type: ReturnHistoryType,
    val selectorOptions: List<String>,
    val selectedOption: String?,
    val items: List<ReturnHistoryItem>,
)

/** Builds daily, monthly, and yearly return query results from end-of-day snapshots. */
object PortfolioReturnHistoryCalculator {
    private val shanghaiZone: ZoneId = ZoneId.of("Asia/Shanghai")

    fun availableOptions(snapshots: List<PortfolioSnapshot>, type: ReturnHistoryType): List<String> {
        return when (type) {
            ReturnHistoryType.DAILY -> snapshots.map { dateOf(it).toString().take(7) }.distinct().sortedDescending()
            ReturnHistoryType.MONTHLY -> snapshots.map { dateOf(it).year.toString() }.distinct().sortedDescending()
            ReturnHistoryType.YEARLY -> emptyList()
        }
    }

    fun calculate(
        snapshots: List<PortfolioSnapshot>,
        transactions: List<InvestmentTransaction>,
        type: ReturnHistoryType,
        selectedOption: String? = null,
    ): List<ReturnHistoryItem> {
        val sorted = snapshots.sortedBy { it.timestamp }
        return when (type) {
            ReturnHistoryType.DAILY -> {
                val month = selectedOption ?: return emptyList()
                sorted.filter { dateOf(it).toString().startsWith(month) }
                    .mapNotNull { end -> returnForPeriod(
                        snapshots = sorted,
                        transactions = transactions,
                        end = end,
                        startDate = dateOf(end),
                    )
                        ?.copy(label = dateOf(end).toString().substring(5)) }
            }
            ReturnHistoryType.MONTHLY -> {
                val year = selectedOption ?: return emptyList()
                sorted.groupBy { dateOf(it).toString().take(7) }
                    .filterKeys { it.startsWith(year) }
                    .toSortedMap()
                    .mapNotNull { (month, values) ->
                        returnForPeriod(sorted, transactions, values.maxBy { it.timestamp }, LocalDate.parse("${month}-01"))
                            ?.copy(label = month.substring(5) + "月")
                    }
            }
            ReturnHistoryType.YEARLY -> {
                sorted.groupBy { dateOf(it).year }
                    .toSortedMap()
                    .mapNotNull { (year, values) ->
                        returnForPeriod(sorted, transactions, values.maxBy { it.timestamp }, LocalDate.of(year, 1, 1))
                            ?.copy(label = "${year}年")
                    }
            }
        }.asReversed()
    }

    private fun returnForPeriod(
        snapshots: List<PortfolioSnapshot>,
        transactions: List<InvestmentTransaction>,
        end: PortfolioSnapshot,
        startDate: LocalDate,
    ): ReturnHistoryItem? {
        val startTimestamp = startDate.atStartOfDay(shanghaiZone).toInstant().toEpochMilli()
        val baseline = snapshots.lastOrNull { it.timestamp < startTimestamp } ?: return null
        val baselineAsset = baseline.totalMarketValue + baseline.cash
        if (baselineAsset <= 0.0) return null
        val netCashFlow = transactions.sumOf { transaction ->
            if (transaction.tradeDate <= baseline.timestamp || transaction.tradeDate > end.timestamp) 0.0
            else when (transaction.type) {
                TransactionType.CASH_IN -> transaction.amountCny
                TransactionType.CASH_OUT -> -transaction.amountCny
                else -> 0.0
            }
        }
        val endAsset = end.totalMarketValue + end.cash
        val earnings = endAsset - baselineAsset - netCashFlow
        return ReturnHistoryItem("", earnings, earnings / baselineAsset * 100.0)
    }

    private fun dateOf(snapshot: PortfolioSnapshot): LocalDate =
        Instant.ofEpochMilli(snapshot.timestamp).atZone(shanghaiZone).toLocalDate()
}

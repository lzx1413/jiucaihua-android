package com.jiucaihua.app.domain.model

data class KLineData(
    val code: String,
    val name: String,
    val period: KLinePeriod,
    val points: List<KLinePoint>,
)

enum class KLinePeriod(val label: String, val klt: Int) {
    DAILY("日K", 101),
    WEEKLY("周K", 102),
    MONTHLY("月K", 103),
}

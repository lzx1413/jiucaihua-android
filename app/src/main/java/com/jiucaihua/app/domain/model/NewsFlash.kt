package com.jiucaihua.app.domain.model

enum class NewsSource(val displayName: String) {
    STCN("人民财讯"),
    XUANGUBAO("选股宝"),
    CLS("财联社"),
    WALLSTREETCN("华尔街见闻"),
    JIN10("金十数据"),
    EASTMONEY("东方财富"),
    JIUYAN("韭研公社"),
}

enum class NewsTopic(val displayName: String, val sources: List<NewsSource>) {
    A_STOCK("A股", listOf(NewsSource.CLS, NewsSource.XUANGUBAO, NewsSource.EASTMONEY, NewsSource.STCN)),
    GLOBAL("国际宏观", listOf(NewsSource.WALLSTREETCN, NewsSource.JIN10)),
    FUTURES("期货商品", listOf(NewsSource.JIN10, NewsSource.EASTMONEY)),
    US_STOCK("美股", listOf(NewsSource.WALLSTREETCN)),
    FOREX("外汇", listOf(NewsSource.WALLSTREETCN, NewsSource.JIN10)),
}

data class NewsFlash(
    val id: Long,
    val title: String,
    val summary: String,
    val content: String,
    val impact: String,
    val source: String,
    val time: String,
    val sourceType: NewsSource,
    val epochMillis: Long = 0L,
    val detailUrl: String = "",
)

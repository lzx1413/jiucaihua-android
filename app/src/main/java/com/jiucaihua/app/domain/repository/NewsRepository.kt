package com.jiucaihua.app.domain.repository

import com.jiucaihua.app.domain.model.NewsFlash
import com.jiucaihua.app.domain.model.StockArticle

interface NewsRepository {
    suspend fun getMarketNews(limit: Int = 20): List<NewsFlash>
    suspend fun getStockNews(stockName: String, limit: Int = 10): List<StockArticle>
}

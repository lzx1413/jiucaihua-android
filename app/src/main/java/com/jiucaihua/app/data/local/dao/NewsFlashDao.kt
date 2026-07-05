package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.NewsFlashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsFlashDao {

    @Query("SELECT * FROM news_flash ORDER BY epochMillis DESC")
    fun getAll(): Flow<List<NewsFlashEntity>>

    @Query("SELECT * FROM news_flash WHERE epochMillis > :cutoff ORDER BY epochMillis DESC")
    suspend fun getAllOnce(cutoff: Long): List<NewsFlashEntity>

    @Query("SELECT * FROM news_flash ORDER BY epochMillis DESC")
    suspend fun getAllOnce(): List<NewsFlashEntity>

    @Query("SELECT * FROM news_flash WHERE sourceType = :sourceType ORDER BY epochMillis DESC")
    fun getBySourceType(sourceType: String): Flow<List<NewsFlashEntity>>

    @Query("SELECT * FROM news_flash WHERE epochMillis > :cutoff AND (title LIKE :query OR content LIKE :query OR summary LIKE :query) ORDER BY epochMillis DESC")
    suspend fun searchOnce(query: String, cutoff: Long): List<NewsFlashEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(news: List<NewsFlashEntity>)

    @Query("DELETE FROM news_flash")
    suspend fun clearAll()

    @Query("DELETE FROM news_flash WHERE epochMillis < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT * FROM news_flash WHERE isBookmarked = 1 ORDER BY epochMillis DESC")
    fun getBookmarked(): Flow<List<NewsFlashEntity>>

    @Query("SELECT * FROM news_flash WHERE isBookmarked = 1 ORDER BY epochMillis DESC")
    suspend fun getBookmarkedOnce(): List<NewsFlashEntity>

    @Query("UPDATE news_flash SET isBookmarked = :bookmarked WHERE newsId = :newsId AND sourceType = :sourceType")
    suspend fun setBookmarked(newsId: Long, sourceType: String, bookmarked: Boolean)
}

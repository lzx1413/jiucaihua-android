package com.jiucaihua.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jiucaihua.app.data.local.entity.NewsFlashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsFlashDao {

    @Query("SELECT * FROM news_flash WHERE epochMillis > :cutoff ORDER BY epochMillis DESC")
    fun getAll(cutoff: Long): Flow<List<NewsFlashEntity>>

    @Query("SELECT * FROM news_flash WHERE epochMillis > :cutoff ORDER BY epochMillis DESC")
    suspend fun getAllOnce(cutoff: Long): List<NewsFlashEntity>

    @Query("SELECT * FROM news_flash WHERE sourceType = :sourceType AND epochMillis > :cutoff ORDER BY epochMillis DESC")
    fun getBySourceType(sourceType: String, cutoff: Long): Flow<List<NewsFlashEntity>>

    @Query("SELECT * FROM news_flash WHERE epochMillis > :cutoff AND (title LIKE :query OR content LIKE :query OR summary LIKE :query) ORDER BY epochMillis DESC")
    suspend fun searchOnce(query: String, cutoff: Long): List<NewsFlashEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(news: List<NewsFlashEntity>)

    @Query("DELETE FROM news_flash WHERE epochMillis < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

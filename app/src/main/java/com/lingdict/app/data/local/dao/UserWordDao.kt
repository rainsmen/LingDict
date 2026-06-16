package com.lingdict.app.data.local.dao

import androidx.room.*
import com.lingdict.app.data.local.entity.UserWordEntity
import com.lingdict.app.data.local.entity.WordStatus
import kotlinx.coroutines.flow.Flow

/**
 * 用户生词库数据访问对象
 */
@Dao
interface UserWordDao {

    /**
     * 获取所有生词
     */
    @Query("SELECT * FROM user_words ORDER BY addedDate DESC")
    fun getAllUserWords(): Flow<List<UserWordEntity>>

    /**
     * 根据ID获取生词
     */
    @Query("SELECT * FROM user_words WHERE id = :id")
    suspend fun getUserWordById(id: Long): UserWordEntity?

    /**
     * 根据单词查询
     */
    @Query("SELECT * FROM user_words WHERE word = :word")
    suspend fun getUserWordByWord(word: String): UserWordEntity?

    /**
     * 获取待复习的单词（SM-2算法核心查询）
     * @param currentTime 当前时间戳
     * @param limit 返回数量限制
     */
    @Query("SELECT * FROM user_words WHERE nextReviewDate <= :currentTime AND status != 'MASTERED' ORDER BY nextReviewDate ASC LIMIT :limit")
    fun getDueWords(currentTime: Long, limit: Int = 20): Flow<List<UserWordEntity>>

    /**
     * 按状态查询
     */
    @Query("SELECT * FROM user_words WHERE status = :status ORDER BY addedDate DESC")
    fun getWordsByStatus(status: WordStatus): Flow<List<UserWordEntity>>

    /**
     * 获取收藏的单词
     */
    @Query("SELECT * FROM user_words WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoriteWords(): Flow<List<UserWordEntity>>

    /**
     * 获取今日新学单词
     */
    @Query("SELECT * FROM user_words WHERE addedDate >= :startOfDay ORDER BY addedDate DESC")
    fun getTodayNewWords(startOfDay: Long): Flow<List<UserWordEntity>>

    /**
     * 获取今日复习单词
     */
    @Query("SELECT * FROM user_words WHERE lastReviewDate >= :startOfDay ORDER BY lastReviewDate DESC")
    fun getTodayReviewedWords(startOfDay: Long): Flow<List<UserWordEntity>>

    /**
     * 统计各状态单词数量
     */
    @Query("SELECT status, COUNT(*) as count FROM user_words GROUP BY status")
    fun getStatusCounts(): Flow<Map<WordStatus, Int>>

    /**
     * 获取待复习单词数量
     */
    @Query("SELECT COUNT(*) FROM user_words WHERE nextReviewDate <= :currentTime AND status != 'MASTERED'")
    suspend fun getDueWordCount(currentTime: Long): Int

    /**
     * 插入生词
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWord(userWord: UserWordEntity): Long

    /**
     * 更新生词
     */
    @Update
    suspend fun updateUserWord(userWord: UserWordEntity)

    /**
     * 删除生词
     */
    @Delete
    suspend fun deleteUserWord(userWord: UserWordEntity)

    /**
     * 批量删除已掌握单词
     */
    @Query("DELETE FROM user_words WHERE status = 'MASTERED'")
    suspend fun deleteMasteredWords()

    /**
     * 检查单词是否已添加
     */
    @Query("SELECT EXISTS(SELECT 1 FROM user_words WHERE word = :word)")
    suspend fun isWordAdded(word: String): Boolean

    /**
     * 获取生词总数
     */
    @Query("SELECT COUNT(*) FROM user_words")
    suspend fun getUserWordCount(): Int

    /**
     * 获取连续学习天数（简化版：查询最近有记录的连续天数）
     */
    @Query("SELECT COUNT(DISTINCT date(addedDate / 1000, 'unixepoch')) FROM user_words WHERE addedDate >= :startDate")
    suspend fun getStudyStreakDays(startDate: Long): Int
}

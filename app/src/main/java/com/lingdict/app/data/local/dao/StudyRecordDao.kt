package com.lingdict.app.data.local.dao

import androidx.room.*
import com.lingdict.app.data.local.entity.StudyRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 学习记录数据访问对象
 */
@Dao
interface StudyRecordDao {

    /**
     * 获取指定日期的学习记录
     */
    @Query("SELECT * FROM study_records WHERE date = :date")
    suspend fun getRecordByDate(date: Long): StudyRecordEntity?

    /**
     * 获取日期范围内的学习记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    @Query("SELECT * FROM study_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<StudyRecordEntity>>

    /**
     * 获取最近N天的学习记录
     */
    @Query("SELECT * FROM study_records ORDER BY date DESC LIMIT :days")
    fun getRecentRecords(days: Int = 7): Flow<List<StudyRecordEntity>>

    /**
     * 获取所有学习记录
     */
    @Query("SELECT * FROM study_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<StudyRecordEntity>>

    /**
     * 获取有学习行为的日期，用于计算连续学习天数。
     */
    @Query("SELECT date FROM study_records WHERE wordsLearned > 0 OR wordsReviewed > 0 OR testTotal > 0 ORDER BY date DESC")
    suspend fun getStudyDatesDesc(): List<Long>

    /**
     * 插入或更新学习记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: StudyRecordEntity)

    /**
     * 更新学习记录
     */
    @Update
    suspend fun updateRecord(record: StudyRecordEntity)

    /**
     * 增加今日学习单词数
     */
    @Query("UPDATE study_records SET wordsLearned = wordsLearned + :count WHERE date = :date")
    suspend fun incrementWordsLearned(date: Long, count: Int = 1)

    /**
     * 增加今日复习单词数
     */
    @Query("UPDATE study_records SET wordsReviewed = wordsReviewed + :count WHERE date = :date")
    suspend fun incrementWordsReviewed(date: Long, count: Int = 1)

    /**
     * 增加今日测试数据
     */
    @Query("UPDATE study_records SET testCorrect = testCorrect + :correct, testTotal = testTotal + :total WHERE date = :date")
    suspend fun incrementTestData(date: Long, correct: Int, total: Int)

    /**
     * 获取总学习天数
     */
    @Query("SELECT COUNT(*) FROM study_records WHERE wordsLearned > 0 OR wordsReviewed > 0")
    suspend fun getTotalStudyDays(): Int

    /**
     * 获取总学习单词数
     */
    @Query("SELECT SUM(wordsLearned) FROM study_records")
    suspend fun getTotalWordsLearned(): Int?

    /**
     * 获取总复习单词数
     */
    @Query("SELECT SUM(wordsReviewed) FROM study_records")
    suspend fun getTotalWordsReviewed(): Int?

    /**
     * 删除指定日期之前的记录（数据清理）
     */
    @Query("DELETE FROM study_records WHERE date < :date")
    suspend fun deleteRecordsBefore(date: Long)
}

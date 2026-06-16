package com.lingdict.app.data.repository

import com.lingdict.app.data.local.dao.StudyRecordDao
import com.lingdict.app.data.local.entity.StudyRecordEntity
import com.lingdict.app.domain.model.DailyProgress
import com.lingdict.app.domain.model.DailyRecord
import com.lingdict.app.domain.model.MasteryDistribution
import com.lingdict.app.domain.model.StudyStatistics
import com.lingdict.app.domain.repository.StudyRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习记录仓库实现
 */
@Singleton
class StudyRecordRepositoryImpl @Inject constructor(
    private val studyRecordDao: StudyRecordDao
) : StudyRecordRepository {

    /**
     * 获取或创建今日学习记录
     */
    suspend fun getTodayRecord(): StudyRecordEntity {
        val today = getStartOfDay()
        return studyRecordDao.getRecordByDate(today) ?: run {
            val newRecord = StudyRecordEntity(date = today)
            studyRecordDao.insertRecord(newRecord)
            newRecord
        }
    }

    /**
     * 获取日期范围内的学习记录
     */
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<StudyRecordEntity>> {
        return studyRecordDao.getRecordsBetween(startDate, endDate)
    }

    /**
     * 获取最近N天的学习记录
     */
    fun getRecentRecords(days: Int = 7): Flow<List<StudyRecordEntity>> {
        return studyRecordDao.getRecentRecords(days)
    }

    /**
     * 记录学习了一个新单词
     */
    suspend fun recordWordLearned() {
        val today = getStartOfDay()
        val record = studyRecordDao.getRecordByDate(today)
        if (record != null) {
            studyRecordDao.incrementWordsLearned(today, 1)
        } else {
            studyRecordDao.insertRecord(
                StudyRecordEntity(
                    date = today,
                    wordsLearned = 1
                )
            )
        }
    }

    /**
     * 记录复习了一个单词
     */
    suspend fun recordWordReviewed() {
        val today = getStartOfDay()
        val record = studyRecordDao.getRecordByDate(today)
        if (record != null) {
            studyRecordDao.incrementWordsReviewed(today, 1)
        } else {
            studyRecordDao.insertRecord(
                StudyRecordEntity(
                    date = today,
                    wordsReviewed = 1
                )
            )
        }
    }

    /**
     * 记录测试结果
     */
    suspend fun recordTestResult(correct: Int, total: Int) {
        val today = getStartOfDay()
        val record = studyRecordDao.getRecordByDate(today)
        if (record != null) {
            studyRecordDao.incrementTestData(today, correct, total)
        } else {
            studyRecordDao.insertRecord(
                StudyRecordEntity(
                    date = today,
                    testCorrect = correct,
                    testTotal = total
                )
            )
        }
    }

    /**
     * 获取总学习天数
     */
    suspend fun getTotalStudyDays(): Int {
        return studyRecordDao.getTotalStudyDays()
    }

    /**
     * 获取总学习单词数
     */
    suspend fun getTotalWordsLearned(): Int {
        return studyRecordDao.getTotalWordsLearned() ?: 0
    }

    /**
     * 获取总复习单词数
     */
    suspend fun getTotalWordsReviewed(): Int {
        return studyRecordDao.getTotalWordsReviewed() ?: 0
    }

    /**
     * 实现接口：获取学习统计数据
     */
    override fun getStatistics(): Flow<StudyStatistics> = flow {
        val totalDays = getTotalStudyDays()
        val totalLearned = getTotalWordsLearned()
        val today = getTodayRecord()

        val stats = StudyStatistics(
            todayProgress = DailyProgress(
                wordsLearned = today.wordsLearned,
                wordsReviewed = today.wordsReviewed,
                dailyGoal = 20,
                completed = today.wordsLearned + today.wordsReviewed,
                accuracy = if (today.testTotal > 0) today.testCorrect.toFloat() / today.testTotal else 0f
            ),
            recentTrend = emptyList(),
            masteryDistribution = MasteryDistribution(0, 0, 0),
            studyStreak = 0,
            totalWordsLearned = totalLearned,
            totalStudyDays = totalDays
        )
        emit(stats)
    }

    /**
     * 实现接口：获取指定日期的学习记录
     */
    override suspend fun getRecordByDate(date: Long): StudyStatistics? {
        val record = studyRecordDao.getRecordByDate(date) ?: return null
        return StudyStatistics(
            todayProgress = DailyProgress(
                wordsLearned = record.wordsLearned,
                wordsReviewed = record.wordsReviewed,
                dailyGoal = 20,
                completed = record.wordsLearned + record.wordsReviewed,
                accuracy = if (record.testTotal > 0) record.testCorrect.toFloat() / record.testTotal else 0f
            ),
            recentTrend = listOf(
                DailyRecord(
                    date = record.date,
                    wordsLearned = record.wordsLearned,
                    wordsReviewed = record.wordsReviewed,
                    testCorrect = record.testCorrect,
                    testTotal = record.testTotal
                )
            ),
            masteryDistribution = MasteryDistribution(0, 0, 0),
            studyStreak = 0,
            totalWordsLearned = record.wordsLearned,
            totalStudyDays = 1
        )
    }

    /**
     * 实现接口：记录学习数据
     */
    override suspend fun recordStudy(wordsLearned: Int, wordsReviewed: Int): Result<Unit> {
        return try {
            val today = getStartOfDay()
            val record = studyRecordDao.getRecordByDate(today)
            if (record != null) {
                studyRecordDao.incrementWordsLearned(today, wordsLearned)
                studyRecordDao.incrementWordsReviewed(today, wordsReviewed)
            } else {
                studyRecordDao.insertRecord(
                    StudyRecordEntity(
                        date = today,
                        wordsLearned = wordsLearned,
                        wordsReviewed = wordsReviewed
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取今天开始的时间戳（00:00:00）
     * 修复：使用LocalDate处理时区问题
     */
    private fun getStartOfDay(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

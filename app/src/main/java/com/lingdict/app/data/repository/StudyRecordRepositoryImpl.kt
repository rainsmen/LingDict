package com.lingdict.app.data.repository

import com.lingdict.app.data.local.dao.StudyRecordDao
import com.lingdict.app.data.local.entity.StudyRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学习记录仓库实现
 */
@Singleton
class StudyRecordRepositoryImpl @Inject constructor(
    private val studyRecordDao: StudyRecordDao
) {

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
     * 获取今天开始的时间戳（00:00:00）
     */
    private fun getStartOfDay(): Long {
        val now = System.currentTimeMillis()
        return now - (now % (24 * 60 * 60 * 1000))
    }
}

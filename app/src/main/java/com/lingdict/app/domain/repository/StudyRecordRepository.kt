package com.lingdict.app.domain.repository

import com.lingdict.app.domain.model.StudyStatistics
import kotlinx.coroutines.flow.Flow

/**
 * 学习记录Repository接口
 */
interface StudyRecordRepository {
    /**
     * 获取学习统计数据
     */
    fun getStatistics(): Flow<StudyStatistics>

    /**
     * 获取指定日期的学习记录
     */
    suspend fun getRecordByDate(date: Long): StudyStatistics?

    /**
     * 记录学习数据
     */
    suspend fun recordStudy(wordsLearned: Int, wordsReviewed: Int): Result<Unit>
}

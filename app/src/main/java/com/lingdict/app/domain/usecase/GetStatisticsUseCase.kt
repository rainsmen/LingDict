package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.data.repository.StudyRecordRepositoryImpl
import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.domain.model.DailyProgress
import com.lingdict.app.domain.model.DailyRecord
import com.lingdict.app.domain.model.MasteryDistribution
import com.lingdict.app.domain.model.StudyStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取学习统计用例
 */
class GetStatisticsUseCase @Inject constructor(
    private val studyRecordRepository: StudyRecordRepositoryImpl,
    private val userWordRepository: UserWordRepositoryImpl
) {

    /**
     * 获取完整统计数据
     * @param dailyGoal 每日学习目标
     */
    suspend operator fun invoke(dailyGoal: Int = 20, trendDays: Int = 7): StudyStatistics {
        return StudyStatistics(
            todayProgress = getTodayProgress(dailyGoal),
            recentTrend = getRecentTrend(trendDays),
            masteryDistribution = getMasteryDistribution(),
            studyStreak = getStudyStreak(),
            totalWordsLearned = studyRecordRepository.getTotalWordsLearned(),
            totalStudyDays = studyRecordRepository.getTotalStudyDays()
        )
    }

    /**
     * 获取今日进度
     */
    suspend fun getTodayProgress(dailyGoal: Int = 20): DailyProgress {
        val todayRecord = studyRecordRepository.getTodayRecord()

        val accuracy = if (todayRecord.testTotal > 0) {
            todayRecord.testCorrect.toFloat() / todayRecord.testTotal
        } else {
            0f
        }

        return DailyProgress(
            wordsLearned = todayRecord.wordsLearned,
            wordsReviewed = todayRecord.wordsReviewed,
            dailyGoal = dailyGoal,
            completed = todayRecord.wordsLearned + todayRecord.wordsReviewed,
            accuracy = accuracy
        )
    }

    /**
     * 获取近期学习趋势
     * @param days 天数
     */
    suspend fun getRecentTrend(days: Int = 7): List<DailyRecord> {
        return studyRecordRepository.getRecentRecords(days).first().map { record ->
            DailyRecord(
                date = record.date,
                wordsLearned = record.wordsLearned,
                wordsReviewed = record.wordsReviewed,
                testCorrect = record.testCorrect,
                testTotal = record.testTotal
            )
        }
    }

    /**
     * 获取掌握度分布
     */
    suspend fun getMasteryDistribution(): MasteryDistribution {
        var newWords = 0
        var learningWords = 0
        var masteredWords = 0

        // 统计各状态单词数量
        val newWordsList = userWordRepository.getWordsByStatus(WordStatus.NEW).first()
        val learningWordsList = userWordRepository.getWordsByStatus(WordStatus.LEARNING).first()
        val masteredWordsList = userWordRepository.getWordsByStatus(WordStatus.MASTERED).first()

        newWords = newWordsList.size
        learningWords = learningWordsList.size
        masteredWords = masteredWordsList.size

        return MasteryDistribution(
            newWords = newWords,
            learningWords = learningWords,
            masteredWords = masteredWords
        )
    }

    /**
     * 获取连续学习天数
     */
    suspend fun getStudyStreak(): Int {
        return studyRecordRepository.getStudyStreakDays()
    }

    /**
     * 获取日期范围内的学习记录（用于热力图）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<DailyRecord>> {
        return studyRecordRepository.getRecordsBetween(startDate, endDate).map { records ->
            records.map { record ->
                DailyRecord(
                    date = record.date,
                    wordsLearned = record.wordsLearned,
                    wordsReviewed = record.wordsReviewed,
                    testCorrect = record.testCorrect,
                    testTotal = record.testTotal
                )
            }
        }
    }
}

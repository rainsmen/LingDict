package com.lingdict.app.domain.model

/**
 * 学习统计数据
 */
data class StudyStatistics(
    val todayProgress: DailyProgress,              // 今日进度
    val recentTrend: List<DailyRecord>,            // 近期趋势
    val masteryDistribution: MasteryDistribution,  // 掌握度分布
    val studyStreak: Int,                          // 连续学习天数
    val totalWordsLearned: Int,                    // 总学习单词数
    val totalStudyDays: Int                        // 总学习天数
)

/**
 * 每日进度
 */
data class DailyProgress(
    val wordsLearned: Int,      // 今日新学
    val wordsReviewed: Int,     // 今日复习
    val dailyGoal: Int,         // 每日目标
    val completed: Int,         // 完成数量（新学+复习）
    val accuracy: Float         // 今日测试正确率
) {
    /**
     * 进度百分比
     */
    fun getProgress(): Float {
        return if (dailyGoal > 0) {
            (completed.toFloat() / dailyGoal).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

/**
 * 每日学习记录
 */
data class DailyRecord(
    val date: Long,             // 日期时间戳
    val wordsLearned: Int,      // 新学单词数
    val wordsReviewed: Int,     // 复习单词数
    val testCorrect: Int,       // 测试正确数
    val testTotal: Int          // 测试总数
) {
    /**
     * 当日测试正确率
     */
    fun getAccuracy(): Float {
        return if (testTotal > 0) {
            testCorrect.toFloat() / testTotal
        } else {
            0f
        }
    }

    /**
     * 当日总学习量
     */
    fun getTotalWords(): Int {
        return wordsLearned + wordsReviewed
    }
}

/**
 * 掌握度分布
 */
data class MasteryDistribution(
    val newWords: Int,          // 新学单词数
    val learningWords: Int,     // 学习中单词数
    val masteredWords: Int      // 已掌握单词数
) {
    /**
     * 总单词数
     */
    fun getTotalWords(): Int {
        return newWords + learningWords + masteredWords
    }

    /**
     * 各部分占比
     */
    fun getNewWordsPercentage(): Float {
        val total = getTotalWords()
        return if (total > 0) newWords.toFloat() / total else 0f
    }

    fun getLearningWordsPercentage(): Float {
        val total = getTotalWords()
        return if (total > 0) learningWords.toFloat() / total else 0f
    }

    fun getMasteredWordsPercentage(): Float {
        val total = getTotalWords()
        return if (total > 0) masteredWords.toFloat() / total else 0f
    }
}

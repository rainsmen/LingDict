package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.constants.ReviewThresholds
import com.lingdict.app.domain.model.UserWord
import kotlin.math.max

/**
 * SM-2算法实现
 * SuperMemo-2 间隔重复算法
 *
 * 参考：https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
 */
object SM2Algorithm {

    /**
     * 计算下次复习时间
     *
     * @param userWord 当前用户单词
     * @param quality 回答质量 (0-5)
     *   5: 完美回答
     *   4: 正确但犹豫
     *   3: 正确但困难
     *   2: 错误但记得
     *   1: 错误且模糊
     *   0: 完全不记得
     * @return 更新后的用户单词
     */
    fun calculateNextReview(userWord: UserWord, quality: Int): UserWord {
        // 确保quality在0-5范围内
        val q = quality.coerceIn(0, 5)

        // 计算新的难度因子 (EF)
        val newEaseFactor = calculateEaseFactor(userWord.easeFactor, q)

        // 根据质量决定间隔和重复次数
        val (newInterval, newRepetitions) = when {
            q < 3 -> {
                // 回答质量低于3，重置学习进度
                1 to 0
            }
            userWord.repetitions == 0 -> {
                // 第一次正确回答
                1 to 1
            }
            userWord.repetitions == 1 -> {
                // 第二次正确回答
                6 to 2
            }
            else -> {
                // 第三次及以后，间隔按EF倍增
                val calculatedInterval = (userWord.interval * newEaseFactor).toInt()
                // 限制最大间隔为365天，防止溢出
                minOf(calculatedInterval, ReviewThresholds.MAX_INTERVAL_DAYS) to (userWord.repetitions + 1)
            }
        }

        // 计算下次复习时间戳，防止溢出
        val safeInterval = minOf(newInterval, ReviewThresholds.MAX_INTERVAL_DAYS)
        val nextReviewDate = System.currentTimeMillis() + safeInterval * 24 * 60 * 60 * 1000L

        // 确定学习状态
        val newStatus = determineStatus(newRepetitions, userWord.knownCount, userWord.testCorrectCount, userWord.testTotalCount)

        return userWord.copy(
            easeFactor = newEaseFactor,
            interval = newInterval,
            repetitions = newRepetitions,
            nextReviewDate = nextReviewDate,
            lastReviewDate = System.currentTimeMillis(),
            status = newStatus
        )
    }

    /**
     * 计算新的难度因子
     * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     * EF最小为1.3
     */
    private fun calculateEaseFactor(currentEF: Float, quality: Int): Float {
        val delta = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
        val newEF = currentEF + delta
        return max(1.3f, newEF)
    }

    /**
     * 确定学习状态
     */
    private fun determineStatus(repetitions: Int, knownCount: Int, testCorrect: Int, testTotal: Int): WordStatus {
        return when {
            // 已掌握：重复次数>=5 且 标记认识>=3次 且 测试正确率>=75%
            repetitions >= ReviewThresholds.MASTERED_REPETITIONS
                && knownCount >= ReviewThresholds.KNOWN_COUNT_THRESHOLD
                && testTotal >= ReviewThresholds.MIN_TEST_COUNT
                && (testCorrect.toFloat() / testTotal) >= ReviewThresholds.TEST_ACCURACY_THRESHOLD -> {
                WordStatus.MASTERED
            }
            // 学习中：至少复习过一次
            repetitions > 0 -> {
                WordStatus.LEARNING
            }
            // 新学：还没开始复习
            else -> {
                WordStatus.NEW
            }
        }
    }

    /**
     * 根据用户标记"认识/不认识"计算质量
     */
    fun qualityFromUserMark(isKnown: Boolean): Int {
        return if (isKnown) 4 else 1
    }

    /**
     * 根据测试结果计算质量
     */
    fun qualityFromTestResult(isCorrect: Boolean, hesitation: Boolean = false): Int {
        return when {
            isCorrect && !hesitation -> 5  // 完美
            isCorrect && hesitation -> 4   // 正确但犹豫
            else -> 2                      // 错误但有印象
        }
    }
}

package com.lingdict.app.domain.model

import com.lingdict.app.data.local.entity.WordStatus

/**
 * 用户生词领域模型
 */
data class UserWord(
    val id: Long = 0,
    val word: Word,                         // 单词详情
    val addedDate: Long,                    // 添加时间
    val lastReviewDate: Long? = null,       // 最后复习时间
    val nextReviewDate: Long,               // 下次复习时间

    // SM-2算法参数
    val easeFactor: Float = 2.5f,           // 难度因子
    val interval: Int = 1,                  // 复习间隔（天）
    val repetitions: Int = 0,               // 连续正确次数

    // 学习状态
    val status: WordStatus,                 // 学习状态
    val knownCount: Int = 0,                // 标记"认识"的次数
    val unknownCount: Int = 0,              // 标记"不认识"的次数
    val testCorrectCount: Int = 0,          // 测试正确次数
    val testTotalCount: Int = 0,            // 测试总次数

    // 其他
    val isFavorite: Boolean = false,        // 是否收藏
    val notes: String? = null               // 个人笔记
) {
    /**
     * 是否到期需要复习
     */
    fun isDue(): Boolean {
        return System.currentTimeMillis() >= nextReviewDate && status != WordStatus.MASTERED
    }

    /**
     * 是否已掌握
     * 规则：标记"认识">=3次 且 测试正确率>=75%
     */
    fun isMastered(): Boolean {
        return knownCount >= 3 &&
               testTotalCount >= 3 &&
               testCorrectCount.toFloat() / testTotalCount >= 0.75f
    }

    /**
     * 测试正确率
     */
    fun getAccuracy(): Float {
        return if (testTotalCount > 0) {
            testCorrectCount.toFloat() / testTotalCount
        } else {
            0f
        }
    }
}

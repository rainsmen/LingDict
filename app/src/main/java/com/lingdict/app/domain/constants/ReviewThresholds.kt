package com.lingdict.app.domain.constants

/**
 * SM-2算法复习阈值常量
 */
object ReviewThresholds {
    /** 掌握所需的最少重复次数 */
    const val MASTERED_REPETITIONS = 5

    /** 掌握所需的最少"认识"标记次数 */
    const val KNOWN_COUNT_THRESHOLD = 3

    /** 掌握所需的最少测试次数 */
    const val MIN_TEST_COUNT = 3

    /** 掌握所需的测试正确率阈值 */
    const val TEST_ACCURACY_THRESHOLD = 0.75f

    /** 最大复习间隔（天） */
    const val MAX_INTERVAL_DAYS = 365
}

package com.lingdict.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户生词库表 - 存储用户添加的生词及学习进度
 * 实现SM-2算法的间隔重复学习
 */
@Entity(
    tableName = "user_words",
    indices = [
        Index(value = ["word"]),
        Index(value = ["nextReviewDate"]),
        Index(value = ["status"])
    ]
)
data class UserWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,                       // 单词
    val addedDate: Long = System.currentTimeMillis(),  // 添加时间
    val lastReviewDate: Long? = null,       // 最后复习时间
    val nextReviewDate: Long = System.currentTimeMillis(),  // 下次复习时间

    // SM-2算法参数
    val easeFactor: Float = 2.5f,           // 难度因子 (1.3 - 2.5)
    val interval: Int = 1,                  // 复习间隔（天）
    val repetitions: Int = 0,               // 连续正确次数

    // 学习状态
    val status: WordStatus = WordStatus.NEW,  // 学习状态
    val knownCount: Int = 0,                // 标记"认识"的次数
    val unknownCount: Int = 0,              // 标记"不认识"的次数
    val testCorrectCount: Int = 0,          // 测试正确次数
    val testTotalCount: Int = 0,            // 测试总次数

    // 其他
    val isFavorite: Boolean = false,        // 是否收藏
    val notes: String? = null               // 个人笔记
)

/**
 * 单词学习状态枚举
 */
enum class WordStatus {
    NEW,        // 新学：刚添加，未开始学习
    LEARNING,   // 学习中：正在间隔重复学习
    MASTERED    // 已掌握：连续3次标记认识且3次测试通过
}

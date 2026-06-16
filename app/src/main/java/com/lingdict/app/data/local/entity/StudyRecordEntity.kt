package com.lingdict.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 学习记录表 - 记录每日学习数据，用于统计分析
 */
@Entity(
    tableName = "study_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,                     // 日期 (yyyy-MM-dd 转为时间戳，零点)
    val wordsLearned: Int = 0,          // 新学单词数
    val wordsReviewed: Int = 0,         // 复习单词数
    val testCorrect: Int = 0,           // 测试正确数
    val testTotal: Int = 0,             // 测试总数
    val studyDuration: Long = 0,        // 学习时长（毫秒）
    val createdAt: Long = System.currentTimeMillis()  // 记录创建时间
)

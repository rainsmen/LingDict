package com.lingdict.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 例句表 - 存储单词的例句
 */
@Entity(
    tableName = "examples",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["word"],
            childColumns = ["word"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["word"])]
)
data class ExampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,               // 关联的单词
    val sentenceEn: String,         // 英文例句
    val sentenceZh: String,         // 中文翻译
    val source: String? = null,     // 来源 (如：VOA, BBC等)
    val audioUrl: String? = null    // 例句音频URL（如果有）
)

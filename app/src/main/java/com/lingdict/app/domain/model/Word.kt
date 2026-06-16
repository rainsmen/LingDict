package com.lingdict.app.domain.model

/**
 * 单词领域模型
 */
data class Word(
    val word: String,                   // 单词
    val phonetic: String? = null,       // 音标
    val phoneticUs: String? = null,     // 美式音标
    val phoneticUk: String? = null,     // 英式音标
    val definition: String,             // 英文释义
    val translation: String,            // 中文翻译
    val level: String? = null,          // 难度等级
    val examples: List<Example> = emptyList(),  // 例句
    val imageUrl: String? = null        // 助记图片URL
)

/**
 * 例句
 */
data class Example(
    val sentenceEn: String,             // 英文例句
    val sentenceZh: String,             // 中文翻译
    val audioUrl: String? = null        // 音频URL
)

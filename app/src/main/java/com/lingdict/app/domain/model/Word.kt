package com.lingdict.app.domain.model

/**
 * 单词领域模型
 *
 * 从ECDICT词库或有道API获取的单词详细信息
 */
data class Word(
    /** 单词原文 */
    val word: String,

    /** 音标（通用） */
    val phonetic: String? = null,

    /** 英文释义 */
    val definition: String? = null,

    /** 中文翻译 */
    val translation: String? = null,

    /** 难度等级（如 CET4、CET6、TOEFL） */
    val level: String? = null,

    /** 词性 */
    val pos: String? = null,

    /** 柯林斯星级 */
    val collins: Int? = null,

    /** 牛津词典标记 */
    val oxford: Boolean? = null,

    /** 标签（如zk/gk等） */
    val tag: String? = null,

    /** BNC词频 */
    val bnc: Int? = null,

    /** 词频 */
    val frq: Int? = null,

    /** 时态复数等变换 */
    val exchange: String? = null,

    /** 详细释义 */
    val detail: String? = null,

    /** 发音音频URL */
    val audio: String? = null,

    /** 助记图片URL */
    val imageUrl: String? = null
)

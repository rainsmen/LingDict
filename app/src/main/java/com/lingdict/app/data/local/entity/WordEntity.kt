package com.lingdict.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 词典库表 - 存储所有单词的基础信息
 * 来源：ECDICT离线词库 + 有道API在线查询结果
 *
 * 注意：预置数据库使用 id 作为主键，但为了查询性能，这里使用 word 作为主键
 * 预置数据库字段：id, word, phonetic, definition, translation, level, frequency
 * 扩展字段（可为空）：phoneticUs, phoneticUk, exchange, collins, bnc, frq, tag, pos, oxford, detail, audio
 */
@Entity(
    tableName = "words",
    indices = [Index(value = ["word"])]
)
data class WordEntity(
    @PrimaryKey
    val word: String,                   // 单词
    val phonetic: String? = null,       // 音标 (IPA格式)
    val phoneticUs: String? = null,     // 美式音标
    val phoneticUk: String? = null,     // 英式音标
    val definition: String,             // 英文释义
    val translation: String,            // 中文翻译
    val level: String? = null,          // 难度等级 (CET4, CET6, TOEFL, IELTS, GRE等)
    val frequency: Int = 0,             // 词频 (COCA/BNC排名)
    val exchange: String? = null,       // 词形变化 (过去式、复数等)
    val collins: Int? = null,           // 柯林斯星级 (0-5)
    val bnc: Int? = null,               // BNC词频排名
    val frq: Int? = null,               // 当代语料库词频
    val tag: String? = null,            // 词性标签
    val addedTime: Long = System.currentTimeMillis(),  // 添加时间

    // 以下字段为 WordRepositoryImpl.toDomainModel() 所需，预置数据库中不存在
    val pos: String? = null,            // 词性 (part of speech)
    val oxford: Boolean? = null,        // 牛津词典标记
    val detail: String? = null,         // 详细释义
    val audio: String? = null           // 发音音频URL
)

package com.lingdict.app.domain.repository

import com.lingdict.app.domain.model.Word
import kotlinx.coroutines.flow.Flow

/**
 * 词典库Repository接口
 */
interface WordRepository {
    /**
     * 搜索单词（用于自动补全）
     */
    fun searchWords(query: String): Flow<List<String>>

    /**
     * 获取单词详情
     */
    suspend fun getWord(word: String): Word?

    /**
     * 获取随机单词（用于测试干扰项）
     */
    suspend fun getRandomWords(count: Int): List<Word>
}

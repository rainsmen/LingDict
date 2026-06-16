package com.lingdict.app.domain.repository

/**
 * Pexels图片Repository接口
 */
interface PexelsRepository {
    /**
     * 搜索单词配图（带降级策略）
     * @param word 单词
     * @param translation 翻译（用于降级搜索）
     * @return 图片URL，失败返回null
     */
    suspend fun searchWordImageWithFallback(word: String, translation: String?): String?
}

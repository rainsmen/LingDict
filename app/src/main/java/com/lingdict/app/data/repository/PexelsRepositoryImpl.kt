package com.lingdict.app.data.repository

import com.lingdict.app.BuildConfig
import com.lingdict.app.data.remote.PexelsApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pexels图片仓库实现
 */
@Singleton
class PexelsRepositoryImpl @Inject constructor(
    private val pexelsApi: PexelsApiService
) {

    /**
     * 搜索单词相关图片
     * @param word 单词
     * @return 图片URL，失败返回null
     */
    suspend fun searchWordImage(word: String): String? {
        return try {
            val response = pexelsApi.searchPhotos(
                query = word,
                perPage = 1,
                apiKey = BuildConfig.PEXELS_API_KEY
            )

            if (response.photos.isNotEmpty()) {
                // 返回中等尺寸图片
                response.photos.first().src.medium
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 搜索单词图片（带翻译关键词）
     * 优先使用英文单词搜索，失败则尝试中文翻译
     */
    suspend fun searchWordImageWithFallback(word: String, translation: String?): String? {
        // 先用英文单词搜索
        var imageUrl = searchWordImage(word)

        // 如果失败且有中文翻译，尝试用中文搜索
        if (imageUrl == null && !translation.isNullOrBlank()) {
            imageUrl = searchWordImage(translation)
        }

        return imageUrl
    }
}

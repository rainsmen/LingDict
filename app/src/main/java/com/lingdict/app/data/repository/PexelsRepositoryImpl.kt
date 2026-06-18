package com.lingdict.app.data.repository

import com.lingdict.app.BuildConfig
import com.lingdict.app.data.datastore.SettingsDataStore
import com.lingdict.app.data.remote.PexelsApiService
import com.lingdict.app.domain.repository.PexelsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pexels图片仓库实现
 */
@Singleton
class PexelsRepositoryImpl @Inject constructor(
    private val pexelsApi: PexelsApiService,
    private val settingsDataStore: SettingsDataStore
) : PexelsRepository {

    /**
     * 实现接口：搜索单词图片（带翻译关键词）
     * 优先使用英文单词搜索，失败则尝试中文翻译
     */
    override suspend fun searchWordImageWithFallback(word: String, translation: String?): String? {
        // 先用英文单词搜索
        var imageUrl = searchWordImage(word)

        // 如果失败且有中文翻译，尝试用中文搜索
        if (imageUrl == null && !translation.isNullOrBlank()) {
            imageUrl = searchWordImage(translation)
        }

        return imageUrl
    }

    /**
     * 内部方法：搜索单词相关图片
     * @param word 单词
     * @return 图片URL，失败返回null
     */
    private suspend fun searchWordImage(word: String): String? {
        val apiKey = settingsDataStore.userSettingsFlow.first().pexelsApiKey.ifBlank { BuildConfig.PEXELS_API_KEY }
        if (apiKey.isBlank()) return null

        return try {
            val response = pexelsApi.searchPhotos(
                query = word,
                perPage = 1,
                apiKey = apiKey
            )

            if (response.photos.isNotEmpty()) {
                // 返回中等尺寸图片
                response.photos.first().src.medium
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

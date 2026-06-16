package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.PexelsRepository
import com.lingdict.app.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 搜索单词用例
 */
class SearchWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val pexelsRepository: PexelsRepository
) {

    /**
     * 搜索单词（用于自动补全）
     */
    operator fun invoke(query: String): Flow<List<Word>> {
        if (query.isBlank()) {
            return flowOf(emptyList())
        }
        return wordRepository.searchWords(query)
    }

    /**
     * 获取单词详情（含图片）
     */
    suspend fun getWordDetail(word: String): Result<Word> {
        // 1. 获取单词基本信息
        val wordDetails = wordRepository.getWord(word)
            ?: return Result.failure(Exception("单词不存在"))

        // 2. 获取助记图片
        val imageUrl = pexelsRepository.searchWordImageWithFallback(
            word = wordDetails.word,
            translation = wordDetails.translation
        )

        // 3. 返回带图片的单词
        return Result.success(wordDetails.copy(imageUrl = imageUrl))
    }
}

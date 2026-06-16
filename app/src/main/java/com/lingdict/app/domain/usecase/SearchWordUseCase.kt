package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordEntity
import com.lingdict.app.data.repository.PexelsRepositoryImpl
import com.lingdict.app.data.repository.WordRepositoryImpl
import com.lingdict.app.domain.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 搜索单词用例
 */
class SearchWordUseCase @Inject constructor(
    private val wordRepository: WordRepositoryImpl,
    private val pexelsRepository: PexelsRepositoryImpl
) {

    /**
     * 搜索单词（用于自动补全）
     */
    operator fun invoke(query: String, limit: Int = 10): Flow<List<String>> {
        if (query.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return wordRepository.searchWords(query, limit).map { words ->
            words.map { it.word }
        }
    }

    /**
     * 获取单词详情（含图片）
     */
    suspend fun getWordDetail(word: String): Result<Word> {
        // 1. 获取单词基本信息
        val wordResult = wordRepository.getWord(word)
        if (wordResult.isFailure) {
            return Result.failure(wordResult.exceptionOrNull() ?: Exception("获取单词失败"))
        }

        val wordEntity = wordResult.getOrNull() ?: return Result.failure(Exception("单词不存在"))

        // 2. 获取助记图片
        val imageUrl = pexelsRepository.searchWordImageWithFallback(
            word = wordEntity.word,
            translation = wordEntity.translation
        )

        // 3. 转换为领域模型
        return Result.success(wordEntity.toDomainModel(imageUrl))
    }

    /**
     * 模糊搜索
     */
    fun fuzzySearch(query: String, limit: Int = 20): Flow<List<Word>> {
        if (query.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return wordRepository.fuzzySearch(query, limit).map { words ->
            words.map { it.toDomainModel() }
        }
    }

    /**
     * Entity转领域模型
     */
    private fun WordEntity.toDomainModel(imageUrl: String? = null): Word {
        return Word(
            word = word,
            phonetic = phonetic,
            phoneticUs = phoneticUs,
            phoneticUk = phoneticUk,
            definition = definition,
            translation = translation,
            level = level,
            imageUrl = imageUrl
        )
    }
}

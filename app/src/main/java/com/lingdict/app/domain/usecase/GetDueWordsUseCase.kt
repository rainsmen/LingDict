package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.repository.UserWordRepository
import com.lingdict.app.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取待复习单词用例
 */
class GetDueWordsUseCase @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val wordRepository: WordRepository
) {

    /**
     * 获取待复习的单词列表
     * @param limit 返回数量限制
     */
    operator fun invoke(limit: Int = 20): Flow<List<UserWord>> {
        return userWordRepository.getDueWords(limit).map { userWords ->
            userWords.mapNotNull { enrichWithWordDetails(it) }
        }
    }

    /**
     * 丰富UserWord的单词详情
     */
    private suspend fun enrichWithWordDetails(userWord: UserWord): UserWord? {
        val wordDetails = wordRepository.getWord(userWord.word.word) ?: return null
        return userWord.copy(word = wordDetails)
    }
}

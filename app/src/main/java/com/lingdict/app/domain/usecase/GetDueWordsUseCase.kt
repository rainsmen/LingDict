package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.UserWordEntity
import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.data.repository.WordRepositoryImpl
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取待复习单词用例
 */
class GetDueWordsUseCase @Inject constructor(
    private val userWordRepository: UserWordRepositoryImpl,
    private val wordRepository: WordRepositoryImpl
) {

    /**
     * 获取待复习的单词列表
     * @param limit 返回数量限制
     */
    operator fun invoke(limit: Int = 20): Flow<List<UserWord>> {
        return userWordRepository.getDueWords(limit).map { userWords ->
            userWords.mapNotNull { it.toDomainModel() }
        }
    }

    /**
     * 获取待复习单词数量
     */
    suspend fun getDueWordCount(): Int {
        return userWordRepository.getDueWordCount()
    }

    /**
     * Entity转领域模型
     */
    private suspend fun UserWordEntity.toDomainModel(): UserWord? {
        // 获取单词详情
        val wordResult = wordRepository.getWord(word)
        if (wordResult.isFailure) {
            return null
        }

        val wordEntity = wordResult.getOrNull() ?: return null

        return UserWord(
            id = id,
            word = Word(
                word = wordEntity.word,
                phonetic = wordEntity.phonetic,
                phoneticUs = wordEntity.phoneticUs,
                phoneticUk = wordEntity.phoneticUk,
                definition = wordEntity.definition,
                translation = wordEntity.translation,
                level = wordEntity.level
            ),
            addedDate = addedDate,
            lastReviewDate = lastReviewDate,
            nextReviewDate = nextReviewDate,
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            status = status,
            knownCount = knownCount,
            unknownCount = unknownCount,
            testCorrectCount = testCorrectCount,
            testTotalCount = testTotalCount,
            isFavorite = isFavorite,
            notes = notes
        )
    }
}

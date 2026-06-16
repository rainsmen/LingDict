package com.lingdict.app.domain.usecase

import com.lingdict.app.data.repository.StudyRecordRepositoryImpl
import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.domain.model.UserWord
import javax.inject.Inject

/**
 * 更新复习记录用例（核心：SM-2算法）
 */
class UpdateReviewUseCase @Inject constructor(
    private val userWordRepository: UserWordRepositoryImpl,
    private val studyRecordRepository: StudyRecordRepositoryImpl
) {

    /**
     * 标记单词为"认识"
     * @param userWord 用户单词
     */
    suspend fun markAsKnown(userWord: UserWord): Result<Unit> {
        // 1. 使用SM-2算法计算下次复习时间
        val quality = SM2Algorithm.qualityFromUserMark(isKnown = true)
        val updatedWord = SM2Algorithm.calculateNextReview(userWord, quality)

        // 2. 更新knownCount
        val wordToSave = updatedWord.copy(
            knownCount = updatedWord.knownCount + 1
        )

        // 3. 保存到数据库
        val entity = wordToSave.toEntity()
        val result = userWordRepository.updateUserWord(entity)

        // 4. 记录复习统计
        if (result.isSuccess) {
            studyRecordRepository.recordWordReviewed()
        }

        return result
    }

    /**
     * 标记单词为"不认识"
     * @param userWord 用户单词
     */
    suspend fun markAsUnknown(userWord: UserWord): Result<Unit> {
        // 1. 使用SM-2算法计算下次复习时间（质量较低，会重置进度）
        val quality = SM2Algorithm.qualityFromUserMark(isKnown = false)
        val updatedWord = SM2Algorithm.calculateNextReview(userWord, quality)

        // 2. 更新unknownCount
        val wordToSave = updatedWord.copy(
            unknownCount = updatedWord.unknownCount + 1
        )

        // 3. 保存到数据库
        val entity = wordToSave.toEntity()
        val result = userWordRepository.updateUserWord(entity)

        // 4. 记录复习统计
        if (result.isSuccess) {
            studyRecordRepository.recordWordReviewed()
        }

        return result
    }

    /**
     * 记录测试结果
     * @param userWord 用户单词
     * @param isCorrect 是否正确
     */
    suspend fun recordTestResult(userWord: UserWord, isCorrect: Boolean): Result<Unit> {
        // 1. 使用SM-2算法计算
        val quality = SM2Algorithm.qualityFromTestResult(isCorrect)
        val updatedWord = SM2Algorithm.calculateNextReview(userWord, quality)

        // 2. 更新测试统计
        val wordToSave = updatedWord.copy(
            testTotalCount = updatedWord.testTotalCount + 1,
            testCorrectCount = if (isCorrect) updatedWord.testCorrectCount + 1 else updatedWord.testCorrectCount
        )

        // 3. 保存到数据库
        val entity = wordToSave.toEntity()
        return userWordRepository.updateUserWord(entity)
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(wordId: Long): Result<Unit> {
        return userWordRepository.toggleFavorite(wordId)
    }

    /**
     * 领域模型转Entity
     */
    private fun UserWord.toEntity(): com.lingdict.app.data.local.entity.UserWordEntity {
        return com.lingdict.app.data.local.entity.UserWordEntity(
            id = id,
            word = word.word,
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

    /**
     * 操作符重载，支持简化调用
     * @param userWordId 用户单词ID
     * @param quality SM-2质量评分（0-5）
     */
    suspend operator fun invoke(userWordId: Long, quality: Int): Result<Unit> {
        // 1. 获取UserWord
        val userWord = userWordRepository.getUserWord(userWordId)
            ?: return Result.failure(Exception("单词不存在"))

        // 2. 根据quality调用对应方法
        return when {
            quality >= 4 -> markAsKnown(userWord)
            quality >= 2 -> markAsKnown(userWord) // 中等质量也算认识
            else -> markAsUnknown(userWord)
        }
    }
}

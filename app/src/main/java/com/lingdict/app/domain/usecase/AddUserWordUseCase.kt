package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.repository.StudyRecordRepository
import com.lingdict.app.domain.repository.UserWordRepository
import javax.inject.Inject

/**
 * 添加生词用例
 */
class AddUserWordUseCase @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val studyRecordRepository: StudyRecordRepository
) {

    /**
     * 添加单词到生词库
     * @param word 单词
     * @return 成功返回生词ID，失败返回错误信息
     */
    suspend operator fun invoke(word: String): Result<Unit> {
        // 1. 添加到生词库
        val result = userWordRepository.addUserWord(word)

        if (result.isSuccess) {
            // 2. 记录到学习统计
            studyRecordRepository.recordStudy(wordsLearned = 1, wordsReviewed = 0)
        }

        return result
    }

    /**
     * 检查单词是否已在生词库中
     */
    suspend fun isWordAdded(word: String): Boolean {
        return userWordRepository.isWordAdded(word)
    }
}

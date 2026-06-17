package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.data.repository.StudyRecordRepositoryImpl
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.UserWordRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UpdateReviewUseCaseTest {

    @Test
    fun `mark as known can immediately promote word to mastered`() = runTest {
        val repository = CapturingUserWordRepository()
        val studyRecordRepository = mockk<StudyRecordRepositoryImpl>()
        coEvery { studyRecordRepository.recordWordReviewed() } returns Unit
        val useCase = UpdateReviewUseCase(repository, studyRecordRepository)

        useCase.markAsKnown(
            userWord(
                repetitions = 4,
                knownCount = 2,
                testCorrectCount = 3,
                testTotalCount = 3
            )
        )

        assertNotNull(repository.updatedWord)
        assertEquals(3, repository.updatedWord?.knownCount)
        assertEquals(WordStatus.MASTERED, repository.updatedWord?.status)
    }

    private fun userWord(
        repetitions: Int,
        knownCount: Int,
        testCorrectCount: Int,
        testTotalCount: Int
    ) = UserWord(
        id = 1,
        word = Word(word = "test", translation = "测试"),
        addedDate = 1L,
        nextReviewDate = 1L,
        repetitions = repetitions,
        interval = 6,
        status = WordStatus.LEARNING,
        knownCount = knownCount,
        testCorrectCount = testCorrectCount,
        testTotalCount = testTotalCount
    )

    private class CapturingUserWordRepository : UserWordRepository {
        var updatedWord: UserWord? = null

        override fun getDueWords(limit: Int): Flow<List<UserWord>> = flowOf(emptyList())
        override suspend fun addUserWord(word: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateReview(userWord: UserWord): Result<Unit> {
            updatedWord = userWord
            return Result.success(Unit)
        }
        override suspend fun getUserWord(id: Long): UserWord? = updatedWord
        override fun getAllUserWords(): Flow<List<UserWord>> = flowOf(emptyList())
        override suspend fun isWordAdded(word: String): Boolean = false
    }
}

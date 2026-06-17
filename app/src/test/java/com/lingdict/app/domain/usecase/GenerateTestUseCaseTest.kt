package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.constants.QuestionTypes
import com.lingdict.app.domain.model.Question
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.UserWordRepository
import com.lingdict.app.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateTestUseCaseTest {

    @Test
    fun `multiple choice uses enriched word details`() = runTest {
        val useCase = GenerateTestUseCase(
            userWordRepository = FakeUserWordRepository(
                dueWords = listOf(userWord("test"))
            ),
            wordRepository = FakeWordRepository(
                details = mapOf("test" to Word(word = "test", translation = "测试")),
                randomWords = listOf(
                    Word(word = "one", translation = "一"),
                    Word(word = "two", translation = "二"),
                    Word(word = "three", translation = "三")
                )
            )
        )

        val question = useCase(QuestionTypes.MULTIPLE_CHOICE, 1).single() as Question.MultipleChoice

        assertEquals("测试", question.correctAnswer)
        assertTrue(question.options.contains("测试"))
    }

    @Test
    fun `test generation skips words without dictionary details`() = runTest {
        val useCase = GenerateTestUseCase(
            userWordRepository = FakeUserWordRepository(
                dueWords = listOf(userWord("missing"))
            ),
            wordRepository = FakeWordRepository(details = emptyMap())
        )

        assertTrue(useCase(QuestionTypes.MULTIPLE_CHOICE, 1).isEmpty())
    }

    private fun userWord(word: String) = UserWord(
        id = 1,
        word = Word(word = word),
        addedDate = 1L,
        nextReviewDate = 1L,
        status = WordStatus.LEARNING
    )

    private class FakeUserWordRepository(
        private val dueWords: List<UserWord>
    ) : UserWordRepository {
        override fun getDueWords(limit: Int): Flow<List<UserWord>> = flowOf(dueWords.take(limit))
        override suspend fun addUserWord(word: String): Result<Unit> = Result.success(Unit)
        override suspend fun updateReview(userWord: UserWord): Result<Unit> = Result.success(Unit)
        override suspend fun getUserWord(id: Long): UserWord? = dueWords.firstOrNull { it.id == id }
        override fun getAllUserWords(): Flow<List<UserWord>> = flowOf(dueWords)
        override suspend fun isWordAdded(word: String): Boolean = dueWords.any { it.word.word == word }
    }

    private class FakeWordRepository(
        private val details: Map<String, Word>,
        private val randomWords: List<Word> = emptyList()
    ) : WordRepository {
        override fun searchWords(query: String): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWord(word: String): Word? = details[word]
        override suspend fun getRandomWords(count: Int): List<Word> = randomWords.take(count)
    }
}

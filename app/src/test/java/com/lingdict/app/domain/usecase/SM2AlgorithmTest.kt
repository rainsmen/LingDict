package com.lingdict.app.domain.usecase

import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import org.junit.Assert.*
import org.junit.Test

class SM2AlgorithmTest {

    private fun userWord(
        repetitions: Int = 0,
        interval: Int = 1,
        easeFactor: Float = 2.5f,
        knownCount: Int = 0,
        testCorrectCount: Int = 0,
        testTotalCount: Int = 0
    ) = UserWord(
        id = 1,
        word = Word(word = "test", translation = "测试"),
        addedDate = System.currentTimeMillis(),
        nextReviewDate = System.currentTimeMillis(),
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        status = WordStatus.NEW,
        knownCount = knownCount,
        testCorrectCount = testCorrectCount,
        testTotalCount = testTotalCount
    )

    @Test
    fun `low quality resets repetitions and interval`() {
        val result = SM2Algorithm.calculateNextReview(
            userWord(repetitions = 5, interval = 10),
            quality = 1
        )

        assertEquals(1, result.interval)
        assertEquals(0, result.repetitions)
        assertTrue(result.easeFactor >= 1.3f)
        assertEquals(WordStatus.NEW, result.status)
    }

    @Test
    fun `first successful review schedules one day`() {
        val result = SM2Algorithm.calculateNextReview(userWord(), quality = 4)

        assertEquals(1, result.interval)
        assertEquals(1, result.repetitions)
        assertEquals(WordStatus.LEARNING, result.status)
    }

    @Test
    fun `second successful review schedules six days`() {
        val result = SM2Algorithm.calculateNextReview(
            userWord(repetitions = 1, interval = 1),
            quality = 4
        )

        assertEquals(6, result.interval)
        assertEquals(2, result.repetitions)
    }

    @Test
    fun `quality is coerced into valid range`() {
        val result = SM2Algorithm.calculateNextReview(
            userWord(repetitions = 2, interval = 6),
            quality = 10
        )

        assertEquals(3, result.repetitions)
        assertTrue(result.interval > 6)
    }
}

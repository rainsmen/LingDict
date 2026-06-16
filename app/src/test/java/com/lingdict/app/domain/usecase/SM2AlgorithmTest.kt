package com.lingdict.app.domain.usecase

import org.junit.Assert.*
import org.junit.Test

class SM2AlgorithmTest {

    private val algorithm = SM2Algorithm()

    @Test
    fun `calculateNextReview with quality 0 should reset to initial values`() {
        // Given
        val easeFactor = 2.5f
        val interval = 10
        val repetitions = 5

        // When
        val result = algorithm.calculateNextReview(0, easeFactor, interval, repetitions)

        // Then
        assertEquals(1.3f, result.easeFactor, 0.01f) // Minimum ease factor
        assertEquals(1, result.interval)
        assertEquals(0, result.repetitions)
    }

    @Test
    fun `calculateNextReview with quality 1 should reset to initial values`() {
        // Given
        val easeFactor = 2.5f
        val interval = 10
        val repetitions = 5

        // When
        val result = algorithm.calculateNextReview(1, easeFactor, interval, repetitions)

        // Then
        assertEquals(1.3f, result.easeFactor, 0.01f)
        assertEquals(1, result.interval)
        assertEquals(0, result.repetitions)
    }

    @Test
    fun `calculateNextReview with quality 2 should reset repetitions but keep ease factor`() {
        // Given
        val easeFactor = 2.5f
        val interval = 10
        val repetitions = 5

        // When
        val result = algorithm.calculateNextReview(2, easeFactor, interval, repetitions)

        // Then
        assertTrue(result.easeFactor < easeFactor) // Ease factor decreased
        assertTrue(result.easeFactor >= 1.3f) // But not below minimum
        assertEquals(1, result.interval)
        assertEquals(0, result.repetitions)
    }

    @Test
    fun `calculateNextReview with quality 3 should maintain interval and increment repetitions`() {
        // Given
        val easeFactor = 2.5f
        val interval = 6
        val repetitions = 2

        // When
        val result = algorithm.calculateNextReview(3, easeFactor, interval, repetitions)

        // Then
        assertTrue(result.easeFactor >= easeFactor) // Ease factor increased or same
        assertTrue(result.interval >= interval) // Interval increased or same
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `calculateNextReview with quality 4 should increase interval`() {
        // Given
        val easeFactor = 2.5f
        val interval = 6
        val repetitions = 2

        // When
        val result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)

        // Then
        assertTrue(result.easeFactor > easeFactor) // Ease factor increased
        assertTrue(result.interval > interval) // Interval increased
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `calculateNextReview with quality 5 should maximize increase`() {
        // Given
        val easeFactor = 2.5f
        val interval = 6
        val repetitions = 2

        // When
        val result = algorithm.calculateNextReview(5, easeFactor, interval, repetitions)

        // Then
        assertTrue(result.easeFactor > easeFactor) // Ease factor increased
        assertTrue(result.interval > interval) // Interval increased
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `calculateNextReview first repetition should return interval 1`() {
        // Given
        val easeFactor = 2.5f
        val interval = 1
        val repetitions = 0

        // When
        val result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)

        // Then
        assertEquals(1, result.interval)
        assertEquals(1, result.repetitions)
    }

    @Test
    fun `calculateNextReview second repetition should return interval 6`() {
        // Given
        val easeFactor = 2.5f
        val interval = 1
        val repetitions = 1

        // When
        val result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)

        // Then
        assertEquals(6, result.interval)
        assertEquals(2, result.repetitions)
    }

    @Test
    fun `calculateNextReview third and beyond should multiply by ease factor`() {
        // Given
        val easeFactor = 2.5f
        val interval = 6
        val repetitions = 2

        // When
        val result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)

        // Then
        assertTrue(result.interval > 6) // Should be 6 * easeFactor
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `calculateNextReview ease factor should not go below 1_3`() {
        // Given - start with minimum ease factor
        val easeFactor = 1.3f
        val interval = 1
        val repetitions = 0

        // When - use quality 0 which decreases ease factor
        val result = algorithm.calculateNextReview(0, easeFactor, interval, repetitions)

        // Then
        assertEquals(1.3f, result.easeFactor, 0.01f) // Should stay at minimum
    }

    @Test
    fun `calculateNextReview with invalid quality below 0 should treat as 0`() {
        // Given
        val easeFactor = 2.5f
        val interval = 10
        val repetitions = 5

        // When
        val result = algorithm.calculateNextReview(-1, easeFactor, interval, repetitions)

        // Then - should behave like quality 0
        assertEquals(1.3f, result.easeFactor, 0.01f)
        assertEquals(1, result.interval)
        assertEquals(0, result.repetitions)
    }

    @Test
    fun `calculateNextReview with invalid quality above 5 should treat as 5`() {
        // Given
        val easeFactor = 2.5f
        val interval = 6
        val repetitions = 2

        // When
        val result = algorithm.calculateNextReview(10, easeFactor, interval, repetitions)

        // Then - should behave like quality 5
        assertTrue(result.easeFactor >= easeFactor)
        assertTrue(result.interval > interval)
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `calculateNextReview multiple iterations should maintain consistency`() {
        // Given - simulate multiple good reviews
        var easeFactor = 2.5f
        var interval = 1
        var repetitions = 0

        // When - perform 5 successful reviews
        repeat(5) {
            val result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)
            easeFactor = result.easeFactor
            interval = result.interval
            repetitions = result.repetitions
        }

        // Then - values should be reasonable
        assertTrue(easeFactor >= 2.5f) // Should have increased
        assertTrue(interval > 6) // Should have grown significantly
        assertEquals(5, repetitions)
    }

    @Test
    fun `calculateNextReview with alternating quality should show realistic pattern`() {
        // Given
        var easeFactor = 2.5f
        var interval = 1
        var repetitions = 0

        // When - good, bad, good pattern
        var result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)
        easeFactor = result.easeFactor
        interval = result.interval
        repetitions = result.repetitions

        result = algorithm.calculateNextReview(2, easeFactor, interval, repetitions)
        easeFactor = result.easeFactor
        interval = result.interval
        repetitions = result.repetitions

        result = algorithm.calculateNextReview(4, easeFactor, interval, repetitions)

        // Then - should show learning pattern
        assertTrue(result.easeFactor < 2.5f) // Decreased due to one bad review
        assertTrue(result.interval >= 1) // Reset but progressing
    }
}

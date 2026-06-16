package com.lingdict.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lingdict.app.data.local.entity.UserWordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserWordDaoTest {

    private lateinit var database: LingDictDatabase
    private lateinit var userWordDao: UserWordDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            LingDictDatabase::class.java
        ).allowMainThreadQueries().build()

        userWordDao = database.userWordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUserWord_and_getById() = runTest {
        // Given
        val word = UserWordEntity(
            id = 1,
            word = "test",
            phonetic = "/test/",
            definition = "A procedure",
            translation = "n. 测试",
            level = "CET4",
            addedDate = System.currentTimeMillis(),
            lastReviewDate = null,
            nextReviewDate = System.currentTimeMillis() + 86400000,
            easeFactor = 2.5f,
            interval = 1,
            repetitions = 0,
            status = "LEARNING"
        )

        // When
        userWordDao.insert(word)
        val result = userWordDao.getById(1).first()

        // Then
        assertNotNull(result)
        assertEquals("test", result?.word)
        assertEquals(2.5f, result?.easeFactor)
    }

    @Test
    fun getAllUserWords_returns_all_words() = runTest {
        // Given
        val words = listOf(
            UserWordEntity(
                id = 1, word = "test", phonetic = "/test/", definition = "A",
                translation = "n. 测试", level = "CET4",
                addedDate = System.currentTimeMillis(),
                lastReviewDate = null,
                nextReviewDate = System.currentTimeMillis(),
                easeFactor = 2.5f, interval = 1, repetitions = 0, status = "NEW"
            ),
            UserWordEntity(
                id = 2, word = "example", phonetic = "/ex/", definition = "B",
                translation = "n. 例子", level = "CET4",
                addedDate = System.currentTimeMillis(),
                lastReviewDate = null,
                nextReviewDate = System.currentTimeMillis(),
                easeFactor = 2.5f, interval = 1, repetitions = 0, status = "LEARNING"
            )
        )

        // When
        words.forEach { userWordDao.insert(it) }
        val result = userWordDao.getAllUserWords().first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getDueWords_returns_only_due_words() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val words = listOf(
            UserWordEntity(
                id = 1, word = "due", phonetic = "/due/", definition = "A",
                translation = "到期的", level = "CET4",
                addedDate = now,
                lastReviewDate = null,
                nextReviewDate = now - 1000, // Due (past)
                easeFactor = 2.5f, interval = 1, repetitions = 0, status = "LEARNING"
            ),
            UserWordEntity(
                id = 2, word = "future", phonetic = "/future/", definition = "B",
                translation = "未来的", level = "CET4",
                addedDate = now,
                lastReviewDate = null,
                nextReviewDate = now + 86400000, // Not due (future)
                easeFactor = 2.5f, interval = 1, repetitions = 0, status = "LEARNING"
            )
        )

        // When
        words.forEach { userWordDao.insert(it) }
        val result = userWordDao.getDueWords(now, 10).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("due", result[0].word)
    }

    @Test
    fun update_modifies_existing_word() = runTest {
        // Given
        val word = UserWordEntity(
            id = 1, word = "test", phonetic = "/test/", definition = "A",
            translation = "n. 测试", level = "CET4",
            addedDate = System.currentTimeMillis(),
            lastReviewDate = null,
            nextReviewDate = System.currentTimeMillis(),
            easeFactor = 2.5f, interval = 1, repetitions = 0, status = "NEW"
        )
        userWordDao.insert(word)

        // When
        val updated = word.copy(
            easeFactor = 2.8f,
            interval = 6,
            repetitions = 1,
            status = "LEARNING"
        )
        userWordDao.update(updated)

        // Then
        val result = userWordDao.getById(1).first()
        assertEquals(2.8f, result?.easeFactor)
        assertEquals(6, result?.interval)
        assertEquals(1, result?.repetitions)
        assertEquals("LEARNING", result?.status)
    }

    @Test
    fun delete_removes_word() = runTest {
        // Given
        val word = UserWordEntity(
            id = 1, word = "test", phonetic = "/test/", definition = "A",
            translation = "n. 测试", level = "CET4",
            addedDate = System.currentTimeMillis(),
            lastReviewDate = null,
            nextReviewDate = System.currentTimeMillis(),
            easeFactor = 2.5f, interval = 1, repetitions = 0, status = "NEW"
        )
        userWordDao.insert(word)

        // When
        userWordDao.delete(word)

        // Then
        val result = userWordDao.getById(1).first()
        assertNull(result)
    }

    @Test
    fun getWordByText_finds_existing_word() = runTest {
        // Given
        val word = UserWordEntity(
            id = 1, word = "test", phonetic = "/test/", definition = "A",
            translation = "n. 测试", level = "CET4",
            addedDate = System.currentTimeMillis(),
            lastReviewDate = null,
            nextReviewDate = System.currentTimeMillis(),
            easeFactor = 2.5f, interval = 1, repetitions = 0, status = "NEW"
        )
        userWordDao.insert(word)

        // When
        val result = userWordDao.getWordByText("test").first()

        // Then
        assertNotNull(result)
        assertEquals("test", result?.word)
    }

    @Test
    fun getWordByText_returns_null_for_nonexistent_word() = runTest {
        // When
        val result = userWordDao.getWordByText("nonexistent").first()

        // Then
        assertNull(result)
    }

    @Test
    fun getDueWords_respects_limit() = runTest {
        // Given - insert 10 due words
        val now = System.currentTimeMillis()
        repeat(10) { index ->
            val word = UserWordEntity(
                id = index.toLong(),
                word = "word$index",
                phonetic = "/word/",
                definition = "Definition",
                translation = "单词",
                level = "CET4",
                addedDate = now,
                lastReviewDate = null,
                nextReviewDate = now - 1000, // All due
                easeFactor = 2.5f,
                interval = 1,
                repetitions = 0,
                status = "LEARNING"
            )
            userWordDao.insert(word)
        }

        // When - request only 5
        val result = userWordDao.getDueWords(now, 5).first()

        // Then
        assertEquals(5, result.size)
    }
}

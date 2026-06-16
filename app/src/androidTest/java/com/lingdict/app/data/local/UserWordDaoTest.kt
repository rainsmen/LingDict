package com.lingdict.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lingdict.app.data.local.dao.UserWordDao
import com.lingdict.app.data.local.entity.UserWordEntity
import com.lingdict.app.data.local.entity.WordStatus
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
        database = Room.inMemoryDatabaseBuilder(context, LingDictDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userWordDao = database.userWordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun word(
        id: Long = 0,
        text: String = "test",
        nextReviewDate: Long = System.currentTimeMillis(),
        status: WordStatus = WordStatus.NEW
    ) = UserWordEntity(
        id = id,
        word = text,
        addedDate = System.currentTimeMillis(),
        nextReviewDate = nextReviewDate,
        status = status
    )

    @Test
    fun insertUserWord_and_getById() = runTest {
        val id = userWordDao.insertUserWord(word(text = "test"))

        val result = userWordDao.getUserWordById(id)

        assertNotNull(result)
        assertEquals("test", result?.word)
    }

    @Test
    fun getAllUserWords_returns_all_words() = runTest {
        userWordDao.insertUserWord(word(text = "test"))
        userWordDao.insertUserWord(word(text = "example"))

        val result = userWordDao.getAllUserWords().first()

        assertEquals(2, result.size)
    }

    @Test
    fun getDueWords_returns_only_due_words() = runTest {
        val now = System.currentTimeMillis()
        userWordDao.insertUserWord(word(text = "due", nextReviewDate = now - 1000, status = WordStatus.LEARNING))
        userWordDao.insertUserWord(word(text = "future", nextReviewDate = now + 86400000, status = WordStatus.LEARNING))

        val result = userWordDao.getDueWords(now, 10).first()

        assertEquals(1, result.size)
        assertEquals("due", result[0].word)
    }

    @Test
    fun updateUserWord_modifies_existing_word() = runTest {
        val id = userWordDao.insertUserWord(word(text = "test"))
        val original = userWordDao.getUserWordById(id)!!

        userWordDao.updateUserWord(original.copy(status = WordStatus.LEARNING, repetitions = 1))

        val result = userWordDao.getUserWordById(id)
        assertEquals(WordStatus.LEARNING, result?.status)
        assertEquals(1, result?.repetitions)
    }

    @Test
    fun getUserWordByWord_finds_existing_word() = runTest {
        userWordDao.insertUserWord(word(text = "test"))

        val result = userWordDao.getUserWordByWord("test")

        assertNotNull(result)
        assertEquals("test", result?.word)
    }
}

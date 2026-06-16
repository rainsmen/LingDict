package com.lingdict.app.data.repository

import com.lingdict.app.data.local.dao.UserWordDao
import com.lingdict.app.data.local.entity.UserWordEntity
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.UserWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户生词库仓库实现
 */
@Singleton
class UserWordRepositoryImpl @Inject constructor(
    private val userWordDao: UserWordDao
) : UserWordRepository {

    /**
     * 实现接口：获取待复习的单词
     */
    override fun getDueWords(limit: Int): Flow<List<UserWord>> {
        val currentTime = System.currentTimeMillis()
        return userWordDao.getDueWords(currentTime, limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * 实现接口：添加生词
     */
    override suspend fun addUserWord(word: String): Result<Unit> {
        return addUserWordInternal(word).map { Unit }
    }

    /**
     * 实现接口：更新单词复习记录
     */
    override suspend fun updateReview(userWord: UserWord): Result<Unit> {
        return updateUserWord(userWord.toEntity())
    }

    /**
     * 实现接口：根据ID获取单词
     */
    override suspend fun getUserWord(id: Long): UserWord? {
        return getUserWordById(id)?.toDomainModel()
    }

    /**
     * 实现接口：获取所有生词
     */
    override fun getAllUserWords(): Flow<List<UserWord>> {
        return getAllUserWordsInternal().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * 内部方法：获取所有生词
     */
    private fun getAllUserWordsInternal(): Flow<List<UserWordEntity>> {
        return userWordDao.getAllUserWords()
    }

    /**
     * 根据ID获取生词
     */
    suspend fun getUserWordById(id: Long): UserWordEntity? {
        return userWordDao.getUserWordById(id)
    }

    /**
     * 根据单词查询
     */
    suspend fun getUserWordByWord(word: String): UserWordEntity? {
        return userWordDao.getUserWordByWord(word)
    }

    /**
     * 内部方法：获取待复习的单词（返回Entity）
     */
    private fun getDueWordsInternal(limit: Int = 20): Flow<List<UserWordEntity>> {
        val currentTime = System.currentTimeMillis()
        return userWordDao.getDueWords(currentTime, limit)
    }

    /**
     * 按状态查询
     */
    fun getWordsByStatus(status: WordStatus): Flow<List<UserWordEntity>> {
        return userWordDao.getWordsByStatus(status)
    }

    /**
     * 获取收藏的单词
     */
    fun getFavoriteWords(): Flow<List<UserWordEntity>> {
        return userWordDao.getFavoriteWords()
    }

    /**
     * 获取今日新学单词
     */
    fun getTodayNewWords(): Flow<List<UserWordEntity>> {
        val startOfDay = getStartOfDay()
        return userWordDao.getTodayNewWords(startOfDay)
    }

    /**
     * 获取今日复习单词
     */
    fun getTodayReviewedWords(): Flow<List<UserWordEntity>> {
        val startOfDay = getStartOfDay()
        return userWordDao.getTodayReviewedWords(startOfDay)
    }

    /**
     * 获取待复习单词数量
     */
    suspend fun getDueWordCount(): Int {
        val currentTime = System.currentTimeMillis()
        return userWordDao.getDueWordCount(currentTime)
    }

    /**
     * 内部方法：添加生词
     */
    private suspend fun addUserWordInternal(word: String): Result<Long> {
        return try {
            // 检查是否已添加
            if (userWordDao.isWordAdded(word)) {
                return Result.failure(Exception("单词已在生词库中"))
            }

            val userWord = UserWordEntity(
                word = word,
                addedDate = System.currentTimeMillis(),
                nextReviewDate = System.currentTimeMillis(),
                status = WordStatus.NEW
            )

            val id = userWordDao.insertUserWord(userWord)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新生词
     */
    suspend fun updateUserWord(userWord: UserWordEntity): Result<Unit> {
        return try {
            userWordDao.updateUserWord(userWord)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除生词
     */
    suspend fun deleteUserWord(userWord: UserWordEntity): Result<Unit> {
        return try {
            userWordDao.deleteUserWord(userWord)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 标记单词为"认识"
     */
    suspend fun markAsKnown(wordId: Long): Result<Unit> {
        return try {
            val userWord = userWordDao.getUserWordById(wordId) ?: return Result.failure(Exception("单词不存在"))
            val updated = userWord.copy(
                knownCount = userWord.knownCount + 1,
                lastReviewDate = System.currentTimeMillis()
            )
            userWordDao.updateUserWord(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 标记单词为"不认识"
     */
    suspend fun markAsUnknown(wordId: Long): Result<Unit> {
        return try {
            val userWord = userWordDao.getUserWordById(wordId) ?: return Result.failure(Exception("单词不存在"))
            val updated = userWord.copy(
                unknownCount = userWord.unknownCount + 1,
                lastReviewDate = System.currentTimeMillis()
            )
            userWordDao.updateUserWord(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(wordId: Long): Result<Unit> {
        return try {
            val userWord = userWordDao.getUserWordById(wordId) ?: return Result.failure(Exception("单词不存在"))
            val updated = userWord.copy(isFavorite = !userWord.isFavorite)
            userWordDao.updateUserWord(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查单词是否已添加
     */
    override suspend fun isWordAdded(word: String): Boolean {
        return userWordDao.isWordAdded(word)
    }

    /**
     * 获取生词总数
     */
    suspend fun getUserWordCount(): Int {
        return userWordDao.getUserWordCount()
    }

    /**
     * 获取连续学习天数
     */
    suspend fun getStudyStreakDays(): Int {
        // 简化实现：从30天前开始统计
        val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        return userWordDao.getStudyStreakDays(thirtyDaysAgo)
    }

    /**
     * 获取今天开始的时间戳（00:00:00）
     * 修复：使用LocalDate处理时区问题
     */
    private fun getStartOfDay(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Domain模型转Entity
     */
    private fun UserWord.toEntity(): UserWordEntity {
        return UserWordEntity(
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
     * Entity转Domain模型
     */
    private fun UserWordEntity.toDomainModel(): UserWord {
        return UserWord(
            id = id,
            word = Word(
                word = word,
                phonetic = null,
                definition = null,
                translation = null,
                level = null,
                pos = null,
                collins = null,
                oxford = null,
                tag = null,
                bnc = null,
                frq = null,
                exchange = null,
                detail = null,
                audio = null
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

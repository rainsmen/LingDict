package com.lingdict.app.domain.repository

import com.lingdict.app.domain.model.UserWord
import kotlinx.coroutines.flow.Flow

/**
 * 用户生词库Repository接口
 */
interface UserWordRepository {
    /**
     * 获取待复习单词
     */
    fun getDueWords(limit: Int): Flow<List<UserWord>>

    /**
     * 添加生词
     */
    suspend fun addUserWord(word: String): Result<Unit>

    /**
     * 更新单词复习记录
     */
    suspend fun updateReview(userWord: UserWord): Result<Unit>

    /**
     * 根据ID获取单词
     */
    suspend fun getUserWord(id: Long): UserWord?

    /**
     * 获取所有生词
     */
    fun getAllUserWords(): Flow<List<UserWord>>
}

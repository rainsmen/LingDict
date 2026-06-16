package com.lingdict.app.data.repository

import com.lingdict.app.BuildConfig
import com.lingdict.app.data.local.dao.WordDao
import com.lingdict.app.data.local.entity.WordEntity
import com.lingdict.app.data.mapper.toWordEntity
import com.lingdict.app.data.remote.YoudaoApiService
import com.lingdict.app.util.YoudaoSignUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 词典仓库实现
 * 混合数据源策略：优先查询本地ECDICT，未找到则调用有道API
 */
@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val youdaoApi: YoudaoApiService
) {

    /**
     * 搜索单词（用于自动补全）
     */
    fun searchWords(query: String, limit: Int = 10): Flow<List<WordEntity>> {
        return wordDao.searchWords(query, limit)
    }

    /**
     * 获取单词详情（混合数据源）
     * 1. 先查本地数据库
     * 2. 未找到则调用有道API
     * 3. 将API结果缓存到本地
     */
    suspend fun getWord(word: String): Result<WordEntity> {
        return try {
            // 1. 查询本地数据库
            val localWord = wordDao.getWord(word)
            if (localWord != null) {
                return Result.success(localWord)
            }

            // 2. 调用有道API
            val salt = YoudaoSignUtil.generateSalt()
            val curtime = YoudaoSignUtil.getCurrentTime()
            val sign = YoudaoSignUtil.generateSign(
                appKey = BuildConfig.YOUDAO_APP_KEY,
                appSecret = BuildConfig.YOUDAO_APP_SECRET,
                query = word,
                salt = salt,
                curtime = curtime
            )

            val response = youdaoApi.translate(
                query = word,
                appKey = BuildConfig.YOUDAO_APP_KEY,
                salt = salt,
                sign = sign,
                curtime = curtime
            )

            // 3. 转换并缓存
            val wordEntity = response.toWordEntity()
            if (wordEntity != null) {
                wordDao.insertWord(wordEntity)
                Result.success(wordEntity)
            } else {
                Result.failure(Exception("单词未找到或API返回错误"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 模糊搜索
     */
    fun fuzzySearch(query: String, limit: Int = 20): Flow<List<WordEntity>> {
        return wordDao.fuzzySearch(query, limit)
    }

    /**
     * 按难度等级查询
     */
    fun getWordsByLevel(level: String): Flow<List<WordEntity>> {
        return wordDao.getWordsByLevel(level)
    }

    /**
     * 获取随机单词（用于生成测试题干扰项）
     */
    suspend fun getRandomWords(count: Int, excludeWord: String? = null): List<WordEntity> {
        val words = wordDao.getRandomWords(count + 1) // 多获取一个以防排除
        return if (excludeWord != null) {
            words.filter { it.word != excludeWord }.take(count)
        } else {
            words.take(count)
        }
    }

    /**
     * 批量导入单词（用于ECDICT初始化）
     */
    suspend fun importWords(words: List<WordEntity>) {
        wordDao.insertWords(words)
    }

    /**
     * 获取词库总数
     */
    suspend fun getWordCount(): Int {
        return wordDao.getWordCount()
    }

    /**
     * 检查单词是否存在
     */
    suspend fun wordExists(word: String): Boolean {
        return wordDao.wordExists(word)
    }
}

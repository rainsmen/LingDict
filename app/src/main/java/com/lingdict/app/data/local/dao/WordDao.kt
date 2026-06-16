package com.lingdict.app.data.local.dao

import androidx.room.*
import com.lingdict.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 词典库数据访问对象
 */
@Dao
interface WordDao {

    /**
     * 搜索单词（用于自动补全）
     * @param query 查询关键词
     * @param limit 返回数量限制
     */
    @Query("SELECT * FROM words WHERE word LIKE :query || '%' ORDER BY frequency DESC LIMIT :limit")
    fun searchWords(query: String, limit: Int = 10): Flow<List<WordEntity>>

    /**
     * 精确查询单词
     */
    @Query("SELECT * FROM words WHERE word = :word")
    suspend fun getWord(word: String): WordEntity?

    /**
     * 模糊搜索（用于全文搜索）
     */
    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR translation LIKE '%' || :query || '%' LIMIT :limit")
    fun fuzzySearch(query: String, limit: Int = 20): Flow<List<WordEntity>>

    /**
     * 按难度等级查询
     */
    @Query("SELECT * FROM words WHERE level = :level ORDER BY frequency DESC")
    fun getWordsByLevel(level: String): Flow<List<WordEntity>>

    /**
     * 获取随机单词（用于测试干扰项）
     */
    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :count")
    suspend fun getRandomWords(count: Int): List<WordEntity>

    /**
     * 插入单词
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    /**
     * 批量插入（用于导入ECDICT）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    /**
     * 批量插入（别名，用于DictionaryImporter）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    /**
     * 更新单词
     */
    @Update
    suspend fun updateWord(word: WordEntity)

    /**
     * 删除单词
     */
    @Delete
    suspend fun deleteWord(word: WordEntity)

    /**
     * 获取词库总数
     */
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    /**
     * 检查单词是否存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE word = :word)")
    suspend fun wordExists(word: String): Boolean
}

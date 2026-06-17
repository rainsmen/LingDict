package com.lingdict.app.data.local.dao

import androidx.room.*
import com.lingdict.app.data.local.entity.ExampleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 例句数据访问对象
 */
@Dao
interface ExampleDao {

    /**
     * 获取单词的所有例句
     */
    @Query("SELECT * FROM examples WHERE word = :word")
    fun getExamples(word: String): Flow<List<ExampleEntity>>

    /**
     * 插入例句
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExample(example: ExampleEntity)

    /**
     * 批量插入例句
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamples(examples: List<ExampleEntity>)

    /**
     * 删除单词的所有例句
     */
    @Query("DELETE FROM examples WHERE word = :word")
    suspend fun deleteExamples(word: String)
}

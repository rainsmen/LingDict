package com.lingdict.app.data.local.importer

import android.content.Context
import com.lingdict.app.data.local.dao.WordDao
import com.lingdict.app.data.local.entity.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class ImportProgress(
    val current: Int,
    val total: Int,
    val percentage: Int = (current * 100f / total).toInt()
)

@Singleton
class DictionaryImporter @Inject constructor(
    private val wordDao: WordDao
) {
    /**
     * 从assets导入ECDICT词库
     *
     * @param context Android上下文
     * @param assetPath assets中的CSV文件路径
     * @param limit 导入词条数量限制
     * @return Flow<ImportProgress> 导入进度
     */
    fun importFromAssets(
        context: Context,
        assetPath: String = "ecdict.csv",
        limit: Int = 50000
    ): Flow<ImportProgress> = flow {
        withContext(Dispatchers.IO) {
            try {
                // 打开CSV文件
                val inputStream = context.assets.open(assetPath)
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

                // 跳过标题行
                reader.readLine()

                val words = mutableListOf<WordEntity>()
                var lineCount = 0
                val batchSize = 1000

                // 读取并解析CSV
                reader.useLines { lines ->
                    lines.take(limit).forEach { line ->
                        val word = parseCsvLine(line, lineCount)
                        if (word != null) {
                            words.add(word)

                            // 批量插入
                            if (words.size >= batchSize) {
                                wordDao.insertAll(words)
                                words.clear()
                                emit(ImportProgress(lineCount, limit))
                            }
                        }
                        lineCount++
                    }
                }

                // 插入剩余数据
                if (words.isNotEmpty()) {
                    wordDao.insertAll(words)
                    emit(ImportProgress(lineCount, limit))
                }

            } catch (e: Exception) {
                throw ImportException("导入失败: ${e.message}", e)
            }
        }
    }

    /**
     * 检查是否已导入词库
     */
    suspend fun isImported(): Boolean {
        return withContext(Dispatchers.IO) {
            wordDao.getWordCount() > 0
        }
    }

    /**
     * 解析CSV行
     */
    private fun parseCsvLine(line: String, id: Int): WordEntity? {
        try {
            val fields = parseCsvFields(line)
            if (fields.size < 4) return null

            val word = fields[0].trim()
            val phonetic = fields.getOrNull(1)?.trim() ?: ""
            val definition = fields.getOrNull(2)?.trim() ?: ""
            val translation = fields.getOrNull(3)?.trim() ?: ""
            val tag = fields.getOrNull(7)?.trim() ?: ""

            // 过滤无效数据
            if (word.isEmpty() || translation.isEmpty()) return null
            if (word.length > 50) return null
            if (!word.matches(Regex("^[a-zA-Z][a-zA-Z\\-']*$"))) return null

            // 计算词频
            val bnc = fields.getOrNull(8)?.toIntOrNull() ?: 0
            val frq = fields.getOrNull(9)?.toIntOrNull() ?: 0
            val frequency = bnc + frq

            // 确定等级
            val level = determineLevel(tag, fields.getOrNull(6))

            return WordEntity(
                id = id.toLong(),
                word = word,
                phonetic = phonetic,
                definition = definition,
                translation = translation,
                level = level,
                frequency = frequency
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 解析CSV字段（处理引号和逗号）
     */
    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> currentField.append(char)
            }
        }
        fields.add(currentField.toString())

        return fields
    }

    /**
     * 确定单词等级
     */
    private fun determineLevel(tag: String?, oxford: String?): String? {
        if (tag.isNullOrEmpty()) {
            return if (!oxford.isNullOrEmpty()) "OXFORD" else null
        }

        val tagLower = tag.lowercase()
        return when {
            "cet4" in tagLower -> "CET4"
            "cet6" in tagLower -> "CET6"
            "考研" in tag || "kaoyan" in tagLower -> "KAOYAN"
            "toefl" in tagLower || "托福" in tag -> "TOEFL"
            "ielts" in tagLower || "雅思" in tag -> "IELTS"
            "gre" in tagLower -> "GRE"
            else -> "COMMON"
        }
    }
}

/**
 * 导入异常
 */
class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

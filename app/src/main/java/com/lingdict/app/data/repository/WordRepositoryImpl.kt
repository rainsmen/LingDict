package com.lingdict.app.data.repository

import com.lingdict.app.BuildConfig
import com.lingdict.app.data.local.dao.ExampleDao
import com.lingdict.app.data.local.dao.WordDao
import com.lingdict.app.data.local.entity.WordEntity
import com.lingdict.app.data.mapper.toWordEntity
import com.lingdict.app.data.remote.FreeDictionaryApiService
import com.lingdict.app.data.remote.YoudaoApiService
import com.lingdict.app.domain.model.Example
import com.lingdict.app.domain.model.Word
import com.lingdict.app.domain.repository.WordRepository
import com.lingdict.app.util.YoudaoSignUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 词典仓库实现
 * 混合数据源策略：优先查询本地ECDICT，未找到则调用有道API
 */
@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val exampleDao: ExampleDao,
    private val youdaoApi: YoudaoApiService,
    private val freeDictionaryApi: FreeDictionaryApiService
) : WordRepository {

    /**
     * 实现接口：搜索单词（用于自动补全）
     */
    override fun searchWords(query: String): Flow<List<Word>> {
        return wordDao.searchWords(query, 10).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * 实现接口：获取单词详情
     */
    override suspend fun getWord(word: String): Word? {
        return getWordInternal(word).getOrNull()?.let { wordEntity ->
            val examples = exampleDao.getExamples(word).first().map { exampleEntity ->
                Example(
                    sentenceEn = exampleEntity.sentenceEn,
                    sentenceZh = exampleEntity.sentenceZh,
                    source = exampleEntity.source
                )
            }
            wordEntity.toDomainModel().copy(
                examples = examples.ifEmpty { fallbackExamples(wordEntity) }
            )
        }
    }

    /**
     * 实现接口：获取随机单词
     */
    override suspend fun getRandomWords(count: Int): List<Word> {
        return getRandomWordsInternal(count).map { it.toDomainModel() }
    }

    /**
     * 内部方法：获取单词详情（混合数据源）
     * 1. 先查本地数据库
     * 2. 未找到则调用有道API
     * 3. 将API结果缓存到本地
     */
    private suspend fun getWordInternal(word: String): Result<WordEntity> {
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
                getWordFromFreeDictionary(word)?.let { fallbackWord ->
                    wordDao.insertWord(fallbackWord)
                    Result.success(fallbackWord)
                } ?: Result.success(createMinimalWord(word))
            }

        } catch (e: Exception) {
            getWordFromFreeDictionary(word)?.let { fallbackWord ->
                wordDao.insertWord(fallbackWord)
                Result.success(fallbackWord)
            } ?: Result.success(createMinimalWord(word))
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
     * 内部方法：获取随机单词（用于生成测试题干扰项）
     */
    private suspend fun getRandomWordsInternal(count: Int, excludeWord: String? = null): List<WordEntity> {
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

    private suspend fun getWordFromFreeDictionary(word: String): WordEntity? {
        return try {
            val entry = freeDictionaryApi.lookup(word.trim().lowercase()).firstOrNull() ?: return null
            val firstMeaning = entry.meanings.orEmpty().firstOrNull()
            val firstDefinition = firstMeaning?.definitions.orEmpty()
                .firstOrNull { !it.definition.isNullOrBlank() }
                ?.definition
                .orEmpty()
            if (firstDefinition.isBlank()) return null

            val phonetic = entry.phonetic
                ?: entry.phonetics.orEmpty().firstOrNull { !it.text.isNullOrBlank() }?.text
            val audio = entry.phonetics.orEmpty().firstOrNull { !it.audio.isNullOrBlank() }?.audio

            WordEntity(
                word = entry.word?.ifBlank { word } ?: word,
                phonetic = phonetic?.takeIf { it.isNotBlank() },
                definition = firstDefinition,
                translation = firstDefinition,
                pos = firstMeaning?.partOfSpeech,
                audio = audio,
                addedTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun createMinimalWord(word: String): WordEntity {
        val normalized = word.trim()
        return WordEntity(
            word = normalized,
            definition = "No local or online definition was returned for this word.",
            translation = "暂无释义，可稍后重试在线查询。",
            addedTime = System.currentTimeMillis()
        )
    }

    private fun fallbackExamples(word: WordEntity): List<Example> {
        val meaning = word.translation.ifBlank { word.definition }.ifBlank { "这个词" }
        val templates = listOf(
            Example(
                sentenceEn = "The article uses ${word.word} to express an important idea.",
                sentenceZh = "这篇文章用 ${word.word} 表达一个重要意思：$meaning。",
                source = "LingDict"
            ),
            Example(
                sentenceEn = "You may see ${word.word} in reading, writing, or daily conversation.",
                sentenceZh = "你可能会在阅读、写作或日常对话中见到 ${word.word}。",
                source = "LingDict"
            ),
            Example(
                sentenceEn = "Try to connect ${word.word} with a real situation you know.",
                sentenceZh = "试着把 ${word.word} 和你熟悉的真实场景联系起来。",
                source = "LingDict"
            ),
            Example(
                sentenceEn = "A short sentence can make ${word.word} easier to remember.",
                sentenceZh = "一个短句可以让 ${word.word} 更容易被记住。",
                source = "LingDict"
            )
        )
        val start = word.word.lowercase().fold(0) { acc, char -> acc + char.code } % templates.size
        return listOf(templates[start], templates[(start + 1) % templates.size])
    }

    /**
     * Entity转Domain模型
     */
    private fun WordEntity.toDomainModel(): Word {
        return Word(
            word = word,
            phonetic = phonetic?.takeIf { it.isNotBlank() }
                ?: phoneticUs?.takeIf { it.isNotBlank() }
                ?: phoneticUk?.takeIf { it.isNotBlank() },
            definition = definition,
            translation = translation,
            level = level,
            pos = pos,
            collins = collins,
            oxford = oxford,
            tag = tag,
            bnc = bnc,
            frq = frq,
            exchange = exchange,
            detail = detail,
            audio = audio
        )
    }
}

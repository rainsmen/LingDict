package com.lingdict.app.data.repository

import com.lingdict.app.BuildConfig
import com.google.gson.JsonElement
import com.lingdict.app.data.datastore.UserSettings
import com.lingdict.app.data.local.dao.ExampleDao
import com.lingdict.app.data.local.dao.WordDao
import com.lingdict.app.data.local.entity.ExampleEntity
import com.lingdict.app.data.datastore.SettingsDataStore
import com.lingdict.app.data.local.entity.WordEntity
import com.lingdict.app.data.mapper.toWordEntity
import com.lingdict.app.data.remote.DatamuseApiService
import com.lingdict.app.data.remote.FreeDictionaryApiService
import com.lingdict.app.data.remote.MerriamApiService
import com.lingdict.app.data.remote.dto.FreeDictionaryDefinition
import com.lingdict.app.data.remote.dto.MerriamEntry
import com.lingdict.app.data.remote.dto.WordsApiResult
import com.lingdict.app.data.remote.YoudaoApiService
import com.lingdict.app.data.remote.WordsApiService
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
    private val freeDictionaryApi: FreeDictionaryApiService,
    private val datamuseApi: DatamuseApiService,
    private val merriamApi: MerriamApiService,
    private val wordsApi: WordsApiService,
    private val settingsDataStore: SettingsDataStore
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
        val wordEntity = getWordInternal(word).getOrNull() ?: return null
        val enrichedEntity = enrichWordEntity(wordEntity)

        val examples = exampleDao.getExamples(enrichedEntity.word).first().map { exampleEntity ->
            Example(
                sentenceEn = exampleEntity.sentenceEn,
                sentenceZh = exampleEntity.sentenceZh,
                source = exampleEntity.source
            )
        }

        return enrichedEntity.toDomainModel().copy(
            examples = examples.ifEmpty { fallbackExamples(enrichedEntity) }
        )
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
        val normalized = word.trim().lowercase()
        return try {
            val settings = settingsDataStore.userSettingsFlow.first()

            if (settings.onlineLookupPreferred) {
                getWordFromOnlineFallbacks(normalized, settings)?.let { fallback ->
                    cacheOnlineResult(fallback)
                    return Result.success(fallback.word)
                }
            }

            val localWord = wordDao.getWord(normalized)
            if (localWord != null) {
                return Result.success(localWord)
            }

            if (!settings.onlineLookupPreferred) {
                getWordFromOnlineFallbacks(normalized, settings)?.let { fallback ->
                    cacheOnlineResult(fallback)
                    return Result.success(fallback.word)
                }
            }

            commonFallback(normalized)?.let { commonWord ->
                val entity = commonWord.toWordEntity(normalized)
                wordDao.insertWord(entity)
                exampleDao.insertExamples(commonWord.toExampleEntities(normalized))
                return Result.success(entity)
            }

            Result.failure(Exception("单词未找到或在线词典暂不可用"))
        } catch (e: Exception) {
            commonFallback(normalized)?.let { commonWord ->
                val entity = commonWord.toWordEntity(normalized)
                wordDao.insertWord(entity)
                exampleDao.insertExamples(commonWord.toExampleEntities(normalized))
                return Result.success(entity)
            }
            Result.failure(e)
        }
    }

    private suspend fun cacheOnlineResult(result: OnlineLookupResult) {
        wordDao.insertWord(result.word)
        if (result.examples.isNotEmpty()) {
            exampleDao.insertExamples(result.examples)
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

    private suspend fun enrichWordEntity(word: WordEntity): WordEntity {
        val normalized = word.word.lowercase()
        val common = commonFallback(normalized)
        val shouldFetchOnline = word.phonetic.isNullOrBlank() || word.audio.isNullOrBlank()

        val online = if (shouldFetchOnline) getWordFromOnlineFallbacks(normalized) else null
        val enriched = word.copy(
            phonetic = word.phonetic?.takeIf { it.isNotBlank() }
                ?: common?.phonetic
                ?: online?.word?.phonetic,
            audio = word.audio?.takeIf { it.isNotBlank() } ?: online?.word?.audio,
            definition = word.definition.ifBlank { common?.definition ?: online?.word?.definition.orEmpty() },
            translation = word.translation.ifBlank { common?.translation ?: online?.word?.translation.orEmpty() },
            pos = word.pos ?: common?.pos ?: online?.word?.pos
        )

        if (enriched != word) {
            wordDao.insertWord(enriched)
        }
        if (exampleDao.getExamples(enriched.word).first().isEmpty()) {
            val examples = common?.toExampleEntities(enriched.word)?.takeIf { it.isNotEmpty() }
                ?: online?.examples.orEmpty()
            if (examples.isNotEmpty()) {
                exampleDao.insertExamples(examples)
            }
        }
        return enriched
    }

    private data class OnlineLookupResult(
        val word: WordEntity,
        val examples: List<ExampleEntity> = emptyList()
    )

    private suspend fun getWordFromYoudao(word: String, settings: UserSettings): OnlineLookupResult? {
        if (!settings.youdaoEnabled) return null
        val appKey = settings.youdaoAppKey.ifBlank { BuildConfig.YOUDAO_APP_KEY }
        val appSecret = settings.youdaoAppSecret.ifBlank { BuildConfig.YOUDAO_APP_SECRET }
        if (appKey.isBlank() || appSecret.isBlank()) return null

        return try {
            val salt = YoudaoSignUtil.generateSalt()
            val curtime = YoudaoSignUtil.getCurrentTime()
            val sign = YoudaoSignUtil.generateSign(
                appKey = appKey,
                appSecret = appSecret,
                query = word,
                salt = salt,
                curtime = curtime
            )
            youdaoApi.translate(
                query = word,
                appKey = appKey,
                salt = salt,
                sign = sign,
                curtime = curtime
            ).toWordEntity()?.let { OnlineLookupResult(it) }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getWordFromOnlineFallbacks(
        word: String,
        settings: UserSettings = settingsDataStore.userSettingsFlow.first()
    ): OnlineLookupResult? {
        getWordFromYoudao(word, settings)?.let { return it }
        if (settings.freeDictionaryEnabled) {
            getWordFromFreeDictionary(word)?.let { return it }
        }
        if (settings.merriamEnabled && settings.merriamApiKey.isNotBlank()) {
            getWordFromMerriam(word, settings.merriamApiKey)?.let { return it }
        }
        if (settings.wordsApiEnabled && settings.wordsApiKey.isNotBlank()) {
            getWordFromWordsApi(word, settings.wordsApiKey, settings.wordsApiHost)?.let { return it }
        }
        if (settings.datamuseEnabled) {
            getWordFromDatamuse(word)?.let { return it }
        }
        return null
    }

    private suspend fun getWordFromFreeDictionary(word: String): OnlineLookupResult? {
        return try {
            val entry = freeDictionaryApi.lookup(word.trim().lowercase()).firstOrNull() ?: return null
            val meanings = entry.meanings.orEmpty()
            val firstMeaning = meanings.firstOrNull()
            val definitions = meanings.flatMap { meaning ->
                meaning.definitions.orEmpty().filter { !it.definition.isNullOrBlank() }
            }
            val firstDefinition = definitions.firstOrNull()?.definition.orEmpty()
            if (firstDefinition.isBlank()) return null

            val phonetic = entry.phonetic
                ?: entry.phonetics.orEmpty().firstOrNull { !it.text.isNullOrBlank() }?.text
            val audio = entry.phonetics.orEmpty().firstOrNull { !it.audio.isNullOrBlank() }?.audio
            val resolvedWord = entry.word?.ifBlank { word } ?: word

            val examples = definitions.toExamples(resolvedWord)

            OnlineLookupResult(
                word = WordEntity(
                    word = resolvedWord,
                    phonetic = phonetic?.takeIf { it.isNotBlank() },
                    definition = definitions.take(2).mapNotNull { it.definition }.joinToString("; ").ifBlank { firstDefinition },
                    translation = firstDefinition,
                    pos = firstMeaning?.partOfSpeech,
                    audio = audio,
                    addedTime = System.currentTimeMillis()
                ),
                examples = examples
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getWordFromMerriam(word: String, apiKey: String): OnlineLookupResult? {
        return try {
            val entry = merriamApi.lookup(word.trim().lowercase(), apiKey).firstOrNull() ?: return null
            entry.toOnlineLookupResult(word)
        } catch (e: Exception) {
            null
        }
    }

    private fun MerriamEntry.toOnlineLookupResult(requestedWord: String): OnlineLookupResult? {
        val definitions = shortDefinitions.orEmpty().filter { it.isNotBlank() }
        val firstDefinition = definitions.firstOrNull() ?: return null
        val resolvedWord = headword?.text?.replace("*", "")?.ifBlank { requestedWord } ?: requestedWord
        val phonetic = headword?.pronunciations.orEmpty()
            .firstOrNull { !it.written.isNullOrBlank() }
            ?.written
            ?.let { "/$it/" }

        return OnlineLookupResult(
            word = WordEntity(
                word = resolvedWord,
                phonetic = phonetic,
                definition = definitions.take(3).joinToString("; "),
                translation = firstDefinition,
                pos = functionalLabel,
                addedTime = System.currentTimeMillis()
            )
        )
    }

    private suspend fun getWordFromWordsApi(word: String, apiKey: String, host: String): OnlineLookupResult? {
        return try {
            val response = wordsApi.lookup(
                word = word.trim().lowercase(),
                apiKey = apiKey,
                host = host.ifBlank { WordsApiService.DEFAULT_HOST }
            )
            val definitions = response.results.orEmpty().filter { !it.definition.isNullOrBlank() }
            val first = definitions.firstOrNull() ?: return null
            val resolvedWord = response.word?.ifBlank { word } ?: word
            val examples = definitions.toWordsApiExamples(resolvedWord)

            OnlineLookupResult(
                word = WordEntity(
                    word = resolvedWord,
                    phonetic = response.pronunciation.toPronunciation(),
                    definition = definitions.take(3).mapNotNull { it.definition }.joinToString("; "),
                    translation = first.definition.orEmpty(),
                    pos = first.partOfSpeech,
                    addedTime = System.currentTimeMillis()
                ),
                examples = examples
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getWordFromDatamuse(word: String): OnlineLookupResult? {
        return try {
            val result = datamuseApi.words(spelling = word.trim().lowercase(), metadata = "dps", max = 1).firstOrNull()
                ?: return null
            val definition = result.definitions.orEmpty().firstOrNull().toCleanDatamuseDefinition() ?: return null
            val pos = result.tags.orEmpty().firstOrNull { it.length <= 5 && it.all(Char::isLetter) }

            OnlineLookupResult(
                word = WordEntity(
                    word = result.word?.ifBlank { word } ?: word,
                    definition = definition,
                    translation = definition,
                    pos = pos,
                    addedTime = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonElement?.toPronunciation(): String? {
        val value = this ?: return null
        if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            return value.asString.takeIf { it.isNotBlank() }
        }
        if (value.isJsonObject) {
            val obj = value.asJsonObject
            return listOf("all", "ipa", "us", "uk")
                .firstNotNullOfOrNull { key ->
                    obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
                }
        }
        return null
    }

    private fun List<WordsApiResult>.toWordsApiExamples(word: String): List<ExampleEntity> {
        return flatMap { result ->
            result.examples.orEmpty().map { example ->
                ExampleEntity(
                    word = word,
                    sentenceEn = example,
                    sentenceZh = result.definition.orEmpty(),
                    source = "WordsAPI"
                )
            }
        }.distinctBy { it.sentenceEn }.take(3)
    }

    private fun String?.toCleanDatamuseDefinition(): String? {
        val value = this?.substringAfter('\t', this)?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() }
    }

    private fun List<FreeDictionaryDefinition>.toExamples(word: String): List<ExampleEntity> {
        return mapNotNull { definition ->
            val example = definition.example?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExampleEntity(
                word = word,
                sentenceEn = example,
                sentenceZh = definition.definition.orEmpty(),
                source = "Free Dictionary"
            )
        }.distinctBy { it.sentenceEn }.take(3)
    }

    private fun fallbackExamples(word: WordEntity): List<Example> {
        return commonFallback(word.word)?.examples?.takeIf { it.isNotEmpty() } ?: emptyList()
    }

    private data class CommonFallbackWord(
        val word: String,
        val phonetic: String,
        val definition: String,
        val translation: String,
        val pos: String,
        val examples: List<Example>
    ) {
        fun toWordEntity(requestedWord: String): WordEntity {
            return WordEntity(
                word = requestedWord.trim().ifBlank { word },
                phonetic = phonetic,
                definition = definition,
                translation = translation,
                pos = pos,
                addedTime = System.currentTimeMillis()
            )
        }

        fun toExampleEntities(requestedWord: String): List<ExampleEntity> {
            val resolvedWord = requestedWord.trim().ifBlank { word }
            return examples.map { example ->
                ExampleEntity(
                    word = resolvedWord,
                    sentenceEn = example.sentenceEn,
                    sentenceZh = example.sentenceZh,
                    source = example.source
                )
            }
        }
    }

    private fun commonFallback(word: String): CommonFallbackWord? {
        return commonFallbackWords[word.trim().lowercase()]
    }

    private val commonFallbackWords = mapOf(
        "apple" to CommonFallbackWord(
            word = "apple",
            phonetic = "/ˈæpəl/",
            definition = "A round fruit with red, yellow, or green skin and firm white flesh.",
            translation = "n. 苹果；苹果树",
            pos = "noun",
            examples = listOf(
                Example("She packed an apple in her lunch bag.", "她在午餐袋里放了一个苹果。", "LingDict"),
                Example("The apple tree blooms every spring.", "这棵苹果树每年春天都会开花。", "LingDict")
            )
        ),
        "application" to CommonFallbackWord(
            word = "application",
            phonetic = "/ˌæplɪˈkeɪʃən/",
            definition = "A formal request, a practical use, or a computer program designed for a task.",
            translation = "n. 申请；应用；应用程序",
            pos = "noun",
            examples = listOf(
                Example("Her job application was accepted yesterday.", "她的求职申请昨天被接受了。", "LingDict"),
                Example("This application helps students review vocabulary.", "这个应用程序帮助学生复习词汇。", "LingDict")
            )
        ),
        "apply" to CommonFallbackWord(
            word = "apply",
            phonetic = "/əˈplaɪ/",
            definition = "To make a formal request or to put something to practical use.",
            translation = "v. 申请；应用；涂抹",
            pos = "verb",
            examples = listOf(
                Example("You should apply for the scholarship before Friday.", "你应该在周五前申请奖学金。", "LingDict"),
                Example("Apply the cream gently to the skin.", "把乳霜轻轻涂在皮肤上。", "LingDict")
            )
        ),
        "banana" to CommonFallbackWord(
            word = "banana",
            phonetic = "/bəˈnænə/",
            definition = "A long curved fruit with a yellow skin and soft sweet flesh.",
            translation = "n. 香蕉",
            pos = "noun",
            examples = listOf(
                Example("He ate a banana after running.", "他跑步后吃了一根香蕉。", "LingDict"),
                Example("Bananas are rich in potassium.", "香蕉富含钾。", "LingDict")
            )
        ),
        "computer" to CommonFallbackWord(
            word = "computer",
            phonetic = "/kəmˈpjuːtər/",
            definition = "An electronic machine that stores and processes data.",
            translation = "n. 计算机；电脑",
            pos = "noun",
            examples = listOf(
                Example("The computer stores all the project files.", "这台电脑保存了所有项目文件。", "LingDict"),
                Example("She bought a new computer for school.", "她为上学买了一台新电脑。", "LingDict")
            )
        ),
        "dictionary" to CommonFallbackWord(
            word = "dictionary",
            phonetic = "/ˈdɪkʃəneri/",
            definition = "A book or digital resource that explains words and their meanings.",
            translation = "n. 字典；词典",
            pos = "noun",
            examples = listOf(
                Example("I looked up the word in a dictionary.", "我在词典里查了这个单词。", "LingDict"),
                Example("A good dictionary gives examples as well as meanings.", "一本好词典会给出例句和释义。", "LingDict")
            )
        ),
        "hello" to CommonFallbackWord(
            word = "hello",
            phonetic = "/həˈloʊ/",
            definition = "A greeting used when meeting someone or starting a conversation.",
            translation = "int. 你好；喂",
            pos = "interjection",
            examples = listOf(
                Example("She smiled and said hello.", "她微笑着说了声你好。", "LingDict"),
                Example("Hello, may I speak to Mr. Chen?", "喂，我可以和陈先生通话吗？", "LingDict")
            )
        ),
        "world" to CommonFallbackWord(
            word = "world",
            phonetic = "/wɜːrld/",
            definition = "The earth and all the people, places, and things on it.",
            translation = "n. 世界；地球；领域",
            pos = "noun",
            examples = listOf(
                Example("People around the world use the internet every day.", "世界各地的人每天都使用互联网。", "LingDict"),
                Example("The discovery changed the world of medicine.", "这项发现改变了医学界。", "LingDict")
            )
        )
    )

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

package com.lingdict.app.domain.usecase

import com.lingdict.app.data.repository.UserWordRepositoryImpl
import com.lingdict.app.data.repository.WordRepositoryImpl
import com.lingdict.app.domain.model.Question
import com.lingdict.app.domain.model.UserWord
import javax.inject.Inject
import kotlin.random.Random

/**
 * 生成测试题用例
 */
class GenerateTestUseCase @Inject constructor(
    private val wordRepository: WordRepositoryImpl,
    private val userWordRepository: UserWordRepositoryImpl
) {

    /**
     * 生成选择题
     * @param userWord 用户单词
     * @return 选择题
     */
    suspend fun generateMultipleChoice(userWord: UserWord): Question.MultipleChoice {
        val correctAnswer = userWord.word.translation

        // 获取3个干扰项（随机单词的翻译）
        val distractors = wordRepository.getRandomWords(3, excludeWord = userWord.word.word)
            .map { it.translation }
            .filter { it != correctAnswer }
            .take(3)

        // 组合并打乱选项
        val options = (listOf(correctAnswer) + distractors).shuffled()

        return Question.MultipleChoice(
            id = "mc_${userWord.id}_${System.currentTimeMillis()}",
            word = userWord.word.word,
            options = options,
            correctAnswer = correctAnswer
        )
    }

    /**
     * 生成填空题
     * @param userWord 用户单词
     * @return 填空题
     */
    fun generateFillInBlank(userWord: UserWord): Question.FillInBlank {
        val word = userWord.word.word

        // 隐藏中间部分字母（至少保留首尾各2个字母）
        val hiddenStart = minOf(2, word.length / 3)
        val hiddenEnd = maxOf(word.length - 2, word.length - word.length / 3)

        val visibleStart = word.substring(0, hiddenStart)
        val hiddenPart = word.substring(hiddenStart, hiddenEnd)
        val visibleEnd = word.substring(hiddenEnd)

        val displayWord = "$visibleStart${"_".repeat(hiddenPart.length)}$visibleEnd"

        return Question.FillInBlank(
            id = "fb_${userWord.id}_${System.currentTimeMillis()}",
            word = word,
            displayWord = displayWord,
            hiddenPart = hiddenPart,
            correctAnswer = hiddenPart.lowercase(),
            hint = userWord.word.translation
        )
    }

    /**
     * 生成听力题
     * @param userWord 用户单词
     * @return 听力题
     */
    suspend fun generateListening(userWord: UserWord): Question.Listening {
        val correctAnswer = userWord.word.word

        // 获取3个干扰项（发音相似或随机单词）
        val distractors = wordRepository.getRandomWords(3, excludeWord = correctAnswer)
            .map { it.word }
            .take(3)

        // 组合并打乱选项
        val options = (listOf(correctAnswer) + distractors).shuffled()

        return Question.Listening(
            id = "ls_${userWord.id}_${System.currentTimeMillis()}",
            word = correctAnswer,
            options = options,
            correctAnswer = correctAnswer
        )
    }

    /**
     * 生成判断题
     * @param userWord 用户单词
     * @return 判断题
     */
    suspend fun generateTrueFalse(userWord: UserWord): Question.TrueFalse {
        val word = userWord.word.word
        val translation = userWord.word.translation

        // 随机决定是否使用正确用法
        val isCorrectUsage = Random.nextBoolean()

        val (sentence, sentenceTranslation) = if (isCorrectUsage) {
            // 使用正确的单词
            generateCorrectSentence(word, translation)
        } else {
            // 使用错误的单词（用相似词替换）
            generateIncorrectSentence(word, translation)
        }

        return Question.TrueFalse(
            id = "tf_${userWord.id}_${System.currentTimeMillis()}",
            word = word,
            sentence = sentence,
            translation = sentenceTranslation,
            isCorrectUsage = isCorrectUsage,
            correctAnswer = if (isCorrectUsage) "true" else "false"
        )
    }

    /**
     * 生成包含正确单词的句子
     */
    private fun generateCorrectSentence(word: String, translation: String): Pair<String, String> {
        // 简化实现：使用模板句子
        val templates = listOf(
            "I need to $word this." to "我需要${translation}这个。",
            "Can you $word that?" to "你能${translation}那个吗？",
            "The $word is important." to "这个$translation很重要。"
        )
        return templates.random()
    }

    /**
     * 生成包含错误单词的句子
     */
    private suspend fun generateIncorrectSentence(word: String, translation: String): Pair<String, String> {
        // 获取一个随机单词作为错误替换
        val wrongWord = wordRepository.getRandomWords(1, excludeWord = word)
            .firstOrNull()?.word ?: "wrong"

        val templates = listOf(
            "I need to $wrongWord this." to "我需要${translation}这个。",
            "Can you $wrongWord that?" to "你能${translation}那个吗？",
            "The $wrongWord is important." to "这个$translation很重要。"
        )
        return templates.random()
    }

    /**
     * 批量生成测试题
     * @param count 题目数量
     * @param types 题型列表（空表示随机所有类型）
     */
    suspend fun generateTest(count: Int, types: List<String> = emptyList()): List<Question> {
        // 获取用户的生词
        val dueWords = userWordRepository.getDueWords(count).let { flow ->
            val list = mutableListOf<com.lingdict.app.data.local.entity.UserWordEntity>()
            flow.collect { list.addAll(it) }
            list
        }

        if (dueWords.isEmpty()) {
            return emptyList()
        }

        val availableTypes = types.ifEmpty { listOf("choice", "fill", "listening", "judge") }

        return dueWords.take(count).mapIndexed { index, entity ->
            // 转换为UserWord
            val wordResult = wordRepository.getWord(entity.word)
            val wordEntity = wordResult.getOrNull() ?: return@mapIndexed null

            val userWord = UserWord(
                id = entity.id,
                word = com.lingdict.app.domain.model.Word(
                    word = wordEntity.word,
                    phonetic = wordEntity.phonetic,
                    phoneticUs = wordEntity.phoneticUs,
                    phoneticUk = wordEntity.phoneticUk,
                    definition = wordEntity.definition,
                    translation = wordEntity.translation,
                    level = wordEntity.level
                ),
                addedDate = entity.addedDate,
                lastReviewDate = entity.lastReviewDate,
                nextReviewDate = entity.nextReviewDate,
                easeFactor = entity.easeFactor,
                interval = entity.interval,
                repetitions = entity.repetitions,
                status = entity.status,
                knownCount = entity.knownCount,
                unknownCount = entity.unknownCount,
                testCorrectCount = entity.testCorrectCount,
                testTotalCount = entity.testTotalCount,
                isFavorite = entity.isFavorite,
                notes = entity.notes
            )

            // 根据题型生成
            when (availableTypes[index % availableTypes.size]) {
                "choice" -> generateMultipleChoice(userWord)
                "fill" -> generateFillInBlank(userWord)
                "listening" -> generateListening(userWord)
                "judge" -> generateTrueFalse(userWord)
                else -> generateMultipleChoice(userWord)
            }
        }.filterNotNull()
    }
}

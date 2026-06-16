package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.constants.QuestionTypes
import com.lingdict.app.domain.model.Question
import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.repository.UserWordRepository
import com.lingdict.app.domain.repository.WordRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

/**
 * 生成测试题用例
 */
class GenerateTestUseCase @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val wordRepository: WordRepository
) {

    /**
     * 生成选择题
     * @param userWord 用户单词
     * @return 选择题
     */
    suspend fun generateMultipleChoice(userWord: UserWord): Question.MultipleChoice {
        val correctAnswer = userWord.word.translation ?: "Unknown"

        // 获取4个随机单词，排除正确答案，取3个作为干扰项
        val distractors = wordRepository.getRandomWords(10)
            .mapNotNull { it.translation }
            .filter { it != correctAnswer }
            .distinct()
            .take(3)

        // 确保至少有3个干扰项，不足则用占位符
        val finalDistractors = if (distractors.size < 3) {
            distractors + List(3 - distractors.size) { "Option ${it + distractors.size + 1}" }
        } else {
            distractors
        }

        // 组合并打乱选项，确保有4个
        val options = (listOf(correctAnswer) + finalDistractors).take(4).shuffled()

        return Question.MultipleChoice(
            id = "mc_${userWord.id}_${System.currentTimeMillis()}",
            word = userWord.word.word,
            userWordId = userWord.id,
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

        // 计算隐藏长度，至少隐藏1个字母
        val hideLength = maxOf((word.length * 0.4).toInt(), 1)
        val hiddenStart = maxOf(1, (word.length - hideLength) / 2)
        val hiddenEnd = minOf(word.length - 1, hiddenStart + hideLength)

        val visibleStart = word.substring(0, hiddenStart)
        val hiddenPart = word.substring(hiddenStart, hiddenEnd)
        val visibleEnd = word.substring(hiddenEnd)

        val displayWord = "$visibleStart${"_".repeat(hiddenPart.length)}$visibleEnd"
        val translation = userWord.word.translation.orEmpty()

        return Question.FillInBlank(
            id = "fb_${userWord.id}_${System.currentTimeMillis()}",
            word = word,
            userWordId = userWord.id,
            displayWord = displayWord,
            hiddenPart = hiddenPart,
            correctAnswer = word,
            hint = translation
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
        val distractors = wordRepository.getRandomWords(3)
            .map { it.word }
            .take(3)

        // 组合并打乱选项
        val options = (listOf(correctAnswer) + distractors).shuffled()

        return Question.Listening(
            id = "ls_${userWord.id}_${System.currentTimeMillis()}",
            word = correctAnswer,
            userWordId = userWord.id,
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
        val translation = userWord.word.translation.orEmpty()

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
            userWordId = userWord.id,
            sentence = sentence,
            translation = sentenceTranslation,
            isCorrectUsage = isCorrectUsage,
            correctAnswer = isCorrectUsage
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
        val wrongWord = wordRepository.getRandomWords(1)
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
        // 获取用户的生词 - 使用first()而不是collect()
        val dueWords = userWordRepository.getDueWords(count).first()

        if (dueWords.isEmpty()) {
            return emptyList()
        }

        val availableTypes = types.ifEmpty {
            listOf(
                QuestionTypes.MULTIPLE_CHOICE,
                QuestionTypes.FILL_IN_BLANK,
                QuestionTypes.LISTENING,
                QuestionTypes.TRUE_FALSE
            )
        }

        return dueWords.take(count).mapIndexed { index, userWord ->
            // 根据题型生成
            when (availableTypes[index % availableTypes.size]) {
                QuestionTypes.MULTIPLE_CHOICE -> generateMultipleChoice(userWord)
                QuestionTypes.FILL_IN_BLANK -> generateFillInBlank(userWord)
                QuestionTypes.LISTENING -> generateListening(userWord)
                QuestionTypes.TRUE_FALSE -> generateTrueFalse(userWord)
                else -> generateMultipleChoice(userWord)
            }
        }
    }

    /**
     * 操作符重载，支持简化调用
     * @param type 题型
     * @param count 题目数量
     */
    suspend operator fun invoke(type: String, count: Int): List<Question> {
        return generateTest(count, listOf(type))
    }
}

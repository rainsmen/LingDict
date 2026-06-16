package com.lingdict.app.domain.model

/**
 * 测试题基类
 */
sealed class Question {
    abstract val id: String
    abstract val word: String
    abstract val correctAnswer: Any  // 改为 Any 以支持不同类型的答案

    /**
     * 选择题
     */
    data class MultipleChoice(
        override val id: String,
        override val word: String,
        val options: List<String>,             // 4个选项
        override val correctAnswer: String,    // 正确答案
        val questionText: String = "选择正确的中文释义：",
        val phonetic: String? = null           // 音标（用于 TestScreen 显示）
    ) : Question()

    /**
     * 填空题
     */
    data class FillInBlank(
        override val id: String,
        override val word: String,
        val displayWord: String,               // 显示的单词（部分字母隐藏）
        val hiddenPart: String,                // 隐藏的部分
        override val correctAnswer: String,    // 正确答案（隐藏的字母）
        val hint: String,                      // 提示（中文释义）
        val questionText: String = "根据释义填写缺失的字母：",
        val prompt: String = hint,             // 别名，用于 TestScreen 显示提示
        val fullWord: String = word            // 别名，用于 TestScreen 显示完整答案
    ) : Question()

    /**
     * 听力题
     */
    data class Listening(
        override val id: String,
        override val word: String,
        val options: List<String>,             // 4个选项
        override val correctAnswer: String,    // 正确答案
        val audioUrl: String? = null,          // 音频URL（可选）
        val questionText: String = "听音频选择正确的单词："
    ) : Question()

    /**
     * 判断题
     */
    data class TrueFalse(
        override val id: String,
        override val word: String,
        val sentence: String,                  // 包含单词的句子
        val translation: String,               // 句子翻译
        val isCorrectUsage: Boolean,           // 单词在句子中使用是否正确
        override val correctAnswer: Boolean,   // 改为 Boolean 类型
        val questionText: String = "判断句子中单词使用是否正确：",
        val statement: String = sentence       // 别名，用于 TestScreen 显示句子
    ) : Question()
}

/**
 * 测试结果
 */
data class TestResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val accuracy: Float,
    val timeSpent: Long,                       // 用时（毫秒）
    val wrongQuestions: List<Question> = emptyList()  // 错题列表
) {
    companion object {
        fun calculate(
            totalQuestions: Int,
            correctAnswers: Int,
            timeSpent: Long,
            wrongQuestions: List<Question> = emptyList()
        ): TestResult {
            val wrongAnswers = totalQuestions - correctAnswers
            val accuracy = if (totalQuestions > 0) {
                correctAnswers.toFloat() / totalQuestions
            } else {
                0f
            }
            return TestResult(
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers,
                accuracy = accuracy,
                timeSpent = timeSpent,
                wrongQuestions = wrongQuestions
            )
        }
    }
}

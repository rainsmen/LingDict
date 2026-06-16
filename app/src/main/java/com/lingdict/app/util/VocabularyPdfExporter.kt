package com.lingdict.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.lingdict.app.data.local.entity.WordStatus
import com.lingdict.app.domain.model.UserWord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun export(userWords: List<UserWord>): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "lingdict-vocabulary-${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        try {
            val renderer = Renderer(document)
            renderer.draw(userWords)

            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        file
    }

    private inner class Renderer(
        private val document: PdfDocument
    ) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 40f
        private val contentWidth = pageWidth - margin * 2
        private val lineGap = 6f

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 42, 58)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 75, 110)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 35, 35)
            textSize = 11f
        }
        private val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 100, 100)
            textSize = 9f
        }
        private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 224, 228)
            strokeWidth = 1f
        }

        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = margin

        fun draw(userWords: List<UserWord>) {
            startPage()
            drawTitle(userWords.size)

            userWords.forEachIndexed { index, userWord ->
                drawWord(index + 1, userWord)
            }

            finishPage()
        }

        private fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            y = margin
        }

        private fun finishPage() {
            page?.let {
                drawFooter(it.canvas)
                document.finishPage(it)
            }
            page = null
            canvas = null
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight <= pageHeight - margin - 24f) return
            finishPage()
            startPage()
        }

        private fun drawTitle(count: Int) {
            val c = canvas ?: return
            c.drawText("LingDict 生词库", margin, y, titlePaint)
            y += 22f
            c.drawText("导出时间：${dateFormat.format(Date())}   共 $count 个单词", margin, y, metaPaint)
            y += 22f
            c.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 18f
        }

        private fun drawWord(index: Int, userWord: UserWord) {
            val word = userWord.word
            val phonetic = word.phonetic.orEmpty()
            val translation = word.translation.orEmpty().ifBlank { "无中文释义" }
            val definition = word.definition.orEmpty()
            val meta = listOfNotNull(
                statusLabel(userWord.status),
                word.level,
                if (userWord.isFavorite) "收藏" else null,
                "复习 ${userWord.repetitions} 次",
                "正确 ${userWord.testCorrectCount}/${userWord.testTotalCount}"
            ).joinToString("  ·  ")

            val translationLines = wrapText(translation, bodyPaint, contentWidth)
            val definitionLines = if (definition.isBlank()) emptyList() else wrapText(definition, bodyPaint, contentWidth)
            val required = 30f + (translationLines.size + definitionLines.size) * 15f + 18f
            ensureSpace(required)

            val c = canvas ?: return
            c.drawText("$index. ${word.word}", margin, y, headingPaint)
            if (phonetic.isNotBlank()) {
                c.drawText(phonetic, margin + 180f, y, metaPaint)
            }
            y += 16f
            c.drawText(meta, margin, y, metaPaint)
            y += 16f

            translationLines.forEach { line ->
                c.drawText(line, margin, y, bodyPaint)
                y += 15f
            }

            if (definitionLines.isNotEmpty()) {
                y += 2f
                definitionLines.forEach { line ->
                    c.drawText(line, margin, y, bodyPaint)
                    y += 15f
                }
            }

            y += 5f
            c.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 14f
        }

        private fun drawFooter(c: Canvas) {
            val text = "第 $pageNumber 页"
            val bounds = Rect()
            metaPaint.getTextBounds(text, 0, text.length, bounds)
            c.drawText(text, pageWidth - margin - bounds.width(), pageHeight - 24f, metaPaint)
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return emptyList()
            val lines = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                var line = ""
                paragraph.forEach { char ->
                    val candidate = line + char
                    if (paint.measureText(candidate) <= maxWidth || line.isEmpty()) {
                        line = candidate
                    } else {
                        lines.add(line)
                        line = char.toString()
                    }
                }
                if (line.isNotEmpty()) lines.add(line)
            }
            return lines
        }

        private fun statusLabel(status: WordStatus): String {
            return when (status) {
                WordStatus.NEW -> "新词"
                WordStatus.LEARNING -> "学习中"
                WordStatus.MASTERED -> "已掌握"
            }
        }
    }
}

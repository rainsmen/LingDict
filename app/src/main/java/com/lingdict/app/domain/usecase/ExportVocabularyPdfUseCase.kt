package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.model.UserWord
import com.lingdict.app.domain.repository.UserWordRepository
import com.lingdict.app.domain.repository.WordRepository
import com.lingdict.app.util.VocabularyPdfExporter
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class ExportVocabularyPdfUseCase @Inject constructor(
    private val userWordRepository: UserWordRepository,
    private val wordRepository: WordRepository,
    private val pdfExporter: VocabularyPdfExporter
) {
    suspend operator fun invoke(): Result<File> {
        return try {
            val words = userWordRepository.getAllUserWords().first()
            if (words.isEmpty()) {
                return Result.failure(IllegalStateException("生词库为空，无法导出"))
            }

            val enriched = words.map { userWord ->
                val details = wordRepository.getWord(userWord.word.word) ?: userWord.word
                userWord.copy(word = details)
            }

            Result.success(pdfExporter.export(enriched))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}

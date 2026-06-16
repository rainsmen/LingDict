package com.lingdict.app.domain.usecase

import com.lingdict.app.domain.repository.StudyRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TodayProgress(
    val wordsLearned: Int = 0,
    val wordsReviewed: Int = 0
)

class GetTodayProgressUseCase @Inject constructor(
    private val studyRecordRepository: StudyRecordRepository
) {
    operator fun invoke(): Flow<TodayProgress> = flow {
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val record = studyRecordRepository.getRecordByDate(todayStart)

        emit(TodayProgress(
            wordsLearned = record?.totalWordsLearned ?: 0,
            wordsReviewed = record?.totalWordsReviewed ?: 0
        ))
    }
}

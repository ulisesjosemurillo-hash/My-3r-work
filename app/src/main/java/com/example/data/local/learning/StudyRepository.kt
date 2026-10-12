package com.example.data.local.learning

import com.example.model.CornellNoteItem
import com.example.model.SpacedCard
import com.example.model.StudySessionStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class StudyRepository(private val dao: StudyDao) {

    fun getDueCardsFlow(): Flow<List<SpacedRepetitionEntity>> {
        return dao.getDueCardsFlow(System.currentTimeMillis())
    }

    fun getDueCountFlow(): Flow<Int> {
        return dao.getDueCountFlow(System.currentTimeMillis())
    }

    fun getAllCardsFlow(): Flow<List<SpacedRepetitionEntity>> {
        return dao.getAllCardsFlow()
    }

    fun getCardsForDocumentFlow(documentId: String): Flow<List<SpacedRepetitionEntity>> {
        return dao.getCardsForDocumentFlow(documentId)
    }

    suspend fun saveOrUpdateCard(
        documentId: String,
        concept: String,
        question: String,
        answer: String,
        sourcePage: Int,
        intervalDays: Int = 1,
        easeFactor: Float = 2.5f,
        reviewsCount: Int = 0,
        correctCount: Int = 0,
        failCount: Int = 0,
        nextReviewTimestamp: Long = System.currentTimeMillis() + (intervalDays * 86400000L),
        id: String = UUID.randomUUID().toString()
    ) = withContext(Dispatchers.IO) {
        val entity = SpacedRepetitionEntity(
            id = id,
            documentId = documentId,
            concept = concept,
            question = question,
            answer = answer,
            sourcePage = sourcePage,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            reviewsCount = reviewsCount,
            correctCount = correctCount,
            failCount = failCount,
            nextReviewTimestamp = nextReviewTimestamp,
            lastReviewTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdateCard(entity)
    }

    suspend fun recordReviewOutcome(
        cardId: String,
        isSuccess: Boolean,
        newIntervalDays: Int,
        newEaseFactor: Float
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueCards = dao.getDueCards(now + 8640000000L)
        val existing = dueCards.firstOrNull { it.id == cardId }
        if (existing != null) {
            val updated = existing.copy(
                intervalDays = newIntervalDays,
                easeFactor = newEaseFactor,
                reviewsCount = existing.reviewsCount + 1,
                correctCount = if (isSuccess) existing.correctCount + 1 else existing.correctCount,
                failCount = if (!isSuccess) existing.failCount + 1 else existing.failCount,
                lastReviewTimestamp = now,
                nextReviewTimestamp = now + (newIntervalDays * 86400000L)
            )
            dao.insertOrUpdateCard(updated)
        }
    }

    suspend fun recordStudySession(
        documentId: String,
        documentTitle: String,
        mode: String,
        durationMinutes: Int,
        questionsAnswered: Int,
        questionsCorrect: Int,
        selfReflection: String = ""
    ) = withContext(Dispatchers.IO) {
        val entity = StudySessionEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            documentTitle = documentTitle,
            mode = mode,
            durationMinutes = durationMinutes,
            questionsAnswered = questionsAnswered,
            questionsCorrect = questionsCorrect,
            selfReflection = selfReflection,
            timestamp = System.currentTimeMillis()
        )
        dao.insertSession(entity)
    }

    fun getCornellNotesFlow(documentId: String): Flow<List<CornellNoteItem>> {
        return dao.getCornellNotesForDocumentFlow(documentId).map { list ->
            list.map { entity ->
                val cuesList = mutableListOf<String>()
                try {
                    val arr = JSONArray(entity.cuesJson)
                    for (i in 0 until arr.length()) cuesList.add(arr.getString(i))
                } catch (_: Exception) {}
                CornellNoteItem(
                    id = entity.id,
                    documentId = entity.documentId,
                    pageNumber = entity.pageNumber,
                    title = entity.title,
                    cues = cuesList,
                    notes = entity.notes,
                    summary = entity.summary,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    suspend fun saveCornellNote(note: CornellNoteItem) = withContext(Dispatchers.IO) {
        val cuesArr = JSONArray()
        note.cues.forEach { cuesArr.put(it) }
        val entity = CornellNoteEntity(
            id = note.id,
            documentId = note.documentId,
            pageNumber = note.pageNumber,
            title = note.title,
            cuesJson = cuesArr.toString(),
            notes = note.notes,
            summary = note.summary,
            timestamp = note.timestamp
        )
        dao.insertOrUpdateCornellNote(entity)
    }

    suspend fun computeStats(): StudySessionStats = withContext(Dispatchers.IO) {
        val totalMinutes = dao.getTotalMinutesStudied()
        val totalAnswered = dao.getTotalQuestionsAnswered()
        val totalCorrect = dao.getTotalQuestionsCorrect()
        val now = System.currentTimeMillis()
        val dueCards = dao.getDueCards(now)
        val allCount = dao.getTotalCardCount()

        val accuracy = if (totalAnswered > 0) ((totalCorrect * 100) / totalAnswered) else 0

        StudySessionStats(
            totalStudyMinutes = totalMinutes,
            guidedSessionsCompleted = (totalAnswered / 3),
            questionsAnswered = totalAnswered,
            questionsCorrect = totalCorrect,
            dueReviewsCount = dueCards.size,
            masteredConceptsCount = maxOf(0, allCount - dueCards.size),
            reviewingConceptsCount = dueCards.size,
            accuracyPercent = accuracy
        )
    }
}

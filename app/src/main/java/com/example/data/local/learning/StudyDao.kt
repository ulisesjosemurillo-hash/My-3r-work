package com.example.data.local.learning

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- REPETICIÓN ESPACIADA ---
    @Query("SELECT * FROM spaced_repetition_cards WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun getDueCardsFlow(currentTime: Long): Flow<List<SpacedRepetitionEntity>>

    @Query("SELECT * FROM spaced_repetition_cards WHERE nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    suspend fun getDueCards(currentTime: Long): List<SpacedRepetitionEntity>

    @Query("SELECT * FROM spaced_repetition_cards WHERE documentId = :documentId ORDER BY nextReviewTimestamp ASC")
    fun getCardsForDocumentFlow(documentId: String): Flow<List<SpacedRepetitionEntity>>

    @Query("SELECT * FROM spaced_repetition_cards ORDER BY lastReviewTimestamp DESC")
    fun getAllCardsFlow(): Flow<List<SpacedRepetitionEntity>>

    @Query("SELECT COUNT(*) FROM spaced_repetition_cards WHERE nextReviewTimestamp <= :currentTime")
    fun getDueCountFlow(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM spaced_repetition_cards")
    suspend fun getTotalCardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCard(card: SpacedRepetitionEntity)

    @Query("DELETE FROM spaced_repetition_cards WHERE id = :id")
    suspend fun deleteCard(id: String)

    @Query("DELETE FROM spaced_repetition_cards WHERE documentId = :documentId")
    suspend fun deleteCardsForDocument(documentId: String)


    // --- SESIONES DE ESTUDIO Y AUTORREGULACIÓN ---
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_sessions")
    suspend fun getTotalMinutesStudied(): Int

    @Query("SELECT COALESCE(SUM(questionsAnswered), 0) FROM study_sessions")
    suspend fun getTotalQuestionsAnswered(): Int

    @Query("SELECT COALESCE(SUM(questionsCorrect), 0) FROM study_sessions")
    suspend fun getTotalQuestionsCorrect(): Int


    // --- NOTAS CORNELL ---
    @Query("SELECT * FROM cornell_notes WHERE documentId = :documentId ORDER BY pageNumber ASC, timestamp DESC")
    fun getCornellNotesForDocumentFlow(documentId: String): Flow<List<CornellNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCornellNote(note: CornellNoteEntity)

    @Query("DELETE FROM cornell_notes WHERE id = :id")
    suspend fun deleteCornellNote(id: String)
}

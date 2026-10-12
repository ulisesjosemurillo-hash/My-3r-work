package com.example.data.local.learning

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaced_repetition_cards")
data class SpacedRepetitionEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val concept: String,
    val question: String,
    val answer: String,
    val sourcePage: Int,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val reviewsCount: Int = 0,
    val correctCount: Int = 0,
    val failCount: Int = 0,
    val nextReviewTimestamp: Long = System.currentTimeMillis() + 86400000L,
    val lastReviewTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val documentTitle: String,
    val mode: String, // "GUIDED", "FREE", "REVIEW"
    val durationMinutes: Int,
    val questionsAnswered: Int,
    val questionsCorrect: Int,
    val selfReflection: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cornell_notes")
data class CornellNoteEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val pageNumber: Int,
    val title: String,
    val cuesJson: String, // JSON array of strings
    val notes: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

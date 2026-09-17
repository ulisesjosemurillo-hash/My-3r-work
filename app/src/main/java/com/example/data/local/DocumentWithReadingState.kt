package com.example.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relation model combining a Document with its ReadingState.
 */
data class DocumentWithReadingState(
    @Embedded
    val document: Document,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val readingState: ReadingState?
)

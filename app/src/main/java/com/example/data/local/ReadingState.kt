package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity storing the persistent reading session state of a document,
 * particularly the last read fragment index, last read page, and timestamp.
 */
@Entity(
    tableName = "reading_states",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"], unique = true)]
)
data class ReadingState(
    @PrimaryKey
    val documentId: String,
    val lastReadFragmentIndex: Int = 0,
    val lastReadPage: Int = 1,
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

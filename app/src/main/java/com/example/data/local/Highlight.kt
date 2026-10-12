package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity representing a highlighted excerpt or text selection in a document.
 * Includes precise offsets, color, and location attributes for persistent reading & study.
 */
@Entity(
    tableName = "highlights",
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["documentId", "pageNumber"]),
        Index(value = ["documentId", "fragmentId"])
    ]
)
data class Highlight(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val fragmentId: Int,
    val selectedText: String,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val color: String = "#FFF59D",
    val timestamp: Long = System.currentTimeMillis()
)

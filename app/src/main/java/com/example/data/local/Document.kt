package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a document and its metadata for persistent sessions.
 */
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String = "",
    val totalPages: Int = 1,
    val totalFragments: Int = 0,
    val pagesJson: String = "",
    val sourceType: String = "SAMPLE", // "SAMPLE", "IMPORTED", "PASTED", "PDF"
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

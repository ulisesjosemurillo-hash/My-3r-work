package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a recently loaded legal document,
 * its content, and the saved reading/consultation progress.
 */
@Entity(tableName = "recent_documents")
data class RecentDocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String = "",
    val totalPages: Int,
    val totalFragments: Int,
    val lastPage: Int = 1,
    val lastFragmentIndex: Int = 0,
    val lastAccessedTimestamp: Long = System.currentTimeMillis(),
    val pagesJson: String,
    val sourceType: String = "SAMPLE" // "SAMPLE", "IMPORTED", "PASTED"
)

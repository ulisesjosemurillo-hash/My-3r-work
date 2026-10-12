package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local persistence of text highlights in documents.
 */
@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE documentId = :documentId ORDER BY pageNumber ASC, startOffset ASC, timestamp ASC")
    fun getHighlightsByDocument(documentId: String): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE documentId = :documentId AND pageNumber = :pageNumber ORDER BY startOffset ASC")
    fun getHighlightsByPage(documentId: String, pageNumber: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE documentId = :documentId AND fragmentId = :fragmentId")
    fun getHighlightsByFragment(documentId: String, fragmentId: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE documentId = :documentId ORDER BY timestamp DESC")
    suspend fun getHighlightsByDocumentSync(documentId: String): List<Highlight>

    @Query("SELECT * FROM highlights WHERE id = :id LIMIT 1")
    suspend fun getHighlightById(id: String): Highlight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlights(highlights: List<Highlight>)

    @Update
    suspend fun updateHighlight(highlight: Highlight)

    @Delete
    suspend fun deleteHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: String)

    @Query("DELETE FROM highlights WHERE documentId = :documentId AND fragmentId = :fragmentId")
    suspend fun deleteHighlightsByFragment(documentId: String, fragmentId: Int)

    @Query("DELETE FROM highlights WHERE documentId = :documentId")
    suspend fun deleteHighlightsByDocument(documentId: String)
}

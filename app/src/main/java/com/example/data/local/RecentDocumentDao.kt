package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDocumentDao {

    @Query("SELECT * FROM recent_documents ORDER BY lastAccessedTimestamp DESC")
    fun getAllRecentDocuments(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): RecentDocumentEntity?

    @Query("SELECT * FROM recent_documents WHERE title = :title LIMIT 1")
    suspend fun getDocumentByTitle(title: String): RecentDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(document: RecentDocumentEntity)

    @Query("UPDATE recent_documents SET lastPage = :lastPage, lastFragmentIndex = :lastFragmentIndex, lastAccessedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(
        id: String,
        lastPage: Int,
        lastFragmentIndex: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE recent_documents SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: String, newTitle: String)

    @Query("UPDATE recent_documents SET title = :title, author = :author, materia = :materia, year = :year, description = :description, collection = :collection, tagsJson = :tagsJson WHERE id = :id")
    suspend fun updateMetadata(
        id: String,
        title: String,
        author: String,
        materia: String,
        year: String,
        description: String,
        collection: String,
        tagsJson: String
    )

    @Query("UPDATE recent_documents SET generalNotes = :generalNotes WHERE id = :id")
    suspend fun updateGeneralNotes(id: String, generalNotes: String)

    @Query("UPDATE recent_documents SET customCoverUri = :customCoverUri, coverColor = :coverColor WHERE id = :id")
    suspend fun updateCover(id: String, customCoverUri: String?, coverColor: Long?)

    @Query("UPDATE recent_documents SET highlightsJson = :highlightsJson WHERE id = :id")
    suspend fun updateHighlights(id: String, highlightsJson: String)

    @Query("UPDATE recent_documents SET opinionsJson = :opinionsJson WHERE id = :id")
    suspend fun updateOpinions(id: String, opinionsJson: String)

    @Query("UPDATE recent_documents SET bookmarksJson = :bookmarksJson WHERE id = :id")
    suspend fun updateBookmarks(id: String, bookmarksJson: String)

    @Query("UPDATE recent_documents SET audioCommentsJson = :audioCommentsJson WHERE id = :id")
    suspend fun updateAudioComments(id: String, audioCommentsJson: String)

    @Query("UPDATE recent_documents SET questionsHistoryJson = :questionsHistoryJson WHERE id = :id")
    suspend fun updateQuestionsHistory(id: String, questionsHistoryJson: String)

    @Query("SELECT * FROM recent_documents")
    suspend fun getAllDocumentsList(): List<RecentDocumentEntity>

    @Query("DELETE FROM recent_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_documents")
    suspend fun clearAll()
}

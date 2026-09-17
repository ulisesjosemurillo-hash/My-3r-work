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

    @Query("DELETE FROM recent_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_documents")
    suspend fun clearAll()
}

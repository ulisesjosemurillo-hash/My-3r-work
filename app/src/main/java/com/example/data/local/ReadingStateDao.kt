package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStateDao {

    @Query("SELECT * FROM reading_states WHERE documentId = :documentId LIMIT 1")
    suspend fun getReadingStateForDocument(documentId: String): ReadingState?

    @Query("SELECT * FROM reading_states WHERE documentId = :documentId LIMIT 1")
    fun observeReadingState(documentId: String): Flow<ReadingState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingState(state: ReadingState)

    @Query("UPDATE reading_states SET lastReadFragmentIndex = :fragmentIndex, lastReadPage = :page, lastUpdated = :timestamp WHERE documentId = :documentId")
    suspend fun updateLastReadFragment(
        documentId: String,
        fragmentIndex: Int,
        page: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE reading_states SET isCompleted = :completed, lastUpdated = :timestamp WHERE documentId = :documentId")
    suspend fun setCompleted(documentId: String, completed: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM reading_states WHERE documentId = :documentId")
    suspend fun deleteReadingState(documentId: String)
}

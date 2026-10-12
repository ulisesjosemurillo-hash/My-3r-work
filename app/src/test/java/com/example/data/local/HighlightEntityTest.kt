package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HighlightEntityTest {

    private lateinit var database: AppDatabase
    private lateinit var highlightDao: HighlightDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        highlightDao = database.highlightDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveHighlights() = runBlocking {
        val docId = "doc_test_codigo_penal"
        val highlight1 = Highlight(
            id = "hl_1",
            documentId = docId,
            pageNumber = 1,
            fragmentId = 0,
            selectedText = "Artículo 1. Principio de legalidad",
            startOffset = 0,
            endOffset = 34,
            color = "#FFF59D",
            timestamp = 1000L
        )
        val highlight2 = Highlight(
            id = "hl_2",
            documentId = docId,
            pageNumber = 1,
            fragmentId = 1,
            selectedText = "Nadie podrá ser sancionado sin ley previa",
            startOffset = 5,
            endOffset = 45,
            color = "#80D8FF",
            timestamp = 2000L
        )
        val highlightOtherDoc = Highlight(
            id = "hl_3",
            documentId = "doc_another",
            pageNumber = 2,
            fragmentId = 5,
            selectedText = "Texto de otro documento",
            startOffset = 0,
            endOffset = 23,
            color = "#CCFF90",
            timestamp = 3000L
        )

        highlightDao.insertHighlight(highlight1)
        highlightDao.insertHighlight(highlight2)
        highlightDao.insertHighlight(highlightOtherDoc)

        val docHighlights = highlightDao.getHighlightsByDocument(docId).first()
        assertEquals(2, docHighlights.size)
        assertEquals("hl_1", docHighlights[0].id)
        assertEquals("Artículo 1. Principio de legalidad", docHighlights[0].selectedText)
        assertEquals(0, docHighlights[0].startOffset)
        assertEquals(34, docHighlights[0].endOffset)
        assertEquals("#FFF59D", docHighlights[0].color)

        val pageHighlights = highlightDao.getHighlightsByPage(docId, 1).first()
        assertEquals(2, pageHighlights.size)

        val fragHighlights = highlightDao.getHighlightsByFragment(docId, 1).first()
        assertEquals(1, fragHighlights.size)
        assertEquals("hl_2", fragHighlights[0].id)

        // Test deletion by id
        highlightDao.deleteHighlightById("hl_1")
        val remaining = highlightDao.getHighlightsByDocument(docId).first()
        assertEquals(1, remaining.size)
        assertEquals("hl_2", remaining[0].id)

        // Test deletion by fragment
        highlightDao.deleteHighlightsByFragment(docId, 1)
        val empty = highlightDao.getHighlightsByDocument(docId).first()
        assertTrue(empty.isEmpty())
    }
}

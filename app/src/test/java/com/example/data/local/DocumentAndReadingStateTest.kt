package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentAndReadingStateTest {

    private lateinit var database: AppDatabase
    private lateinit var documentDao: DocumentDao
    private lateinit var readingStateDao: ReadingStateDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        documentDao = database.documentDao()
        readingStateDao = database.readingStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDocumentMetadataAndPersistentReadingState() = runBlocking {
        val docId = "doc_legal_101"
        val document = Document(
            id = docId,
            title = "Contrato de Prestación de Servicios",
            subtitle = "Asesoría Jurídica y Tecnológica",
            totalPages = 5,
            totalFragments = 20,
            sourceType = "PDF"
        )
        documentDao.insertDocument(document)

        val readingState = ReadingState(
            documentId = docId,
            lastReadFragmentIndex = 7,
            lastReadPage = 2,
            isCompleted = false
        )
        readingStateDao.saveReadingState(readingState)

        // Retrieve document with reading state relation
        val docWithState = documentDao.getDocumentWithReadingState(docId)
        assertNotNull(docWithState)
        assertEquals("Contrato de Prestación de Servicios", docWithState!!.document.title)
        assertNotNull(docWithState.readingState)
        assertEquals(7, docWithState.readingState!!.lastReadFragmentIndex)
        assertEquals(2, docWithState.readingState!!.lastReadPage)

        // Update last read fragment index for persistent sessions
        readingStateDao.updateLastReadFragment(
            documentId = docId,
            fragmentIndex = 14,
            page = 4
        )

        val updatedState = readingStateDao.getReadingStateForDocument(docId)
        assertNotNull(updatedState)
        assertEquals(14, updatedState!!.lastReadFragmentIndex)
        assertEquals(4, updatedState.lastReadPage)

        // Check flow of documents
        val allDocs = documentDao.getAllDocuments().first()
        assertEquals(1, allDocs.size)
        assertEquals(docId, allDocs.first().id)
    }
}

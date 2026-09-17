package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.DocumentPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentDocumentRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RecentDocumentRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecentDocumentRepository(database.recentDocumentDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndRetrieveRecentDocument() = runBlocking {
        val pages = listOf(
            DocumentPage(numero = 1, texto = "Cláusula Primera: Objeto contractual."),
            DocumentPage(numero = 2, texto = "Cláusula Segunda: Precio y forma de pago.")
        )

        repository.saveOrUpdateDocument(
            id = "test_doc_1",
            title = "Contrato de Arrendamiento",
            subtitle = "Inmueble Comercial",
            pages = pages,
            totalFragments = 4,
            lastPage = 1,
            lastFragmentIndex = 0,
            sourceType = "SAMPLE"
        )

        val recents = repository.recentDocuments.first()
        assertEquals(1, recents.size)
        val doc = recents.first()
        assertEquals("Contrato de Arrendamiento", doc.title)
        assertEquals(2, doc.totalPages)

        // Actualizar progreso de lectura
        repository.updateReadingProgress("test_doc_1", 2, 3)
        val updatedRecents = repository.recentDocuments.first()
        assertEquals(2, updatedRecents.first().lastPage)
        assertEquals(3, updatedRecents.first().lastFragmentIndex)

        // Verificar deserialización de páginas
        val retrievedPages = repository.parsePages(doc.pagesJson)
        assertEquals(2, retrievedPages.size)
        assertEquals("Cláusula Primera: Objeto contractual.", retrievedPages[0].texto)

        // Eliminar documento
        repository.deleteDocument("test_doc_1")
        val emptyRecents = repository.recentDocuments.first()
        assertEquals(0, emptyRecents.size)
    }
}

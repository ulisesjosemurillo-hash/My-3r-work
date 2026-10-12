package com.example.util

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class PdfStructureParserTest {

    @Test
    fun parseDocument_detectsChaptersArticlesAndParagraphs() {
        val samplePages = listOf(
            DocumentPage(
                numero = 1,
                texto = """
                    CÓDIGO PENAL DE HONDURAS
                    
                    TÍTULO I: DE LAS DISPOSICIONES GENERALES
                    
                    CAPÍTULO I: APLICACIÓN DE LA LEY PENAL
                    
                    ARTÍCULO 1. Principio de legalidad.
                    Nadie puede ser penado por acciones u omisiones que no estén expresamente tipificadas en la ley.
                    
                    Este principio garantiza la seguridad jurídica y los derechos fundamentales del procesado.
                """.trimIndent()
            )
        )

        val (pageStructures, blocks) = PdfStructureParser.parseDocument("doc_1", samplePages)

        assertEquals(1, pageStructures.size)
        assertTrue(blocks.isNotEmpty())

        val chapterBlocks = blocks.filter { it.type == BlockType.CHAPTER_HEADER }
        assertTrue("Debe detectar encabezados de título/capítulo", chapterBlocks.isNotEmpty())
        assertTrue(chapterBlocks.any { it.text.contains("TÍTULO I") || it.text.contains("CAPÍTULO I") })

        val articleBlock = blocks.find { it.type == BlockType.ARTICLE_HEADER }
        assertNotNull("Debe detectar el ARTÍCULO 1", articleBlock)
        assertTrue(articleBlock!!.text.contains("ARTÍCULO 1"))

        val paragraphBlock = blocks.find { it.type == BlockType.PARAGRAPH }
        assertNotNull("Debe detectar párrafos normales de lectura", paragraphBlock)
        assertTrue(paragraphBlock!!.text.contains("principio de legalidad") || paragraphBlock.text.contains("Nadie puede ser penado"))
    }

    @Test
    fun parseDocument_detectsScannedPageWithoutExtractableText() {
        val samplePages = listOf(
            DocumentPage(
                numero = 1,
                texto = "" // Página vacía o sin texto OCR
            ),
            DocumentPage(
                numero = 2,
                texto = "[Página sin texto detectable (requiere OCR)]"
            )
        )

        val (pageStructures, blocks) = PdfStructureParser.parseDocument("doc_scanned", samplePages)

        assertEquals(2, pageStructures.size)
        assertTrue("La página 1 debe estar marcada como imagen escaneada", pageStructures[0].isScannedImage)
        assertTrue("La página 2 debe estar marcada como imagen escaneada", pageStructures[1].isScannedImage)

        val scannedBlocks = blocks.filter { it.type == BlockType.SCANNED_PAGE_ALERT }
        assertEquals(2, scannedBlocks.size)
        assertTrue(scannedBlocks[0].spokenText.contains("óptico de caracteres") || scannedBlocks[0].spokenText.contains("escaneada"))
    }

    @Test
    fun parseDocument_detectsStructuredTablesAndGeneratesAccessibleSpeech() {
        val tableText = """
            FALTAS Y SANCIONES
            
            Infracción | Grado | Sanción Aplicable
            Leve | Primer Grado | Multa de 500 Lempiras
            Grave | Segundo Grado | Suspensión temporal de licencia
            Muy Grave | Tercer Grado | Clausura definitiva del establecimiento
        """.trimIndent()

        val samplePages = listOf(
            DocumentPage(numero = 1, texto = tableText)
        )

        val (_, blocks) = PdfStructureParser.parseDocument("doc_table", samplePages)

        val tableBlock = blocks.find { it.type == BlockType.TABLE }
        assertNotNull("Debe detectar el bloque estructurado como TABLA", tableBlock)
        assertNotNull("Debe poseer datos StructuredTable", tableBlock!!.tableData)

        val table = tableBlock.tableData!!
        assertEquals(3, table.headers.size)
        assertEquals(3, table.rows.size)
        assertEquals("Infracción", table.headers[0])
        assertEquals("Sanción Aplicable", table.headers[2])

        // Comprobar la síntesis accesible para lectura verbal
        val spoken = table.toAccessibleSpokenText()
        assertTrue(spoken.contains("Tabla con 3 columnas y 3 filas"))
        assertTrue(spoken.contains("Columnas: Infracción, Grado, Sanción Aplicable"))
        assertTrue(spoken.contains("Infracción es Leve"))
        assertTrue(spoken.contains("Sanción Aplicable es Multa de 500 Lempiras"))
    }

    @Test
    fun buildOutlineTree_createsHierarchicalNodesFromHeadings() {
        val blocks = listOf(
            StructuredBlock(
                id = "b1",
                documentId = "d1",
                pageNumber = 1,
                fragmentId = 0,
                type = BlockType.CHAPTER_HEADER,
                level = 1,
                text = "CAPÍTULO I: De las Personas"
            ),
            StructuredBlock(
                id = "b2",
                documentId = "d1",
                pageNumber = 1,
                fragmentId = 1,
                type = BlockType.ARTICLE_HEADER,
                level = 2,
                text = "ARTÍCULO 1: Existencia de las personas naturales"
            ),
            StructuredBlock(
                id = "b3",
                documentId = "d1",
                pageNumber = 5,
                fragmentId = 10,
                type = BlockType.CHAPTER_HEADER,
                level = 1,
                text = "CAPÍTULO II: Del Matrimonio"
            )
        )

        val outline = PdfStructureParser.buildOutlineTree(blocks)

        assertEquals(3, outline.size)
        assertEquals("CAPÍTULO I: De las Personas", outline[0].title)
        assertEquals(1, outline[0].pageNumber)
        assertEquals(0, outline[0].fragmentId)

        assertEquals("CAPÍTULO II: Del Matrimonio", outline[2].title)
        assertEquals(5, outline[2].pageNumber)
        assertEquals(10, outline[2].fragmentId)
    }

    @Test
    fun parseDocument_detectsListsQuotesAndFootnotes() {
        val content = """
            DERECHOS RECONOCIDOS
            
            • Derecho a la vida y la integridad física.
            • Derecho a la presunción de inocencia.
            
            «El debido proceso es una garantía ineludible en un Estado de Derecho.»
            
            * Nota: Ratificado por sentencia constitucional 145-2021.
        """.trimIndent()

        val samplePages = listOf(DocumentPage(numero = 1, texto = content))
        val (_, blocks) = PdfStructureParser.parseDocument("doc_misc", samplePages)

        val listBlocks = blocks.filter { it.type == BlockType.LIST_ITEM }
        assertEquals("Debe detectar 2 elementos de lista con viñetas", 2, listBlocks.size)

        val quoteBlock = blocks.find { it.type == BlockType.QUOTE }
        assertNotNull("Debe detectar la cita textual entre comillas angulares", quoteBlock)

        val footnoteBlock = blocks.find { it.type == BlockType.FOOTNOTE }
        assertNotNull("Debe detectar la nota al pie con asterisco", footnoteBlock)
    }
}

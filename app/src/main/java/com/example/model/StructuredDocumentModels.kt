package com.example.model

import android.graphics.RectF
import java.util.UUID

/**
 * Tipos de bloques estructurales identificados en un libro o documento PDF.
 */
enum class BlockType(val displayName: String) {
    DOCUMENT_TITLE("Título del Documento"),
    CHAPTER_HEADER("Capítulo / Título Principal"),
    SECTION_HEADER("Sección / Subtítulo"),
    ARTICLE_HEADER("Artículo / Disposición"),
    PARAGRAPH("Párrafo"),
    LIST_ITEM("Elemento de Lista"),
    TABLE("Tabla Estructurada"),
    FOOTNOTE("Nota al Pie"),
    QUOTE("Cita Textual"),
    SCANNED_PAGE_ALERT("Página Escaneada")
}

/**
 * Representación de una celda de tabla.
 */
data class TableCell(
    val text: String,
    val isHeader: Boolean = false
)

/**
 * Representación de una tabla estructurada con celdas y filas.
 */
data class StructuredTable(
    val headers: List<String>,
    val rows: List<List<String>>,
    val caption: String = ""
) {
    /**
     * Genera una descripción verbal clara y accesible para sintetizadores de voz.
     */
    fun toAccessibleSpokenText(): String {
        val sb = StringBuilder()
        if (caption.isNotBlank()) {
            sb.append("Tabla: $caption. ")
        } else {
            sb.append("Tabla con ${headers.size} columnas y ${rows.size} filas. ")
        }
        if (headers.isNotEmpty()) {
            sb.append("Columnas: ${headers.joinToString(", ")}. ")
        }
        rows.forEachIndexed { rowIndex, row ->
            sb.append("Fila ${rowIndex + 1}: ")
            val rowParts = mutableListOf<String>()
            row.forEachIndexed { colIndex, cell ->
                val headerName = headers.getOrNull(colIndex)
                if (headerName != null && headerName.isNotBlank()) {
                    rowParts.add("$headerName es $cell")
                } else {
                    rowParts.add(cell)
                }
            }
            sb.append(rowParts.joinToString("; ")).append(". ")
        }
        return sb.toString().trim()
    }
}

/**
 * Bloque individual estructurado dentro de una página.
 */
data class StructuredBlock(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String = "",
    val pageNumber: Int = 1,
    val fragmentId: Int = 0,
    val type: BlockType = BlockType.PARAGRAPH,
    val level: Int = 0, // 1: Título/Capítulo, 2: Sección, 3: Artículo, 0: Párrafo regular
    val text: String,
    val spokenText: String = text,
    val tableData: StructuredTable? = null,
    val isAiInferred: Boolean = false,
    val confidence: Float = 1.0f,
    val orderIndex: Int = 0,
    val isSelectedForReading: Boolean = false
)

/**
 * Estructura completa de una página de PDF.
 */
data class PdfPageStructure(
    val pageNumber: Int,
    val isScannedImage: Boolean = false,
    val blocks: List<StructuredBlock> = emptyList(),
    val detectedHeaders: List<String> = emptyList(),
    val confidenceScore: Float = 1.0f
)

/**
 * Nodo de índice jerárquico del libro/PDF (Marcador u Outline).
 */
data class PdfOutlineNode(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pageNumber: Int,
    val fragmentId: Int,
    val level: Int = 0,
    val isAiInferred: Boolean = false,
    val children: List<PdfOutlineNode> = emptyList()
)

/**
 * Modos de visualización del lector.
 */
enum class ReaderViewMode(val label: String, val description: String) {
    STRUCTURED("Estructurada", "Texto semántico con títulos, párrafos y tablas legibles"),
    ORIGINAL_PAGE("Página Original", "Vista visual fidedigna del PDF con diseño original"),
    SPLIT_DUAL("Lado a Lado", "Página original y texto estructurado simultáneamente")
}

/**
 * Configuración de lectura en voz alta para elementos de la estructura.
 */
data class ReadingAnnouncementsConfig(
    val announceHeaders: Boolean = true,
    val announceTables: Boolean = true,
    val announceFootnotes: Boolean = false,
    val announcePageNumbers: Boolean = false,
    val skipRepeatedHeaders: Boolean = true
)

/**
 * Resultado enriquecido de búsqueda semántica con IA o literal.
 */
data class SemanticSearchResult(
    val id: String = UUID.randomUUID().toString(),
    val fragmentId: Int,
    val pageNumber: Int,
    val sectionTitle: String,
    val matchedText: String,
    val explanation: String = "",
    val relevanceScore: Float = 1.0f,
    val isExactMatch: Boolean = false
)

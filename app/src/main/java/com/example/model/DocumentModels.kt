package com.example.model

import java.util.UUID

enum class LibrarySortOrder(val label: String) {
    RECENT("Recientes"),
    TITLE_ASC("Nombre (A-Z)"),
    TITLE_DESC("Nombre (Z-A)"),
    AUTHOR("Por Autor"),
    PROGRESS("Por Progreso")
}

data class DocumentPage(
    val numero: Int,
    val texto: String
)

data class FragmentoLectura(
    val id: Int,
    val pagina: Int,
    val texto: String
)

data class HighlightItem(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String = "",
    val pageNumber: Int = 1,
    val fragmentId: Int = 0,
    val text: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val colorHex: String = "#FFF59D",
    val isUnderline: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class FlashcardItem(
    val id: String = UUID.randomUUID().toString(),
    val pregunta: String,
    val respuesta: String,
    val referenciaLegal: String = "",
    val pagina: Int = 1
)

data class ExamQuestion(
    val id: String = UUID.randomUUID().toString(),
    val pregunta: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val explicacion: String,
    val referenciaLegal: String = ""
)

data class PositionHistoryItem(
    val fragmentIndex: Int,
    val pageNumber: Int,
    val snippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FragmentOpinion(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String = "",
    val fragmentId: Int,
    val pagina: Int,
    val textoFragmento: String,
    val opinion: String,
    val tag: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class BookBookmark(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String = "",
    val fragmentId: Int,
    val pagina: Int,
    val snippet: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AudioComment(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String = "",
    val fragmentId: Int = 0,
    val pagina: Int = 1,
    val snippet: String = "",
    val audioPath: String = "",
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchResultItem(
    val documentId: String,
    val documentTitle: String,
    val pageNumber: Int,
    val fragmentIndex: Int,
    val matchedSnippet: String,
    val isTitleMatch: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val remitente: String,
    val texto: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean = false,
    val referencedPages: List<Int> = emptyList(),
    val referencedFragmentIndex: Int = -1,
    val isAiThinking: Boolean = false
)

data class SampleDocument(
    val title: String,
    val subtitle: String,
    val pages: List<DocumentPage>,
    val coverColor: Long? = null
)

object SampleDocumentRepository {

    val hondurasSamples: List<SampleDocument> = listOf(
        com.example.data.CodigoPenalHondurasData.document
    )

    val samples: List<SampleDocument> = hondurasSamples

    fun crearFragmentos(paginas: List<DocumentPage>): List<FragmentoLectura> {
        val fragmentos = mutableListOf<FragmentoLectura>()
        var globalIndex = 0

        paginas.forEach { pagina ->
            val palabras = pagina.texto.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val bloquePalabras = mutableListOf<String>()

            for (palabra in palabras) {
                bloquePalabras.add(palabra)
                // Aprox 40 palabras por bloque
                if (bloquePalabras.size >= 40) {
                    val textoBloque = bloquePalabras.joinToString(" ")
                    fragmentos.add(
                        FragmentoLectura(
                            id = globalIndex++,
                            pagina = pagina.numero,
                            texto = textoBloque
                        )
                    )
                    bloquePalabras.clear()
                }
            }

            if (bloquePalabras.isNotEmpty()) {
                val textoBloque = bloquePalabras.joinToString(" ")
                fragmentos.add(
                    FragmentoLectura(
                        id = globalIndex++,
                        pagina = pagina.numero,
                        texto = textoBloque
                    )
                )
            }
        }

        return fragmentos
    }
}


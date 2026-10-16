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

data class DocumentIndexEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pageNumber: Int,
    val fragmentIndex: Int
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
    val timestamp: Long = System.currentTimeMillis(),
    val selectedText: String = textoFragmento,
    val startOffset: Int = 0,
    val endOffset: Int = 0
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
    val timestamp: Long = System.currentTimeMillis(),
    val selectedText: String = snippet,
    val startOffset: Int = 0,
    val endOffset: Int = 0
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
    val coverColor: Long? = null,
    val materia: String = "Derecho",
    val author: String = "Poder Legislativo",
    val year: String = "2024"
)

object SampleDocumentRepository {

    val hondurasSamples: List<SampleDocument> = listOf(
        com.example.data.CodigoPenalHondurasData.document.copy(
            materia = "Derecho Penal",
            author = "Congreso Nacional",
            year = "2017"
        ),
        com.example.data.ConstitucionHondurasData.document.copy(
            materia = "Derecho Constitucional",
            author = "Asamblea Nacional Constituyente",
            year = "1982"
        ),
        com.example.data.LeyTransitoHondurasData.document.copy(
            materia = "Derecho Administrativo y Vial",
            author = "Poder Legislativo",
            year = "2005"
        ),
        com.example.data.CodigoTrabajoHondurasData.document.copy(
            materia = "Derecho Laboral",
            author = "Congreso Nacional",
            year = "1959"
        ),
        com.example.data.CodigoCivilHondurasData.document.copy(
            materia = "Derecho Civil y Contratos",
            author = "Congreso Nacional",
            year = "1906"
        ),
        com.example.data.CodigoComercioHondurasData.document.copy(
            materia = "Derecho Mercantil y Corporativo",
            author = "Congreso Nacional",
            year = "1950"
        ),
        com.example.data.CodigoProcesalPenalHondurasData.document.copy(
            materia = "Derecho Procesal Penal",
            author = "Congreso Nacional",
            year = "2002"
        ),
        com.example.data.LeyCiberseguridadHondurasData.document.copy(
            materia = "Derecho Informático y Privacidad",
            author = "Congreso Nacional",
            year = "2024"
        ),
        com.example.data.TratadoDerechosHumanosData.document.copy(
            materia = "Derechos Humanos y Tratados",
            author = "OEA - Pacto de San José",
            year = "1969"
        ),
        com.example.data.CodigoNinezHondurasData.document.copy(
            materia = "Derecho de Familia y Niñez",
            author = "Congreso Nacional",
            year = "1996"
        )
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

enum class ChatProcessState(val label: String, val icon: String) {
    READY("Listo", "✓"),
    LISTENING("Escuchando voz...", "🎙"),
    TRANSCRIBING("Transcribiendo...", "✍"),
    SEARCHING("Buscando en el documento...", "🔎"),
    GENERATING("Generando respuesta...", "⚖"),
    PREPARING_AUDIO("Preparando audio...", "🔊"),
    PLAYING_AUDIO("Leyendo respuesta...", "▶")
}

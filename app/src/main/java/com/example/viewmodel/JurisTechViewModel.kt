package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JurisTechAiService
import com.example.data.local.AppDatabase
import com.example.data.local.RecentDocumentEntity
import com.example.data.local.RecentDocumentRepository
import com.example.model.*
import com.example.tts.TtsManager
import com.example.util.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JurisTechUiState(
    val currentDocumentId: String = "",
    val documentTitle: String = "",
    val fileNameLabel: String = "Seleccionar PDF",
    val paginas: List<DocumentPage> = emptyList(),
    val fragmentosLectura: List<FragmentoLectura> = emptyList(),
    val indiceActual: Int = 0,
    val lecturaActiva: Boolean = false,
    val lecturaPausadaPorPregunta: Boolean = false,
    val voiceStatus: String = "Esperando documento...",
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAskingAi: Boolean = false,
    val speechRate: Float = 1.0f,
    val showContinueButton: Boolean = false,
    val isContinueEnabled: Boolean = false,
    val isProcessingPdf: Boolean = false,
    val pdfProgressPercent: Int = 0,
    val pdfProgressText: String = "",
    val pdfErrorMessage: String? = null,
    val recentDocuments: List<RecentDocumentEntity> = emptyList(),
    val customCoverUri: String? = null,
    val coverColor: Long? = null,
    val highlights: Set<Int> = emptySet(),
    val opinions: List<FragmentOpinion> = emptyList(),
    val bookmarks: List<BookBookmark> = emptyList(),
    val audioComments: List<AudioComment> = emptyList(),
    val isRecordingAudio: Boolean = false,
    val recordingFragmentId: Int? = null,
    val playingAudioPath: String? = null,
    val inDocumentSearchQuery: String = "",
    val inDocumentSearchResults: List<Int> = emptyList(),
    val inDocumentSearchCurrentIndex: Int = -1,
    val showInDocumentSearchBar: Boolean = false,
    val currentTab: Int = 0, // 0: Biblioteca, 1: Buscar, 2: Ajustes
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val fontSizeScale: Float = 1.0f,
    val lineSpacing: Float = 1.5f,
    val selectedCollection: String = "Todas",
    val librarySearchQuery: String = "",
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.RECENT,
    val selectedTagFilter: String? = null,
    val documentAuthor: String = "",
    val documentMateria: String = "",
    val documentYear: String = "",
    val documentDescription: String = "",
    val documentCollection: String = "",
    val documentTags: List<String> = emptyList(),
    val documentGeneralNotes: String = "",
    val activeHighlightColorHex: String = "#FFF59D",
    val isUnderlineMode: Boolean = false,
    val highlightItems: List<HighlightItem> = emptyList(),
    val positionHistory: List<PositionHistoryItem> = emptyList(),
    val activeTtsVoice: String = "",
    val availableTtsVoices: List<String> = emptyList(),
    val studySummary: String = "",
    val studyOutline: List<String> = emptyList(),
    val studyExamQuestions: List<ExamQuestion> = emptyList(),
    val studyFlashcards: List<FlashcardItem> = emptyList(),
    val studyConcepts: List<Pair<String, String>> = emptyList(),
    val isGeneratingStudyTool: Boolean = false,
    val studyToolType: String = "SUMMARY",
    val examUserAnswers: Map<String, Int> = emptyMap(),
    val examScore: Pair<Int, Int>? = null,
    val flashcardCurrentIndex: Int = 0,
    val flashcardFlipped: Boolean = false,
    val podcastText: String = "",
    val isPodcastPlaying: Boolean = false,
    val comparisonResult: String = "",
    val isComparingDocs: Boolean = false,
    val legalReportText: String = ""
)

class JurisTechViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(JurisTechUiState())
    val uiState: StateFlow<JurisTechUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application)
    private val repository = RecentDocumentRepository(database.recentDocumentDao())

    private val aiService = JurisTechAiService()
    private var ttsManager: TtsManager? = null
    private val audioRecordManager = com.example.util.AudioRecordManager(application)
    private val prefs = application.getSharedPreferences("juristech_prefs", android.content.Context.MODE_PRIVATE)

    init {
        val savedSpeechRate = prefs.getFloat("speech_rate", 1.0f)
        val savedFontSizeScale = prefs.getFloat("font_size_scale", 1.0f)
        val savedLineSpacing = prefs.getFloat("line_spacing", 1.5f)
        val savedVoice = prefs.getString("tts_voice", "") ?: ""
        _uiState.value = _uiState.value.copy(
            speechRate = savedSpeechRate,
            fontSizeScale = savedFontSizeScale,
            lineSpacing = savedLineSpacing,
            activeTtsVoice = savedVoice
        )

        ttsManager = TtsManager(
            context = application,
            onFragmentStarted = { fragmentId ->
                val state = _uiState.value
                val frag = state.fragmentosLectura.getOrNull(state.indiceActual)
                if (frag != null && frag.id == fragmentId) {
                    _uiState.value = _uiState.value.copy(
                        voiceStatus = "🔊 Leyendo página ${frag.pagina}"
                    )
                }
            },
            onFragmentCompleted = { fragmentId ->
                val state = _uiState.value
                if (state.lecturaActiva) {
                    val nextIndex = state.indiceActual + 1
                    if (nextIndex < state.fragmentosLectura.size) {
                        _uiState.value = _uiState.value.copy(indiceActual = nextIndex)
                        persistirProgresoActual(nextIndex)
                        leerFragmentoActual()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            lecturaActiva = false,
                            voiceStatus = "Lectura finalizada.",
                            showContinueButton = false
                        )
                        persistirProgresoActual(state.indiceActual)
                    }
                }
            },
            onErrorOccurred = { _, errorMsg ->
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Error de voz: $errorMsg"
                )
            }
        )

        // Cargar voces disponibles
        recargarVocesDisponibles()

        // Observar la lista reactiva de documentos recientes almacenados en Room
        viewModelScope.launch {
            repository.recentDocuments.collect { recents ->
                _uiState.value = _uiState.value.copy(recentDocuments = recents)

                // Eliminar cualquier documento de muestra anterior que no sea el Código Penal
                val oldSamples = recents.filter {
                    it.sourceType == "SAMPLE" && !it.title.contains("Código Penal", ignoreCase = true)
                }
                if (oldSamples.isNotEmpty()) {
                    oldSamples.forEach { repository.deleteDocument(it.id) }
                }

                val hasCodigoPenal = recents.any { it.title.contains("Código Penal", ignoreCase = true) }
                if (!hasCodigoPenal) {
                    sembrarCodigoPenal()
                }
            }
        }
    }

    fun sembrarCodigoPenal() {
        viewModelScope.launch {
            val sample = com.example.data.CodigoPenalHondurasData.document
            val sampleId = "honduras_codigo_penal_130_2017"
            val fragments = SampleDocumentRepository.crearFragmentos(sample.pages)
            repository.saveOrUpdateDocument(
                id = sampleId,
                title = sample.title,
                subtitle = sample.subtitle,
                pages = sample.pages,
                totalFragments = fragments.size,
                lastPage = 1,
                lastFragmentIndex = 0,
                sourceType = "SAMPLE",
                coverColor = sample.coverColor
            )
        }
    }

    fun sembrarCodigosHonduras() {
        sembrarCodigoPenal()
    }

    fun restaurarCodigosHonduras() {
        sembrarCodigoPenal()
    }

    fun setCurrentTab(tab: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setFontSizeScale(scale: Float) {
        _uiState.value = _uiState.value.copy(fontSizeScale = scale)
        prefs.edit().putFloat("font_size_scale", scale).apply()
    }

    fun cargarSample(sample: SampleDocument) {
        val sampleId = "sample_" + sample.title.hashCode().toString()
        cargarDocumentoCompleto(
            id = sampleId,
            titulo = sample.title,
            subtitulo = sample.subtitle,
            paginas = sample.pages,
            sourceType = "SAMPLE",
            coverColor = sample.coverColor
        )
    }

    fun cargarDocumentoPersonalizado(
        titulo: String,
        paginas: List<DocumentPage>,
        subtitulo: String = "Documento importado",
        customCoverUri: String? = null,
        coverColor: Long? = null
    ) {
        val docId = "doc_" + System.currentTimeMillis()
        cargarDocumentoCompleto(
            id = docId,
            titulo = titulo,
            subtitulo = subtitulo,
            paginas = paginas,
            sourceType = "IMPORTED",
            customCoverUri = customCoverUri,
            coverColor = coverColor
        )
    }

    private fun cargarDocumentoCompleto(
        id: String,
        titulo: String,
        subtitulo: String,
        paginas: List<DocumentPage>,
        sourceType: String,
        customCoverUri: String? = null,
        coverColor: Long? = null
    ) {
        ttsManager?.stop()
        val fragmentos = SampleDocumentRepository.crearFragmentos(paginas)

        _uiState.value = _uiState.value.copy(
            currentDocumentId = id,
            documentTitle = titulo,
            paginas = paginas,
            fragmentosLectura = fragmentos,
            indiceActual = 0,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            customCoverUri = customCoverUri,
            coverColor = coverColor,
            highlights = emptySet(),
            opinions = emptyList(),
            bookmarks = emptyList(),
            voiceStatus = "Documento listo para leer.",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Documento '$titulo' cargado (${paginas.size} páginas, ${fragmentos.size} fragmentos). Puedes comenzar la lectura con '▶ Leer' o hacer preguntas en cualquier momento."
                )
            )
        )

        // Guardar documento completo y posición inicial en Room
        viewModelScope.launch {
            repository.saveOrUpdateDocument(
                id = id,
                title = titulo,
                subtitle = subtitulo,
                pages = paginas,
                totalFragments = fragmentos.size,
                lastPage = 1,
                lastFragmentIndex = 0,
                sourceType = sourceType,
                customCoverUri = customCoverUri,
                coverColor = coverColor
            )
        }
    }

    /**
     * Retoma la lectura y consulta de un documento guardado en Room
     * restaurando exactamente la página y fragmento donde el usuario lo dejó,
     * además de carátulas, resaltados, opiniones y marcadores.
     */
    fun retomarDocumento(entity: RecentDocumentEntity) {
        ttsManager?.stop()

        var paginas = repository.parsePages(entity.pagesJson)
        if (paginas.isEmpty()) {
            val sampleMatch = SampleDocumentRepository.samples.firstOrNull { it.title == entity.title }
            if (sampleMatch != null) {
                paginas = sampleMatch.pages
            }
        }

        if (paginas.isEmpty()) return

        val fragmentos = SampleDocumentRepository.crearFragmentos(paginas)
        val indiceRestaurado = entity.lastFragmentIndex.coerceIn(0, maxOf(0, fragmentos.size - 1))
        val paginaRestaurada = fragmentos.getOrNull(indiceRestaurado)?.pagina ?: entity.lastPage

        val highlights = repository.parseHighlights(entity.highlightsJson)
        val opinions = repository.parseOpinions(entity.opinionsJson)
        val bookmarks = repository.parseBookmarks(entity.bookmarksJson)
        val audioComments = repository.parseAudioComments(entity.audioCommentsJson)
        val savedQuestions = repository.parseQuestions(entity.questionsHistoryJson)

        val initialChat = if (savedQuestions.isNotEmpty()) {
            savedQuestions
        } else {
            listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Has retomado '${entity.title}'. Posición actual: Página $paginaRestaurada de ${paginas.size} (fragmento ${indiceRestaurado + 1}/${fragmentos.size}).",
                    isUser = false
                )
            )
        }

        val tagsList = try {
            val arr = org.json.JSONArray(entity.tagsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (_: Exception) { emptyList<String>() }

        _uiState.value = _uiState.value.copy(
            currentDocumentId = entity.id,
            documentTitle = entity.title,
            paginas = paginas,
            fragmentosLectura = fragmentos,
            indiceActual = indiceRestaurado,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            customCoverUri = entity.customCoverUri,
            coverColor = entity.coverColor,
            highlights = highlights,
            opinions = opinions,
            bookmarks = bookmarks,
            audioComments = audioComments,
            voiceStatus = "Lectura retomada en Página $paginaRestaurada (${indiceRestaurado + 1}/${fragmentos.size}).",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = initialChat,
            documentAuthor = entity.author,
            documentMateria = entity.materia,
            documentYear = entity.year,
            documentDescription = entity.description,
            documentCollection = entity.collection,
            documentTags = tagsList,
            documentGeneralNotes = entity.generalNotes
        )

        // Actualizar fecha de acceso reciente en Room
        viewModelScope.launch {
            repository.updateReadingProgress(entity.id, paginaRestaurada, indiceRestaurado)
        }
    }

    fun cambiarNombreDocumento(id: String, nuevoTitulo: String) {
        val tituloLimpio = nuevoTitulo.trim()
        if (tituloLimpio.isBlank()) return

        if (_uiState.value.currentDocumentId == id) {
            _uiState.value = _uiState.value.copy(documentTitle = tituloLimpio)
        }

        viewModelScope.launch {
            repository.renameDocument(id, tituloLimpio)
        }
    }

    fun cambiarCaratulaDocumento(id: String, customCoverUri: String?, coverColor: Long?) {
        var finalCoverUri = customCoverUri
        if (customCoverUri != null && customCoverUri.startsWith("content://")) {
            try {
                val context = getApplication<Application>()
                val coversDir = java.io.File(context.filesDir, "covers").apply { mkdirs() }
                val targetFile = java.io.File(coversDir, "cover_${id}.jpg")
                context.contentResolver.openInputStream(Uri.parse(customCoverUri))?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                finalCoverUri = Uri.fromFile(targetFile).toString()
            } catch (_: Exception) {}
        }

        if (_uiState.value.currentDocumentId == id) {
            _uiState.value = _uiState.value.copy(
                customCoverUri = finalCoverUri,
                coverColor = coverColor
            )
        }

        val updatedList = _uiState.value.recentDocuments.map { doc ->
            if (doc.id == id) doc.copy(customCoverUri = finalCoverUri, coverColor = coverColor) else doc
        }
        _uiState.value = _uiState.value.copy(recentDocuments = updatedList)

        viewModelScope.launch {
            repository.updateCover(id, finalCoverUri, coverColor)
        }
    }

    fun toggleResaltado(fragmentId: Int) {
        val currentHighlights = _uiState.value.highlights.toMutableSet()
        if (currentHighlights.contains(fragmentId)) {
            currentHighlights.remove(fragmentId)
        } else {
            currentHighlights.add(fragmentId)
        }
        _uiState.value = _uiState.value.copy(highlights = currentHighlights)

        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            viewModelScope.launch {
                repository.updateHighlights(docId, currentHighlights)
            }
        }
    }

    fun guardarOpinion(fragmentId: Int, pagina: Int, snippet: String, opinionTexto: String, tag: String = "") {
        val textoLimpio = opinionTexto.trim()
        if (textoLimpio.isBlank()) return

        val currentList = _uiState.value.opinions.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.fragmentId == fragmentId }
        val newOpinion = FragmentOpinion(
            id = if (existingIndex >= 0) currentList[existingIndex].id else "op_" + System.currentTimeMillis(),
            documentId = _uiState.value.currentDocumentId,
            fragmentId = fragmentId,
            pagina = pagina,
            textoFragmento = snippet,
            opinion = textoLimpio,
            tag = tag.trim(),
            timestamp = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = newOpinion
        } else {
            currentList.add(newOpinion)
        }

        _uiState.value = _uiState.value.copy(opinions = currentList)

        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            viewModelScope.launch {
                repository.updateOpinions(docId, currentList)
            }
        }
    }

    fun eliminarOpinion(opinionId: String) {
        val currentList = _uiState.value.opinions.filterNot { it.id == opinionId }
        _uiState.value = _uiState.value.copy(opinions = currentList)

        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            viewModelScope.launch {
                repository.updateOpinions(docId, currentList)
            }
        }
    }

    fun iniciarGrabacionAudio(fragmentId: Int) {
        val docId = _uiState.value.currentDocumentId
        if (docId.isBlank()) return
        val file = audioRecordManager.startRecording(docId, fragmentId)
        if (file != null) {
            _uiState.value = _uiState.value.copy(
                isRecordingAudio = true,
                recordingFragmentId = fragmentId
            )
        }
    }

    fun detenerGrabacionAudio(fragmentId: Int, pagina: Int, snippet: String) {
        val (file, duration) = audioRecordManager.stopRecording()
        _uiState.value = _uiState.value.copy(
            isRecordingAudio = false,
            recordingFragmentId = null
        )
        if (file != null && file.exists()) {
            val newAudio = AudioComment(
                id = "aud_" + System.currentTimeMillis(),
                documentId = _uiState.value.currentDocumentId,
                fragmentId = fragmentId,
                pagina = pagina,
                snippet = snippet,
                audioPath = file.absolutePath,
                durationSeconds = duration,
                timestamp = System.currentTimeMillis()
            )
            val updated = _uiState.value.audioComments + newAudio
            _uiState.value = _uiState.value.copy(audioComments = updated)
            val docId = _uiState.value.currentDocumentId
            if (docId.isNotBlank()) {
                viewModelScope.launch {
                    repository.updateAudioComments(docId, updated)
                }
            }
        }
    }

    fun cancelarGrabacionAudio() {
        audioRecordManager.cancelRecording()
        _uiState.value = _uiState.value.copy(
            isRecordingAudio = false,
            recordingFragmentId = null
        )
    }

    fun reproducirAudio(audioPath: String) {
        _uiState.value = _uiState.value.copy(playingAudioPath = audioPath)
        audioRecordManager.playAudio(audioPath) {
            _uiState.value = _uiState.value.copy(playingAudioPath = null)
        }
    }

    fun detenerReproduccionAudio() {
        audioRecordManager.stopPlayback()
        _uiState.value = _uiState.value.copy(playingAudioPath = null)
    }

    fun eliminarAudioComentario(id: String) {
        val item = _uiState.value.audioComments.firstOrNull { it.id == id }
        if (item != null) {
            audioRecordManager.deleteAudioFile(item.audioPath)
            val updated = _uiState.value.audioComments.filterNot { it.id == id }
            _uiState.value = _uiState.value.copy(audioComments = updated)
            val docId = _uiState.value.currentDocumentId
            if (docId.isNotBlank()) {
                viewModelScope.launch {
                    repository.updateAudioComments(docId, updated)
                }
            }
        }
    }

    fun abrirBusquedaEnDocumento() {
        _uiState.value = _uiState.value.copy(showInDocumentSearchBar = true)
    }

    fun cerrarBusquedaEnDocumento() {
        _uiState.value = _uiState.value.copy(
            showInDocumentSearchBar = false,
            inDocumentSearchQuery = "",
            inDocumentSearchResults = emptyList(),
            inDocumentSearchCurrentIndex = -1
        )
    }

    fun buscarEnDocumentoActual(query: String) {
        val clean = query.trim()
        _uiState.value = _uiState.value.copy(inDocumentSearchQuery = clean)
        if (clean.isBlank()) {
            _uiState.value = _uiState.value.copy(
                inDocumentSearchResults = emptyList(),
                inDocumentSearchCurrentIndex = -1
            )
            return
        }

        val matches = mutableListOf<Int>()
        _uiState.value.fragmentosLectura.forEachIndexed { index, frag ->
            if (frag.texto.contains(clean, ignoreCase = true)) {
                matches.add(index)
            }
        }

        val firstIndex = if (matches.isNotEmpty()) 0 else -1
        _uiState.value = _uiState.value.copy(
            inDocumentSearchResults = matches,
            inDocumentSearchCurrentIndex = firstIndex
        )
        if (firstIndex >= 0) {
            saltarAFragmento(matches[firstIndex])
        }
    }

    fun irASiguienteResultado() {
        val results = _uiState.value.inDocumentSearchResults
        if (results.isEmpty()) return
        val next = (_uiState.value.inDocumentSearchCurrentIndex + 1) % results.size
        _uiState.value = _uiState.value.copy(inDocumentSearchCurrentIndex = next)
        saltarAFragmento(results[next])
    }

    fun irAAnteriorResultado() {
        val results = _uiState.value.inDocumentSearchResults
        if (results.isEmpty()) return
        val prev = if (_uiState.value.inDocumentSearchCurrentIndex <= 0) results.size - 1 else _uiState.value.inDocumentSearchCurrentIndex - 1
        _uiState.value = _uiState.value.copy(inDocumentSearchCurrentIndex = prev)
        saltarAFragmento(results[prev])
    }

    fun toggleMarcador(fragmentId: Int, pagina: Int, snippet: String) {
        val currentBookmarks = _uiState.value.bookmarks.toMutableList()
        val existingIndex = currentBookmarks.indexOfFirst { it.fragmentId == fragmentId }

        if (existingIndex >= 0) {
            currentBookmarks.removeAt(existingIndex)
        } else {
            currentBookmarks.add(
                BookBookmark(
                    id = "bm_" + System.currentTimeMillis(),
                    documentId = _uiState.value.currentDocumentId,
                    fragmentId = fragmentId,
                    pagina = pagina,
                    snippet = snippet,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        _uiState.value = _uiState.value.copy(bookmarks = currentBookmarks)

        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            viewModelScope.launch {
                repository.updateBookmarks(docId, currentBookmarks)
            }
        }
    }

    fun eliminarMarcador(bookmarkId: String) {
        val currentBookmarks = _uiState.value.bookmarks.filter { it.id != bookmarkId }
        _uiState.value = _uiState.value.copy(bookmarks = currentBookmarks)
        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            viewModelScope.launch {
                repository.updateBookmarks(docId, currentBookmarks)
            }
        }
    }

    fun esMarcadorActual(): Boolean {
        val fragmentoActual = _uiState.value.fragmentosLectura.getOrNull(_uiState.value.indiceActual) ?: return false
        return _uiState.value.bookmarks.any { it.fragmentId == fragmentoActual.id }
    }

    fun saltarAPagina(numeroPagina: Int) {
        val state = _uiState.value
        val index = state.fragmentosLectura.indexOfFirst { it.pagina == numeroPagina }
        if (index >= 0) {
            saltarAFragmento(index)
        }
    }

    fun buscarEnBiblioteca(query: String) {
        val cleanQuery = query.trim()
        _uiState.value = _uiState.value.copy(searchQuery = cleanQuery)

        if (cleanQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            val results = repository.searchInDocuments(cleanQuery)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    /**
     * Elimina un documento de la lista de recientes en Room y limpia sus archivos
     */
    fun eliminarDocumentoReciente(id: String) {
        audioRecordManager.deleteAllAudioForDocument(id)
        try {
            val context = getApplication<Application>()
            val coverFile = java.io.File(context.filesDir, "covers/cover_${id}.jpg")
            if (coverFile.exists()) coverFile.delete()
        } catch (_: Exception) {}

        if (_uiState.value.currentDocumentId == id) {
            cerrarDocumento()
        }

        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun procesarArchivoPdf(uri: Uri, nombreArchivo: String) {
        ttsManager?.stop()
        limpiarError()

        _uiState.value = _uiState.value.copy(
            isProcessingPdf = true,
            pdfProgressPercent = 0,
            pdfProgressText = "Abriendo archivo...",
            fileNameLabel = nombreArchivo
        )

        viewModelScope.launch {
            try {
                val isTxt = nombreArchivo.lowercase().endsWith(".txt")
                val isPdf = nombreArchivo.lowercase().endsWith(".pdf") || uri.toString().contains("pdf", ignoreCase = true)

                if (isTxt) {
                    _uiState.value = _uiState.value.copy(
                        pdfProgressPercent = 40,
                        pdfProgressText = "Leyendo texto plano..."
                    )
                    val texto = withContext(Dispatchers.IO) {
                        getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: ""
                    }
                    if (texto.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isProcessingPdf = false,
                            pdfErrorMessage = "El archivo de texto está vacío."
                        )
                        return@launch
                    }
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfProgressPercent = 100,
                        pdfProgressText = "Documento procesado correctamente."
                    )
                    procesarTextoImportado(nombreArchivo, texto)
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    pdfProgressText = "Cargando documento PDF...",
                    pdfProgressPercent = 5
                )

                val result = PdfExtractor.extractTextFromPdf(
                    context = getApplication(),
                    uri = uri,
                    onProgress = { current, total, text ->
                        val percent = ((current.toFloat() / total) * 100).toInt().coerceIn(5, 98)
                        _uiState.value = _uiState.value.copy(
                            pdfProgressPercent = percent,
                            pdfProgressText = text
                        )
                    }
                )

                result.onSuccess { paginas ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfProgressPercent = 100,
                        pdfProgressText = "Documento procesado correctamente."
                    )
                    cargarDocumentoPersonalizado(
                        titulo = nombreArchivo,
                        paginas = paginas,
                        subtitulo = "${paginas.size} páginas procesadas"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfErrorMessage = "No se pudo abrir este PDF. Puede estar dañado, protegido, ser un PDF escaneado o no ser compatible.",
                        pdfProgressText = "No se pudo procesar el documento."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingPdf = false,
                    pdfErrorMessage = "Error al procesar el archivo: ${e.localizedMessage ?: "Error de lectura"}",
                    pdfProgressText = "No se pudo procesar el documento."
                )
            }
        }
    }

    fun cerrarDocumento() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            currentDocumentId = "",
            documentTitle = "",
            paginas = emptyList(),
            fragmentosLectura = emptyList(),
            indiceActual = 0,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            voiceStatus = "Esperando documento...",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = emptyList()
        )
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(pdfErrorMessage = null)
    }

    fun limpiarTodaLaInformacion() {
        ttsManager?.stop()
        viewModelScope.launch {
            repository.clearAll()
        }
        _uiState.value = JurisTechUiState(
            voiceStatus = "Esperando documento...",
            fileNameLabel = "Seleccionar PDF",
            chatMessages = emptyList()
        )
    }

    fun procesarTextoImportado(titulo: String, textoCompleto: String) {
        if (textoCompleto.isBlank()) return

        // Dividir en páginas según párrafos o bloques de longitud
        val lineas = textoCompleto.split("\n\n").filter { it.isNotBlank() }
        val paginasCreadas = mutableListOf<DocumentPage>()
        var bufferTexto = StringBuilder()
        var numeroPagina = 1

        for (linea in lineas) {
            bufferTexto.append(linea).append("\n\n")
            // ~200 palabras por página aproximado
            if (bufferTexto.split(Regex("\\s+")).size >= 180) {
                paginasCreadas.add(
                    DocumentPage(
                        numero = numeroPagina++,
                        texto = bufferTexto.toString().trim()
                    )
                )
                bufferTexto = StringBuilder()
            }
        }

        if (bufferTexto.isNotBlank()) {
            paginasCreadas.add(
                DocumentPage(
                    numero = numeroPagina,
                    texto = bufferTexto.toString().trim()
                )
            )
        }

        if (paginasCreadas.isEmpty()) {
            paginasCreadas.add(DocumentPage(numero = 1, texto = textoCompleto.trim()))
        }

        cargarDocumentoPersonalizado(titulo, paginasCreadas)
    }

    fun iniciarLectura() {
        val state = _uiState.value
        if (state.fragmentosLectura.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            lecturaActiva = true,
            lecturaPausadaPorPregunta = false,
            showContinueButton = false
        )

        leerFragmentoActual()
    }

    private fun leerFragmentoActual() {
        val state = _uiState.value
        if (!state.lecturaActiva) return

        if (state.indiceActual >= state.fragmentosLectura.size) {
            _uiState.value = _uiState.value.copy(
                lecturaActiva = false,
                voiceStatus = "Lectura finalizada."
            )
            return
        }

        ttsManager?.stop()

        val fragmento = state.fragmentosLectura[state.indiceActual]
        _uiState.value = _uiState.value.copy(
            voiceStatus = "🔊 Leyendo página ${fragmento.pagina}"
        )

        ttsManager?.speakFragment(fragmento.id, fragmento.texto)
    }

    private fun persistirProgresoActual(indice: Int) {
        val state = _uiState.value
        val id = state.currentDocumentId
        if (id.isNotBlank() && state.fragmentosLectura.isNotEmpty()) {
            val frag = state.fragmentosLectura.getOrNull(indice)
            val pag = frag?.pagina ?: 1
            viewModelScope.launch {
                repository.updateReadingProgress(id, pag, indice)
            }
        }
    }

    fun pausarManual() {
        _uiState.value = _uiState.value.copy(
            lecturaActiva = false,
            voiceStatus = "⏸ Lectura pausada.",
            showContinueButton = true,
            isContinueEnabled = true
        )

        ttsManager?.stop()
        persistirProgresoActual(_uiState.value.indiceActual)
    }

    fun activarPregunta() {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(
            lecturaActiva = false,
            lecturaPausadaPorPregunta = true,
            voiceStatus = "❓ Lectura pausada. Haz tu pregunta.",
            showContinueButton = true,
            isContinueEnabled = false
        )

        ttsManager?.stop()
        persistirProgresoActual(_uiState.value.indiceActual)

        val paginaActual = obtenerPaginaActual()
        agregarMensaje(
            remitente = "Sistema",
            texto = "La lectura está pausada en la página $paginaActual. Puedes hacer tu pregunta."
        )
    }

    fun continuarLectura() {
        _uiState.value = _uiState.value.copy(
            lecturaActiva = true,
            lecturaPausadaPorPregunta = false,
            showContinueButton = false,
            voiceStatus = "▶ Continuando lectura..."
        )

        leerFragmentoActual()
    }

    fun saltarAFragmento(indice: Int) {
        val state = _uiState.value
        if (indice in state.fragmentosLectura.indices) {
            val fragmento = state.fragmentosLectura[indice]
            val snippet = fragmento.texto.take(80)
            val historyItem = com.example.model.PositionHistoryItem(
                fragmentIndex = indice,
                pageNumber = fragmento.pagina,
                snippet = snippet
            )
            val updatedHistory = (listOf(historyItem) + state.positionHistory).distinctBy { it.fragmentIndex }.take(30)

            _uiState.value = _uiState.value.copy(
                indiceActual = indice,
                positionHistory = updatedHistory
            )
            persistirProgresoActual(indice)
            if (state.lecturaActiva) {
                leerFragmentoActual()
            } else {
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Posición: Página ${fragmento.pagina} (Fragmento ${indice + 1}/${state.fragmentosLectura.size})"
                )
            }
        }
    }

    fun limpiarHistorialPosiciones() {
        _uiState.value = _uiState.value.copy(positionHistory = emptyList())
    }

    // --- FILTRADO Y ORGANIZACIÓN DE BIBLIOTECA ---
    fun setCollectionFilter(collection: String) {
        _uiState.value = _uiState.value.copy(selectedCollection = collection)
    }

    fun setLibrarySortOrder(order: LibrarySortOrder) {
        _uiState.value = _uiState.value.copy(librarySortOrder = order)
    }

    fun setTagFilter(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTagFilter = tag)
    }

    fun setLibrarySearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(librarySearchQuery = query)
    }

    fun actualizarMetadatosDocumento(
        id: String,
        title: String,
        author: String,
        materia: String,
        year: String,
        description: String,
        collection: String,
        tags: List<String>
    ) {
        viewModelScope.launch {
            repository.updateMetadata(id, title, author, materia, year, description, collection, tags)
            if (_uiState.value.currentDocumentId == id) {
                _uiState.value = _uiState.value.copy(
                    documentTitle = title.trim(),
                    documentAuthor = author.trim(),
                    documentMateria = materia.trim(),
                    documentYear = year.trim(),
                    documentDescription = description.trim(),
                    documentCollection = collection.trim(),
                    documentTags = tags
                )
            }
        }
    }

    fun guardarNotasGenerales(id: String, notes: String) {
        viewModelScope.launch {
            repository.updateGeneralNotes(id, notes)
            if (_uiState.value.currentDocumentId == id) {
                _uiState.value = _uiState.value.copy(documentGeneralNotes = notes)
            }
        }
    }

    fun guardarNotasGeneralesDocumento(notes: String) {
        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            guardarNotasGenerales(docId, notes)
        }
    }

    fun editarOpinion(opinionId: String, nuevoTexto: String, nuevaEtiqueta: String) {
        val currentList = _uiState.value.opinions.toMutableList()
        val index = currentList.indexOfFirst { it.id == opinionId }
        if (index >= 0) {
            val old = currentList[index]
            currentList[index] = old.copy(opinion = nuevoTexto.trim(), tag = nuevaEtiqueta.trim(), timestamp = System.currentTimeMillis())
            _uiState.value = _uiState.value.copy(opinions = currentList)
            val docId = _uiState.value.currentDocumentId
            if (docId.isNotBlank()) {
                viewModelScope.launch {
                    repository.updateOpinions(docId, currentList)
                }
            }
        }
    }

    fun compartirDocumento(context: android.content.Context, doc: RecentDocumentEntity) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, doc.title)
                val body = buildString {
                    append("Documento Jurídico: ${doc.title}\n")
                    if (doc.author.isNotBlank()) append("Autor: ${doc.author}\n")
                    if (doc.materia.isNotBlank()) append("Materia: ${doc.materia}\n")
                    if (doc.year.isNotBlank()) append("Año: ${doc.year}\n")
                    if (doc.description.isNotBlank()) append("Descripción: ${doc.description}\n\n")
                    val samplePages = repository.parsePages(doc.pagesJson)
                    if (samplePages.isNotEmpty()) {
                        append("Fragmento inicial:\n")
                        append(samplePages.first().texto.take(300))
                        append("...\n\n")
                    }
                    append("Compartido desde JurisTech Reader.")
                }
                putExtra(android.content.Intent.EXTRA_TEXT, body)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Compartir documento"))
        } catch (_: Exception) {}
    }

    fun compartirTexto(context: android.content.Context, titulo: String, texto: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, titulo)
                putExtra(android.content.Intent.EXTRA_TEXT, "$titulo\n\n\"$texto\"\n\n— JurisTech Legal Reader")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Compartir texto"))
        } catch (_: Exception) {}
    }

    // --- AJUSTES DE LECTURA Y ESTILO ---
    fun setActiveHighlightColor(colorHex: String) {
        _uiState.value = _uiState.value.copy(activeHighlightColorHex = colorHex)
        prefs.edit().putString("highlight_color", colorHex).apply()
    }

    fun setIsUnderlineMode(isUnderline: Boolean) {
        _uiState.value = _uiState.value.copy(isUnderlineMode = isUnderline)
        prefs.edit().putBoolean("is_underline_mode", isUnderline).apply()
    }

    fun setLineSpacing(spacing: Float) {
        _uiState.value = _uiState.value.copy(lineSpacing = spacing)
        prefs.edit().putFloat("line_spacing", spacing).apply()
    }

    fun setTtsVoice(voiceName: String) {
        _uiState.value = _uiState.value.copy(activeTtsVoice = voiceName)
        prefs.edit().putString("tts_voice", voiceName).apply()
        ttsManager?.setVoiceByName(voiceName)
    }

    fun recargarVocesDisponibles() {
        val voices = ttsManager?.getAvailableVoices() ?: emptyList()
        _uiState.value = _uiState.value.copy(availableTtsVoices = voices)
    }

    // --- HERRAMIENTAS DE ESTUDIO E IA AVANZADA ---
    fun generarHerramientaEstudio(tipo: String) {
        val state = _uiState.value
        _uiState.value = state.copy(isGeneratingStudyTool = true, studyToolType = tipo)

        viewModelScope.launch {
            when (tipo) {
                "SUMMARY" -> {
                    val res = aiService.generarResumen(state.documentTitle, state.paginas)
                    _uiState.value = _uiState.value.copy(studySummary = res, isGeneratingStudyTool = false)
                }
                "OUTLINE" -> {
                    val outline = aiService.generarEsquema(state.documentTitle, state.paginas)
                    _uiState.value = _uiState.value.copy(studyOutline = outline, isGeneratingStudyTool = false)
                }
                "EXAM" -> {
                    val exam = aiService.generarPreguntasExamen(state.documentTitle, state.paginas)
                    _uiState.value = _uiState.value.copy(
                        studyExamQuestions = exam,
                        examUserAnswers = emptyMap(),
                        examScore = null,
                        isGeneratingStudyTool = false
                    )
                }
                "FLASHCARDS" -> {
                    val cards = aiService.generarFlashcards(state.documentTitle, state.paginas)
                    _uiState.value = _uiState.value.copy(
                        studyFlashcards = cards,
                        flashcardCurrentIndex = 0,
                        flashcardFlipped = false,
                        isGeneratingStudyTool = false
                    )
                }
                "CONCEPTS" -> {
                    val concepts = aiService.extraerConceptosClave(state.paginas)
                    _uiState.value = _uiState.value.copy(studyConcepts = concepts, isGeneratingStudyTool = false)
                }
                "PODCAST" -> {
                    val podcast = aiService.generarPodcastResumen(state.documentTitle, state.paginas)
                    _uiState.value = _uiState.value.copy(podcastText = podcast, isGeneratingStudyTool = false)
                }
                "REPORT" -> {
                    val report = aiService.crearInformeJuridico(state.documentTitle, state.paginas, "Informe de aplicabilidad jurídica")
                    _uiState.value = _uiState.value.copy(legalReportText = report, isGeneratingStudyTool = false)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isGeneratingStudyTool = false)
                }
            }
        }
    }

    fun responderPreguntaExamen(questionId: String, selectedIndex: Int) {
        val answers = _uiState.value.examUserAnswers.toMutableMap()
        answers[questionId] = selectedIndex

        val total = _uiState.value.studyExamQuestions.size
        val corrects = _uiState.value.studyExamQuestions.count { q ->
            answers[q.id] == q.indiceCorrecto
        }

        val score = if (answers.size == total) Pair(corrects, total) else null
        _uiState.value = _uiState.value.copy(
            examUserAnswers = answers,
            examScore = score
        )
    }

    fun reiniciarExamen() {
        _uiState.value = _uiState.value.copy(
            examUserAnswers = emptyMap(),
            examScore = null
        )
    }

    fun siguienteFlashcard() {
        val total = _uiState.value.studyFlashcards.size
        if (total == 0) return
        val next = (_uiState.value.flashcardCurrentIndex + 1) % total
        _uiState.value = _uiState.value.copy(
            flashcardCurrentIndex = next,
            flashcardFlipped = false
        )
    }

    fun anteriorFlashcard() {
        val total = _uiState.value.studyFlashcards.size
        if (total == 0) return
        val prev = if (_uiState.value.flashcardCurrentIndex <= 0) total - 1 else _uiState.value.flashcardCurrentIndex - 1
        _uiState.value = _uiState.value.copy(
            flashcardCurrentIndex = prev,
            flashcardFlipped = false
        )
    }

    fun voltearFlashcard() {
        _uiState.value = _uiState.value.copy(
            flashcardFlipped = !_uiState.value.flashcardFlipped
        )
    }

    fun reproducirResumenPodcast() {
        val text = _uiState.value.podcastText
        if (text.isNotBlank()) {
            _uiState.value = _uiState.value.copy(isPodcastPlaying = true)
            ttsManager?.speakDirect(text) {
                _uiState.value = _uiState.value.copy(isPodcastPlaying = false)
            }
        }
    }

    fun detenerResumenPodcast() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(isPodcastPlaying = false)
    }

    fun compararDocumentos(doc2Id: String, tema: String) {
        val doc1Title = _uiState.value.documentTitle
        val doc1Pages = _uiState.value.paginas
        val doc2 = _uiState.value.recentDocuments.firstOrNull { it.id == doc2Id } ?: return
        val doc2Pages = repository.parsePages(doc2.pagesJson)

        _uiState.value = _uiState.value.copy(isComparingDocs = true)
        viewModelScope.launch {
            val result = aiService.compararDocumentos(doc1Title, doc1Pages, doc2.title, doc2Pages, tema)
            _uiState.value = _uiState.value.copy(comparisonResult = result, isComparingDocs = false)
        }
    }

    fun fragmentoAnterior() {
        val nuevoIndice = _uiState.value.indiceActual - 1
        if (nuevoIndice >= 0) {
            saltarAFragmento(nuevoIndice)
        }
    }

    fun fragmentoSiguiente() {
        val nuevoIndice = _uiState.value.indiceActual + 1
        if (nuevoIndice < _uiState.value.fragmentosLectura.size) {
            saltarAFragmento(nuevoIndice)
        }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
        prefs.edit().putFloat("speech_rate", rate).apply()
        ttsManager?.setSpeechRate(rate)
        if (_uiState.value.lecturaActiva) {
            leerFragmentoActual()
        }
    }

    fun enviarPregunta(pregunta: String) {
        val preguntaLimpia = pregunta.trim()
        if (preguntaLimpia.isBlank()) return

        if (_uiState.value.lecturaActiva) {
            activarPregunta()
        }

        agregarMensaje(remitente = "Tú", texto = preguntaLimpia, isUser = true)
        val state = _uiState.value
        val paginaActual = obtenerPaginaActual()
        val documentTitle = state.documentTitle
        val paginas = state.paginas
        val historialConversacion = state.chatMessages

        _uiState.value = _uiState.value.copy(isAskingAi = true)

        viewModelScope.launch {
            val respuesta = aiService.responderPreguntaConHistorial(
                pregunta = preguntaLimpia,
                documentTitle = documentTitle,
                paginas = paginas,
                paginaActual = paginaActual,
                historialConversacion = historialConversacion
            )

            val regexPage = Regex("Página (\\d+)")
            val matchedPages = mutableListOf<Int>()
            regexPage.findAll(respuesta).forEach { match ->
                match.groupValues[1].toIntOrNull()?.let { matchedPages.add(it) }
            }
            val matchedFrag = if (matchedPages.isNotEmpty()) {
                val p = matchedPages.first()
                state.fragmentosLectura.indexOfFirst { it.pagina == p }
            } else -1

            val assistantMsg = ChatMessage(
                remitente = "JurisTech AI",
                texto = respuesta,
                isUser = false,
                referencedPages = matchedPages,
                referencedFragmentIndex = matchedFrag
            )

            val updatedMessages = _uiState.value.chatMessages + assistantMsg
            _uiState.value = _uiState.value.copy(
                chatMessages = updatedMessages,
                isAskingAi = false,
                showContinueButton = true,
                isContinueEnabled = true,
                voiceStatus = "Pregunta respondida. Puedes hacer preguntas de seguimiento o pulsar CONTINUAR."
            )

            val docId = _uiState.value.currentDocumentId
            if (docId.isNotBlank()) {
                repository.updateQuestionsHistory(docId, updatedMessages)
            }
        }
    }

    fun limpiarChat() {
        val state = _uiState.value
        _uiState.value = state.copy(
            chatMessages = listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Historial reiniciado. He analizado el documento '${state.documentTitle}' (${state.paginas.size} páginas). Puedes hacerme preguntas globales o sobre lo que estás leyendo.",
                    isUser = false
                )
            )
        )
    }

    private fun obtenerContextoActual(): String {
        val state = _uiState.value
        if (state.fragmentosLectura.isEmpty()) {
            return state.paginas.joinToString("\n\n") { "Página ${it.numero}:\n${it.texto}" }
        }

        val inicio = maxOf(0, state.indiceActual - 3)
        val fin = minOf(state.fragmentosLectura.size, state.indiceActual + 4)

        return state.fragmentosLectura.subList(inicio, fin).joinToString("\n\n") {
            "Página ${it.pagina}:\n${it.texto}"
        }
    }

    fun obtenerPaginaActual(): Int {
        val state = _uiState.value
        return state.fragmentosLectura.getOrNull(state.indiceActual)?.pagina ?: 1
    }

    private fun agregarMensaje(remitente: String, texto: String, isUser: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + ChatMessage(
                remitente = remitente,
                texto = texto,
                isUser = isUser
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
    }
}

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
    val positionHistory: List<PositionHistoryItem> = emptyList(),
    val libraryFeedbackMessage: String? = null,
    val selectedVoiceGender: com.example.tts.VoiceGender = com.example.tts.VoiceGender.MUJER,
    val isTtsPreviewPlaying: Boolean = false,
    val activeTtsVoice: String = "Voz de Mujer",
    val availableTtsVoices: List<String> = listOf("Voz de Hombre", "Voz de Mujer"),
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
    val legalReportText: String = "",
    val learningMode: ReaderLearningMode = ReaderLearningMode.FREE_READING,
    val guidedBlockSize: Int = 3,
    val fragmentsInCurrentBlock: Int = 0,
    val activeGuidedQuestion: GuidedQuestion? = null,
    val showGuidedQuestionDialog: Boolean = false,
    val guidedAnswerInput: String = "",
    val isEvaluatingAnswer: Boolean = false,
    val guidedEvaluationResult: EvaluationResult? = null,
    val isVoiceListening: Boolean = false,
    val voiceListeningStatus: String = "",
    val dueCards: List<com.example.data.local.learning.SpacedRepetitionEntity> = emptyList(),
    val currentReviewCardIndex: Int = 0,
    val showReviewAnswer: Boolean = false,
    val dueCardsCount: Int = 0,
    val cornellNotes: List<CornellNoteItem> = emptyList(),
    val activeCornellNote: CornellNoteItem? = null,
    val isGeneratingCornell: Boolean = false,
    val dualCodingMap: DualCodingConceptMap? = null,
    val isGeneratingDualCoding: Boolean = false,
    val studyStats: StudySessionStats = StudySessionStats(),
    val isPomodoroActive: Boolean = false,
    val pomodoroRemainingSeconds: Int = 25 * 60,
    val pomodoroIsBreak: Boolean = false,
    val showStudyCoachDialog: Boolean = false,
    val studyCoachSelectedTab: Int = 0,
    val socraticTutorLevel: String = "Sencillo",
    val apiKeyConfigured: Boolean = com.example.ai.GeminiApiClient.hasValidApiKey(),
    val ttsMode: com.example.tts.TtsMode = com.example.tts.TtsMode.AUTO,
    val activeTtsEngineType: com.example.tts.ActiveEngineType = com.example.tts.ActiveEngineType.LOCAL,
    val isNetworkAvailable: Boolean = true,
    val offlineVoiceInstructionsNeeded: Boolean = false,
    val offlineInstructionsText: String = "",
    val readerViewMode: ReaderViewMode = ReaderViewMode.STRUCTURED,
    val structuredBlocks: List<StructuredBlock> = emptyList(),
    val pageStructures: List<PdfPageStructure> = emptyList(),
    val structuredOutline: List<PdfOutlineNode> = emptyList(),
    val currentOriginalPage: Int = 1,
    val originalPageBitmap: android.graphics.Bitmap? = null,
    val readingAnnouncements: ReadingAnnouncementsConfig = ReadingAnnouncementsConfig(),
    val isSemanticSearchMode: Boolean = false,
    val semanticSearchResults: List<SemanticSearchResult> = emptyList(),
    val isSemanticSearching: Boolean = false,
    val selectedOutlineNodeId: String? = null,
    val isChatVoiceListening: Boolean = false,
    val chatVoiceStatus: String = "",
    val autoReadChatResponses: Boolean = false,
    val isReadingChatResponse: Boolean = false,
    val isReadingChatPaused: Boolean = false,
    val playingChatMessageIndex: Int? = null,
    val chatProcessState: ChatProcessState = ChatProcessState.READY,
    val chatSpeechRate: Float = 1.0f,
    val activeMultimodalLesson: MultimodalLesson? = null,
    val isGeneratingMultimodalLesson: Boolean = false,
    val showMultimodalLessonScreen: Boolean = false,
    val isLessonSpeechPlaying: Boolean = false,
    val currentSpokenHighlightIndex: Int = -1,
    val lessonSpeechRate: Float = 1.0f
)

class JurisTechViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(JurisTechUiState())
    val uiState: StateFlow<JurisTechUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application)
    private val highlightDao = database.highlightDao()
    private val repository = RecentDocumentRepository(database.recentDocumentDao(), highlightDao, application)
    val studyRepository = com.example.data.local.learning.StudyRepository(database.studyDao())

    private val aiService = JurisTechAiService()
    val learningEngine = com.example.ai.JurisTechLearningEngine(aiService)
    val pdfPageRenderer = com.example.util.PdfPageRenderer(application)
    private var ttsManager: TtsManager? = null
    private val audioRecordManager = com.example.util.AudioRecordManager(application)
    private var speechHelper: com.example.util.SpeechRecognitionHelper? = null
    private var pomodoroJob: kotlinx.coroutines.Job? = null
    private var lessonSpeechJob: kotlinx.coroutines.Job? = null
    private val prefs = application.getSharedPreferences("juristech_prefs", android.content.Context.MODE_PRIVATE)

    init {
        val savedSpeechRate = prefs.getFloat("speech_rate", 1.0f)
        val savedFontSizeScale = prefs.getFloat("font_size_scale", 1.0f)
        val savedLineSpacing = prefs.getFloat("line_spacing", 1.5f)
        val savedVoiceGenderId = prefs.getString("tts_voice_gender", com.example.tts.VoiceGender.MUJER.id) ?: com.example.tts.VoiceGender.MUJER.id
        val initialVoiceGender = com.example.tts.VoiceGender.fromId(savedVoiceGenderId)
        val savedVoice = prefs.getString("tts_voice", initialVoiceGender.displayName) ?: initialVoiceGender.displayName
        val savedUnderline = prefs.getBoolean("is_underline_mode", false)
        val savedHighlightColor = prefs.getString("highlight_color", "#FFF59D") ?: "#FFF59D"
        _uiState.value = _uiState.value.copy(
            speechRate = savedSpeechRate,
            fontSizeScale = savedFontSizeScale,
            lineSpacing = savedLineSpacing,
            selectedVoiceGender = initialVoiceGender,
            activeTtsVoice = initialVoiceGender.displayName,
            isUnderlineMode = savedUnderline,
            activeHighlightColorHex = savedHighlightColor
        )

        ttsManager = TtsManager(
            context = application,
            onFragmentStarted = { fragmentId ->
                if (fragmentId == -999) {
                    _uiState.value = _uiState.value.copy(
                        isReadingChatResponse = true,
                        isReadingChatPaused = false,
                        chatProcessState = ChatProcessState.PLAYING_AUDIO
                    )
                } else {
                    val state = _uiState.value
                    val frag = state.fragmentosLectura.getOrNull(state.indiceActual)
                    if (frag != null && frag.id == fragmentId) {
                        _uiState.value = _uiState.value.copy(
                            voiceStatus = "🔊 Leyendo página ${frag.pagina}"
                        )
                    }
                }
            },
            onFragmentCompleted = { fragmentId ->
                if (fragmentId == -888) {
                    _uiState.value = _uiState.value.copy(
                        isLessonSpeechPlaying = false,
                        currentSpokenHighlightIndex = -1
                    )
                } else if (fragmentId == -999) {
                    _uiState.value = _uiState.value.copy(
                        isReadingChatResponse = false,
                        isReadingChatPaused = false,
                        playingChatMessageIndex = null,
                        chatProcessState = ChatProcessState.READY
                    )
                } else {
                    val state = _uiState.value
                    if (state.lecturaActiva) {
                        val nextIndex = state.indiceActual + 1
                        val newBlockCount = state.fragmentsInCurrentBlock + 1
                        if (state.learningMode == ReaderLearningMode.GUIDED_LEARNING && newBlockCount >= state.guidedBlockSize) {
                            _uiState.value = _uiState.value.copy(
                                indiceActual = if (nextIndex < state.fragmentosLectura.size) nextIndex else state.indiceActual,
                                lecturaActiva = false,
                                fragmentsInCurrentBlock = 0,
                                voiceStatus = "🧠 Bloque completado. Momento de evocación activa."
                            )
                            persistirProgresoActual(state.indiceActual)
                            activarPreguntaGuiada()
                        } else if (nextIndex < state.fragmentosLectura.size) {
                            _uiState.value = _uiState.value.copy(
                                indiceActual = nextIndex,
                                fragmentsInCurrentBlock = newBlockCount
                            )
                            persistirProgresoActual(nextIndex)
                            leerFragmentoActual()
                        } else {
                            val lastFrag = state.fragmentosLectura.lastOrNull()
                            val lastPageInWindow = lastFrag?.pagina ?: 1
                            if (state.paginas.size > 80 && lastPageInWindow < state.paginas.size) {
                                avanzarVentanaLectura(lastPageInWindow + 1)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    lecturaActiva = false,
                                    voiceStatus = "Lectura finalizada.",
                                    showContinueButton = false,
                                    fragmentsInCurrentBlock = 0
                                )
                                persistirProgresoActual(state.indiceActual)
                            }
                        }
                    }
                }
            },
            onErrorOccurred = { _, errorMsg ->
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Error de voz: $errorMsg"
                )
            },
            onEngineSwitched = { activeEngine, reason ->
                _uiState.value = _uiState.value.copy(
                    activeTtsEngineType = activeEngine,
                    isNetworkAvailable = ttsManager?.isNetworkConnected ?: true,
                    voiceStatus = reason
                )
            }
        )
        ttsManager?.setSpeechRate(savedSpeechRate)
        ttsManager?.setVoiceGender(initialVoiceGender)

        val hasSpanish = ttsManager?.hasSpanishLocalVoicesInstalled() ?: true
        if (!hasSpanish) {
            _uiState.value = _uiState.value.copy(
                offlineVoiceInstructionsNeeded = true,
                offlineInstructionsText = ttsManager?.getOfflineSetupInstructions() ?: ""
            )
        }
        _uiState.value = _uiState.value.copy(
            availableTtsVoices = ttsManager?.getAvailableVoices() ?: emptyList(),
            activeTtsEngineType = ttsManager?.activeEngineType ?: com.example.tts.ActiveEngineType.LOCAL,
            isNetworkAvailable = ttsManager?.isNetworkConnected ?: true
        )

        // Inicializar reconocimiento de voz para respuestas orales
        speechHelper = com.example.util.SpeechRecognitionHelper(
            context = application,
            onResultText = { recognizedText ->
                _uiState.value = _uiState.value.copy(
                    guidedAnswerInput = recognizedText,
                    isVoiceListening = false,
                    voiceListeningStatus = "Voz capturada correctamente."
                )
            },
            onListeningStateChanged = { listening, status ->
                _uiState.value = _uiState.value.copy(
                    isVoiceListening = listening,
                    voiceListeningStatus = status
                )
            }
        )

        // Observar repetición espaciada en Room
        viewModelScope.launch {
            studyRepository.getDueCardsFlow().collect { cards ->
                _uiState.value = _uiState.value.copy(
                    dueCards = cards,
                    dueCardsCount = cards.size
                )
            }
        }

        // Cargar estadísticas iniciales
        viewModelScope.launch {
            actualizarEstadisticasEstudio()
        }

        // Cargar voces disponibles
        recargarVocesDisponibles()

        // Observar la lista reactiva de documentos recientes almacenados en Room
        viewModelScope.launch {
            repository.recentDocuments.collect { recents ->
                _uiState.value = _uiState.value.copy(recentDocuments = recents)

                val hasCodigoPenal = recents.any { it.title.contains("Código Penal", ignoreCase = true) }
                if (recents.isEmpty() || !hasCodigoPenal) {
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

    fun clearLibraryFeedbackMessage() {
        _uiState.value = _uiState.value.copy(libraryFeedbackMessage = null)
    }

    fun sembrarCodigosHonduras() {
        agregarTodosLosLibrosDelCatalogo()
    }

    fun restaurarCodigosHonduras() {
        agregarTodosLosLibrosDelCatalogo()
    }

    fun agregarLibroALaBiblioteca(sample: SampleDocument, abrirAhora: Boolean = false) {
        if (abrirAhora) {
            cargarSample(sample)
            return
        }
        viewModelScope.launch {
            val sampleId = "sample_" + sample.title.hashCode().toString()
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
                coverColor = sample.coverColor,
                author = sample.author,
                materia = sample.materia,
                year = sample.year
            )
            _uiState.value = _uiState.value.copy(
                libraryFeedbackMessage = "✓ '${sample.title}' se añadió a tu biblioteca"
            )
        }
    }

    fun agregarTodosLosLibrosDelCatalogo() {
        viewModelScope.launch {
            val currentTitles = _uiState.value.recentDocuments.map { it.title.trim().lowercase() }.toSet()
            var addedCount = 0
            SampleDocumentRepository.hondurasSamples.forEach { sample ->
                if (!currentTitles.contains(sample.title.trim().lowercase())) {
                    val sampleId = "sample_" + sample.title.hashCode().toString()
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
                        coverColor = sample.coverColor,
                        author = sample.author,
                        materia = sample.materia,
                        year = sample.year
                    )
                    addedCount++
                }
            }
            _uiState.value = _uiState.value.copy(
                libraryFeedbackMessage = if (addedCount > 0) "✓ Se añadieron $addedCount libros a tu biblioteca" else "Todos los libros del catálogo ya están en tu biblioteca"
            )
        }
    }

    fun crearNuevoLibroPersonalizado(
        titulo: String,
        autor: String = "Autor Desconocido",
        materia: String = "Derecho",
        contenido: String,
        abrirAhora: Boolean = false
    ) {
        if (contenido.isBlank() || titulo.isBlank()) return

        val lineas = contenido.split("\n\n").filter { it.isNotBlank() }
        val paginasCreadas = mutableListOf<DocumentPage>()
        var bufferTexto = StringBuilder()
        var numeroPagina = 1

        for (linea in lineas) {
            bufferTexto.append(linea).append("\n\n")
            if (bufferTexto.split(Regex("\\s+")).size >= 160) {
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
            paginasCreadas.add(DocumentPage(numero = 1, texto = contenido.trim()))
        }

        val generatedDocId = "doc_" + System.currentTimeMillis()

        if (abrirAhora) {
            cargarDocumentoCompleto(
                id = generatedDocId,
                titulo = titulo.trim(),
                subtitulo = "${paginasCreadas.size} páginas • $materia",
                paginas = paginasCreadas,
                sourceType = "IMPORTED",
                author = autor.trim(),
                materia = materia.trim(),
                year = "2024"
            )
        } else {
            viewModelScope.launch {
                val fragments = SampleDocumentRepository.crearFragmentos(paginasCreadas)
                repository.saveOrUpdateDocument(
                    id = generatedDocId,
                    title = titulo.trim(),
                    subtitle = "${paginasCreadas.size} páginas • $materia",
                    pages = paginasCreadas,
                    totalFragments = fragments.size,
                    lastPage = 1,
                    lastFragmentIndex = 0,
                    sourceType = "IMPORTED",
                    author = autor.trim(),
                    materia = materia.trim(),
                    year = "2024"
                )
                _uiState.value = _uiState.value.copy(
                    libraryFeedbackMessage = "✓ Libro '$titulo' añadido a tu biblioteca"
                )
            }
        }
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
            coverColor = sample.coverColor,
            author = sample.author,
            materia = sample.materia,
            year = sample.year
        )
    }

    fun cargarDocumentoPersonalizado(
        titulo: String,
        paginas: List<DocumentPage>,
        subtitulo: String = "Documento importado",
        customCoverUri: String? = null,
        coverColor: Long? = null,
        id: String = "doc_" + System.currentTimeMillis()
    ) {
        cargarDocumentoCompleto(
            id = id,
            titulo = titulo,
            subtitulo = subtitulo,
            paginas = paginas,
            sourceType = "IMPORTED",
            customCoverUri = customCoverUri,
            coverColor = coverColor
        )
    }

    private fun getPagesWindow(allPages: List<DocumentPage>, targetPage: Int, windowRadius: Int = 20): List<DocumentPage> {
        if (allPages.size <= 80) return allPages
        val startPage = (targetPage - windowRadius).coerceAtLeast(1)
        val endPage = (targetPage + windowRadius).coerceAtMost(allPages.size)
        return allPages.filter { it.numero in startPage..endPage }
    }

    private fun actualizarVentanaLectura(targetPage: Int) {
        val state = _uiState.value
        val allPages = state.paginas
        if (allPages.size <= 80) return
        val docId = state.currentDocumentId
        viewModelScope.launch {
            val fromDisk = repository.loadPageWindow(docId, targetPage, windowRadius = 25)
            val windowPages = if (fromDisk.isNotEmpty()) fromDisk else getPagesWindow(allPages, targetPage, windowRadius = 25)
            val fragmentos = SampleDocumentRepository.crearFragmentos(windowPages)
            val (structures, blocks) = com.example.util.PdfStructureParser.parseDocument(docId, windowPages)
            _uiState.value = _uiState.value.copy(
                fragmentosLectura = fragmentos,
                structuredBlocks = blocks,
                pageStructures = structures
            )
        }
    }

    private fun avanzarVentanaLectura(startPage: Int) {
        val state = _uiState.value
        val docId = state.currentDocumentId
        viewModelScope.launch {
            val fromDisk = repository.loadPageWindow(docId, startPage, windowRadius = 25)
            val windowPages = if (fromDisk.isNotEmpty()) fromDisk else getPagesWindow(state.paginas, startPage, windowRadius = 25)
            if (windowPages.isNotEmpty()) {
                val fragmentos = SampleDocumentRepository.crearFragmentos(windowPages)
                val (structures, blocks) = com.example.util.PdfStructureParser.parseDocument(docId, windowPages)
                _uiState.value = _uiState.value.copy(
                    fragmentosLectura = fragmentos,
                    structuredBlocks = blocks,
                    pageStructures = structures,
                    indiceActual = 0,
                    currentOriginalPage = startPage
                )
                renderizarPaginaVisual(startPage)
                leerFragmentoActual()
            }
        }
    }

    private fun cargarDocumentoCompleto(
        id: String,
        titulo: String,
        subtitulo: String,
        paginas: List<DocumentPage>,
        sourceType: String,
        customCoverUri: String? = null,
        coverColor: Long? = null,
        author: String = "",
        materia: String = "",
        year: String = "",
        collection: String = ""
    ) {
        ttsManager?.stop()

        val isLargeDoc = paginas.size > 80
        val windowPages = if (isLargeDoc) getPagesWindow(paginas, targetPage = 1, windowRadius = 25) else paginas
        val fragmentos = SampleDocumentRepository.crearFragmentos(windowPages)
        val (structures, blocks) = com.example.util.PdfStructureParser.parseDocument(id, windowPages)
        val diskOutline = com.example.util.DocumentPageStorage.readOutline(getApplication(), id)
        val outline = if (diskOutline.isNotEmpty()) {
            diskOutline
        } else if (isLargeDoc) {
            com.example.util.PdfStructureParser.buildOutlineFromPages(windowPages)
        } else {
            com.example.util.PdfStructureParser.buildOutlineTree(blocks)
        }

        _uiState.value = _uiState.value.copy(
            currentDocumentId = id,
            documentTitle = titulo,
            paginas = paginas,
            fragmentosLectura = fragmentos,
            structuredBlocks = blocks,
            pageStructures = structures,
            structuredOutline = outline,
            currentOriginalPage = 1,
            indiceActual = 0,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            customCoverUri = customCoverUri,
            coverColor = coverColor,
            documentAuthor = author,
            documentMateria = materia,
            documentYear = year,
            documentCollection = collection,
            highlights = emptySet(),
            opinions = emptyList(),
            bookmarks = emptyList(),
            voiceStatus = "Documento listo para leer.",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Documento '$titulo' cargado (${paginas.size} páginas, ${fragmentos.size} fragmentos, ${blocks.size} bloques estructurados). Puedes comenzar la lectura con '▶ Leer' o hacer preguntas en cualquier momento."
                )
            )
        )

        viewModelScope.launch {
            val pdfFile = com.example.util.DocumentPageStorage.getDocumentPdfFile(getApplication(), id)
            if (pdfFile.exists() && pdfFile.length() > 0L) {
                pdfPageRenderer.openFile(pdfFile)
            } else {
                pdfPageRenderer.close()
            }
            renderizarPaginaVisual(1)
        }

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
                coverColor = coverColor,
                author = author,
                materia = materia,
                year = year,
                collection = collection
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

        var paginas = repository.parsePages(entity.pagesJson, entity.id)
        if (paginas.isEmpty()) {
            val sampleMatch = SampleDocumentRepository.samples.firstOrNull { it.title == entity.title }
            if (sampleMatch != null) {
                paginas = sampleMatch.pages
            }
        }

        if (paginas.isEmpty()) return

        val isLargeDoc = paginas.size > 80 || entity.totalPages > 80
        val paginaRestaurada = entity.lastPage.coerceIn(1, maxOf(1, paginas.size))

        viewModelScope.launch {
            val windowPages = if (isLargeDoc) {
                val fromDisk = repository.loadPageWindow(entity.id, paginaRestaurada, 25)
                if (fromDisk.isNotEmpty()) fromDisk else getPagesWindow(paginas, targetPage = paginaRestaurada, windowRadius = 25)
            } else paginas

            val fragmentos = SampleDocumentRepository.crearFragmentos(windowPages)
            val indiceRestaurado = entity.lastFragmentIndex.coerceIn(0, maxOf(0, fragmentos.size - 1))

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
                        texto = "Has retomado '${entity.title}'. Posición actual: Página $paginaRestaurada de ${paginas.size}.",
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

            val (structures, blocks) = com.example.util.PdfStructureParser.parseDocument(entity.id, windowPages)
            val diskOutline = com.example.util.DocumentPageStorage.readOutline(getApplication(), entity.id)
            val outline = if (diskOutline.isNotEmpty()) {
                diskOutline
            } else if (isLargeDoc) {
                com.example.util.PdfStructureParser.buildOutlineFromPages(windowPages)
            } else {
                com.example.util.PdfStructureParser.buildOutlineTree(blocks)
            }

            _uiState.value = _uiState.value.copy(
                currentDocumentId = entity.id,
                documentTitle = entity.title,
                paginas = paginas,
                fragmentosLectura = fragmentos,
                structuredBlocks = blocks,
                pageStructures = structures,
                structuredOutline = outline,
                currentOriginalPage = paginaRestaurada,
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

            val pdfFile = com.example.util.DocumentPageStorage.getDocumentPdfFile(getApplication(), entity.id)
            if (pdfFile.exists() && pdfFile.length() > 0L) {
                pdfPageRenderer.openFile(pdfFile)
            } else {
                pdfPageRenderer.close()
            }
            renderizarPaginaVisual(paginaRestaurada)
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
        val isAdding = !currentHighlights.contains(fragmentId)
        if (isAdding) {
            currentHighlights.add(fragmentId)
        } else {
            currentHighlights.remove(fragmentId)
        }
        _uiState.value = _uiState.value.copy(highlights = currentHighlights)

        val docId = _uiState.value.currentDocumentId
        if (docId.isNotBlank()) {
            val frag = _uiState.value.fragmentosLectura.firstOrNull { it.id == fragmentId }
            viewModelScope.launch {
                repository.updateHighlights(docId, currentHighlights)
                if (isAdding && frag != null) {
                    val highlightEntity = com.example.data.local.Highlight(
                        documentId = docId,
                        pageNumber = frag.pagina,
                        fragmentId = frag.id,
                        selectedText = frag.texto,
                        startOffset = 0,
                        endOffset = frag.texto.length,
                        color = _uiState.value.activeHighlightColorHex,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.saveHighlight(highlightEntity)
                } else {
                    repository.removeHighlightsByFragment(docId, fragmentId)
                }
            }
        }
    }

    fun addCustomHighlight(
        pageNumber: Int,
        fragmentId: Int,
        selectedText: String,
        startOffset: Int = 0,
        endOffset: Int = selectedText.length,
        color: String = _uiState.value.activeHighlightColorHex
    ) {
        val docId = _uiState.value.currentDocumentId
        if (docId.isBlank() || selectedText.isBlank()) return
        viewModelScope.launch {
            val highlight = com.example.data.local.Highlight(
                documentId = docId,
                pageNumber = pageNumber,
                fragmentId = fragmentId,
                selectedText = selectedText,
                startOffset = startOffset,
                endOffset = endOffset,
                color = color,
                timestamp = System.currentTimeMillis()
            )
            repository.saveHighlight(highlight)
        }
    }

    fun removeHighlight(highlightId: String) {
        viewModelScope.launch {
            repository.removeHighlightById(highlightId)
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
        val clampedPage = numeroPagina.coerceIn(1, maxOf(1, state.paginas.size))
        var index = state.fragmentosLectura.indexOfFirst { it.pagina == clampedPage }
        if (index < 0 && state.paginas.size > 80) {
            val docId = state.currentDocumentId
            viewModelScope.launch {
                val fromDisk = repository.loadPageWindow(docId, clampedPage, windowRadius = 25)
                val windowPages = if (fromDisk.isNotEmpty()) fromDisk else getPagesWindow(state.paginas, clampedPage, windowRadius = 25)
                val fragmentos = SampleDocumentRepository.crearFragmentos(windowPages)
                val (structures, blocks) = com.example.util.PdfStructureParser.parseDocument(docId, windowPages)
                val targetIndex = fragmentos.indexOfFirst { it.pagina == clampedPage }.coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(
                    fragmentosLectura = fragmentos,
                    structuredBlocks = blocks,
                    pageStructures = structures,
                    indiceActual = targetIndex,
                    currentOriginalPage = clampedPage
                )
                renderizarPaginaVisual(clampedPage)
                persistirProgresoActual(targetIndex)
            }
            return
        }
        if (index >= 0) {
            saltarAFragmento(index)
        }
        cambiarPaginaOriginal(clampedPage)
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
            com.example.util.DocumentPageStorage.deleteDocumentStorage(context, id)
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
        pdfPageRenderer.close()
        limpiarError()

        val generatedDocId = "doc_" + System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(
            isProcessingPdf = true,
            pdfProgressPercent = 5,
            pdfProgressText = "Abriendo y analizando archivo...",
            fileNameLabel = nombreArchivo
        )

        viewModelScope.launch {
            try {
                val result = com.example.util.DocumentExtractor.extractDocument(
                    context = getApplication(),
                    uri = uri,
                    fileName = nombreArchivo,
                    documentId = generatedDocId,
                    onProgress = { current, total, text ->
                        val percent = if (total > 0) ((current.toFloat() / total) * 100).toInt().coerceIn(5, 98) else 50
                        _uiState.value = _uiState.value.copy(
                            pdfProgressPercent = percent,
                            pdfProgressText = text
                        )
                    }
                )

                result.onSuccess { paginas ->
                    val pdfFile = com.example.util.DocumentPageStorage.getDocumentPdfFile(getApplication(), generatedDocId)
                    if (pdfFile.exists() && pdfFile.length() > 0L) {
                        pdfPageRenderer.openFile(pdfFile)
                    } else {
                        pdfPageRenderer.close()
                    }

                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfProgressPercent = 100,
                        pdfProgressText = "Documento procesado correctamente."
                    )
                    cargarDocumentoPersonalizado(
                        id = generatedDocId,
                        titulo = nombreArchivo,
                        paginas = paginas,
                        subtitulo = "${paginas.size} páginas procesadas"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingPdf = false,
                        pdfErrorMessage = error.message ?: "No se pudo abrir este archivo. Puede estar dañado o ser un PDF escaneado sin texto seleccionable.",
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
        pdfPageRenderer.close()
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
            if (fragmento.pagina != state.currentOriginalPage) {
                cambiarPaginaOriginal(fragmento.pagina)
            }
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

    fun cambiarModoVistaLector(mode: ReaderViewMode) {
        _uiState.value = _uiState.value.copy(readerViewMode = mode)
        if (mode == ReaderViewMode.ORIGINAL_PAGE || mode == ReaderViewMode.SPLIT_DUAL) {
            cambiarPaginaOriginal(_uiState.value.currentOriginalPage)
        }
    }

    fun cambiarPaginaOriginal(pageNumber: Int) {
        val total = _uiState.value.paginas.size.coerceAtLeast(1)
        val targetPage = pageNumber.coerceIn(1, total)
        _uiState.value = _uiState.value.copy(currentOriginalPage = targetPage)
        viewModelScope.launch {
            renderizarPaginaVisual(targetPage)
        }
    }

    private suspend fun renderizarPaginaVisual(pageNumber: Int) {
        val pageText = _uiState.value.paginas.firstOrNull { it.numero == pageNumber }?.texto ?: ""
        val title = _uiState.value.documentTitle
        var bitmap = pdfPageRenderer.renderPage(pageNumber)
        if (bitmap == null) {
            bitmap = pdfPageRenderer.generateBookPageBitmap(title, pageNumber, pageText)
        }
        _uiState.value = _uiState.value.copy(originalPageBitmap = bitmap)
    }

    fun repetirFragmentoActual() {
        val state = _uiState.value
        if (state.fragmentosLectura.isNotEmpty()) {
            ttsManager?.stop()
            leerFragmentoActual()
        }
    }

    fun avanzarSiguienteFragmento() {
        val state = _uiState.value
        val next = (state.indiceActual + 1).coerceAtMost(state.fragmentosLectura.size - 1)
        saltarAFragmento(next)
    }

    fun retrocederFragmentoAnterior() {
        val state = _uiState.value
        val prev = (state.indiceActual - 1).coerceAtLeast(0)
        saltarAFragmento(prev)
    }

    fun setTtsMode(mode: com.example.tts.TtsMode) {
        ttsManager?.setTtsMode(mode, continuePlayingCurrentFragment = _uiState.value.lecturaActiva)
        _uiState.value = _uiState.value.copy(
            ttsMode = mode,
            activeTtsEngineType = ttsManager?.activeEngineType ?: com.example.tts.ActiveEngineType.LOCAL
        )
    }

    fun setVoiceByName(voiceName: String) {
        val gender = if (voiceName.contains("Hombre", ignoreCase = true) || voiceName.equals(com.example.tts.VoiceGender.HOMBRE.id, ignoreCase = true)) {
            com.example.tts.VoiceGender.HOMBRE
        } else {
            com.example.tts.VoiceGender.MUJER
        }
        setVoiceGender(gender)
    }

    fun setVoiceGender(gender: com.example.tts.VoiceGender) {
        ttsManager?.setVoiceGender(gender, continuePlayingCurrentFragment = _uiState.value.lecturaActiva || _uiState.value.isLessonSpeechPlaying)
        _uiState.value = _uiState.value.copy(
            selectedVoiceGender = gender,
            activeTtsVoice = gender.displayName
        )
        prefs.edit().putString("tts_voice_gender", gender.id).apply()
        prefs.edit().putString("tts_voice", gender.displayName).apply()
    }

    fun previewVoice(gender: com.example.tts.VoiceGender) {
        _uiState.value = _uiState.value.copy(isTtsPreviewPlaying = true)
        ttsManager?.previewVoice(gender) {
            _uiState.value = _uiState.value.copy(isTtsPreviewPlaying = false)
        }
    }

    fun leerDesdeBloqueEstructurado(block: StructuredBlock) {
        val fragIndex = _uiState.value.fragmentosLectura.indexOfFirst { it.id == block.fragmentId }
        if (fragIndex >= 0) {
            saltarAFragmento(fragIndex)
            iniciarLectura()
        }
    }

    fun leerTablaAccesible(table: StructuredTable) {
        ttsManager?.stop()
        val spoken = table.toAccessibleSpokenText()
        _uiState.value = _uiState.value.copy(
            voiceStatus = "🔊 Leyendo tabla estructurada..."
        )
        ttsManager?.speakDirect(spoken) {
            _uiState.value = _uiState.value.copy(
                voiceStatus = "Lectura de tabla completada."
            )
        }
    }

    fun alternarModoBusqueda(semantica: Boolean) {
        _uiState.value = _uiState.value.copy(isSemanticSearchMode = semantica)
    }

    fun ejecutarBusquedaSemantica(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            _uiState.value = _uiState.value.copy(semanticSearchResults = emptyList(), isSemanticSearching = false)
            return
        }
        _uiState.value = _uiState.value.copy(isSemanticSearching = true)
        viewModelScope.launch {
            val blocks = _uiState.value.structuredBlocks
            val terms = aiService.extraerTerminos(clean)
            val scored = mutableListOf<SemanticSearchResult>()

            blocks.forEach { block ->
                val blockText = block.text
                val blockLower = blockText.lowercase()
                var matchCount = 0
                terms.forEach { term ->
                    if (blockLower.contains(term)) matchCount++
                }
                val isExact = blockLower.contains(clean.lowercase())
                val score = if (isExact) 1.0f else (matchCount.toFloat() / terms.size.coerceAtLeast(1))
                if (score > 0.25f || isExact) {
                    val section = blocks.filter { it.pageNumber <= block.pageNumber && (it.type == BlockType.CHAPTER_HEADER || it.type == BlockType.ARTICLE_HEADER) }.lastOrNull()?.text ?: "Página ${block.pageNumber}"
                    scored.add(
                        SemanticSearchResult(
                            fragmentId = block.fragmentId,
                            pageNumber = block.pageNumber,
                            sectionTitle = section.take(50),
                            matchedText = block.text.take(180),
                            explanation = if (isExact) "Coincidencia exacta encontrada en el texto" else "Conceptos coincidentes ($matchCount/${terms.size}): ${terms.filter { blockLower.contains(it) }.joinToString(", ")}",
                            relevanceScore = score,
                            isExactMatch = isExact
                        )
                    )
                }
            }
            val sorted = scored.sortedByDescending { it.relevanceScore }.take(15)
            _uiState.value = _uiState.value.copy(
                semanticSearchResults = sorted,
                isSemanticSearching = false
            )
        }
    }

    fun irAResultadoSemantico(result: SemanticSearchResult) {
        val fragIndex = _uiState.value.fragmentosLectura.indexOfFirst { it.id == result.fragmentId }
        if (fragIndex >= 0) {
            saltarAFragmento(fragIndex)
            cambiarPaginaOriginal(result.pageNumber)
        }
    }

    fun actualizarConfiguracionLectura(config: ReadingAnnouncementsConfig) {
        _uiState.value = _uiState.value.copy(readingAnnouncements = config)
    }

    fun explicarFragmentoConAi(texto: String) {
        enviarPregunta("Explica este fragmento legal de forma sencilla, con conceptos clave y un ejemplo práctico:\n\"$texto\"")
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
                    val firstPage = repository.getPageText(doc.id, 1)
                    if (firstPage.isNotBlank()) {
                        append("Fragmento inicial:\n")
                        append(firstPage.take(300))
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
        setVoiceByName(voiceName)
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
        detenerLecturaChat()

        agregarMensaje(remitente = "Tú", texto = preguntaLimpia, isUser = true)
        val state = _uiState.value
        val paginaActual = obtenerPaginaActual()
        val documentTitle = state.documentTitle
        val paginas = state.paginas
        val historialConversacion = state.chatMessages

        _uiState.value = _uiState.value.copy(
            isAskingAi = true,
            chatProcessState = ChatProcessState.SEARCHING
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(chatProcessState = ChatProcessState.GENERATING)
            val respuesta = aiService.responderPreguntaConHistorial(
                pregunta = preguntaLimpia,
                documentTitle = documentTitle,
                paginas = paginas,
                paginaActual = paginaActual,
                historialConversacion = historialConversacion,
                context = getApplication(),
                documentId = state.currentDocumentId
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
                chatProcessState = if (_uiState.value.autoReadChatResponses) ChatProcessState.PREPARING_AUDIO else ChatProcessState.READY,
                showContinueButton = true,
                isContinueEnabled = true,
                voiceStatus = "Pregunta respondida. Puedes hacer preguntas de seguimiento o pulsar CONTINUAR."
            )

            val docId = _uiState.value.currentDocumentId
            if (docId.isNotBlank()) {
                repository.updateQuestionsHistory(docId, updatedMessages)
            }

            if (_uiState.value.autoReadChatResponses) {
                leerRespuestaChat(updatedMessages.lastIndex, respuesta)
            }
        }
    }

    fun toggleChatVoiceListening(onResult: (String) -> Unit) {
        if (_uiState.value.isChatVoiceListening) {
            speechHelper?.stopListening()
            _uiState.value = _uiState.value.copy(
                isChatVoiceListening = false,
                chatProcessState = ChatProcessState.READY,
                chatVoiceStatus = "Listo"
            )
        } else {
            detenerLecturaChat()
            _uiState.value = _uiState.value.copy(
                isChatVoiceListening = true,
                chatProcessState = ChatProcessState.LISTENING,
                chatVoiceStatus = "🎙 Escuchando voz... Habla tu consulta jurídica"
            )
            speechHelper?.startListening(
                customOnResultText = { recognizedText ->
                    _uiState.value = _uiState.value.copy(
                        isChatVoiceListening = false,
                        chatProcessState = ChatProcessState.READY,
                        chatVoiceStatus = "Voz capturada con éxito."
                    )
                    onResult(recognizedText)
                },
                customOnStateChanged = { listening, status ->
                    val newState = if (listening) ChatProcessState.LISTENING
                        else if (status.contains("Transcribiendo", ignoreCase = true)) ChatProcessState.TRANSCRIBING
                        else ChatProcessState.READY
                    _uiState.value = _uiState.value.copy(
                        isChatVoiceListening = listening,
                        chatProcessState = newState,
                        chatVoiceStatus = status
                    )
                }
            )
        }
    }

    fun leerRespuestaChat(messageIndex: Int, texto: String) {
        if (_uiState.value.playingChatMessageIndex == messageIndex && _uiState.value.isReadingChatResponse) {
            pausarLecturaChat()
            return
        }
        if (_uiState.value.playingChatMessageIndex == messageIndex && _uiState.value.isReadingChatPaused) {
            reanudarLecturaChat(texto)
            return
        }
        ttsManager?.stop()
        ttsManager?.setSpeechRate(_uiState.value.chatSpeechRate)
        _uiState.value = _uiState.value.copy(
            playingChatMessageIndex = messageIndex,
            isReadingChatResponse = true,
            isReadingChatPaused = false,
            chatProcessState = ChatProcessState.PLAYING_AUDIO
        )
        val cleanSpeech = texto
            .replace("Texto encontrado en el documento:", "Pasaje del documento:")
            .replace("Página ", "en la página ")
            .replace(Regex("[*#_✓•]"), "")
        ttsManager?.speak(cleanSpeech)
    }

    fun pausarLecturaChat() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            isReadingChatResponse = false,
            isReadingChatPaused = true,
            chatProcessState = ChatProcessState.READY
        )
    }

    fun reanudarLecturaChat(texto: String? = null) {
        val state = _uiState.value
        val msgIdx = state.playingChatMessageIndex ?: return
        val currentText = texto ?: state.chatMessages.getOrNull(msgIdx)?.texto ?: return
        ttsManager?.stop()
        ttsManager?.setSpeechRate(state.chatSpeechRate)
        _uiState.value = _uiState.value.copy(
            isReadingChatResponse = true,
            isReadingChatPaused = false,
            chatProcessState = ChatProcessState.PLAYING_AUDIO
        )
        val cleanSpeech = currentText
            .replace("Texto encontrado en el documento:", "Pasaje del documento:")
            .replace("Página ", "en la página ")
            .replace(Regex("[*#_✓•]"), "")
        ttsManager?.speak(cleanSpeech)
    }

    fun detenerLecturaChat() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            playingChatMessageIndex = null,
            isReadingChatResponse = false,
            isReadingChatPaused = false,
            chatProcessState = ChatProcessState.READY
        )
    }

    fun cambiarVelocidadChat(rate: Float) {
        _uiState.value = _uiState.value.copy(chatSpeechRate = rate)
        ttsManager?.setSpeechRate(rate)
    }

    fun toggleAutoReadChatResponses() {
        _uiState.value = _uiState.value.copy(
            autoReadChatResponses = !_uiState.value.autoReadChatResponses
        )
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

    // --- MÉTODOS DE ENTRENADOR DE APRENDIZAJE E IA COGNITIVA ---

    fun setReaderLearningMode(mode: ReaderLearningMode) {
        _uiState.value = _uiState.value.copy(
            learningMode = mode,
            fragmentsInCurrentBlock = 0
        )
        val status = when (mode) {
            ReaderLearningMode.FREE_READING -> "Modo Lectura Libre activado: escucha continua."
            ReaderLearningMode.GUIDED_LEARNING -> "Modo Aprendizaje Guiado activado: pausas y preguntas por bloque."
            ReaderLearningMode.SPACED_REVIEW -> "Modo Repaso Espaciado: consolidación de memoria."
        }
        _uiState.value = _uiState.value.copy(voiceStatus = status)
    }

    fun setGuidedBlockSize(size: Int) {
        _uiState.value = _uiState.value.copy(guidedBlockSize = size.coerceIn(1, 10))
    }

    fun setShowStudyCoachDialog(show: Boolean, tab: Int = 0) {
        _uiState.value = _uiState.value.copy(
            showStudyCoachDialog = show,
            studyCoachSelectedTab = tab
        )
        if (show) {
            viewModelScope.launch {
                actualizarEstadisticasEstudio()
            }
        }
    }

    fun setSocraticTutorLevel(level: String) {
        _uiState.value = _uiState.value.copy(socraticTutorLevel = level)
    }

    fun activarPreguntaGuiada() {
        val state = _uiState.value
        if (state.fragmentosLectura.isEmpty()) return
        val currentFrag = state.fragmentosLectura.getOrNull(state.indiceActual) ?: return

        _uiState.value = _uiState.value.copy(
            isEvaluatingAnswer = false,
            guidedEvaluationResult = null,
            guidedAnswerInput = "",
            showGuidedQuestionDialog = true,
            voiceStatus = "🧠 Comprobación de evocación activa..."
        )

        viewModelScope.launch {
            val q = learningEngine.generarPreguntaEvocacion(
                documentTitle = state.documentTitle,
                fragmentoTexto = currentFrag.texto,
                pagina = currentFrag.pagina,
                fragmentoId = currentFrag.id
            )
            _uiState.value = _uiState.value.copy(
                activeGuidedQuestion = q
            )
        }
    }

    fun cerrarDialogoPreguntaGuiada() {
        _uiState.value = _uiState.value.copy(showGuidedQuestionDialog = false)
    }

    fun setGuidedAnswerInput(text: String) {
        _uiState.value = _uiState.value.copy(guidedAnswerInput = text)
    }

    fun toggleVoiceListening() {
        if (_uiState.value.isVoiceListening) {
            speechHelper?.stopListening()
        } else {
            speechHelper?.startListening()
        }
    }

    fun enviarRespuestaGuiada() {
        val state = _uiState.value
        val q = state.activeGuidedQuestion ?: return
        val ans = state.guidedAnswerInput.trim()
        if (ans.isBlank()) return

        val currentFrag = state.fragmentosLectura.getOrNull(state.indiceActual)
        val fragText = currentFrag?.texto ?: q.contextSnippet

        _uiState.value = _uiState.value.copy(isEvaluatingAnswer = true)

        viewModelScope.launch {
            val eval = learningEngine.evaluarRespuestaSemantica(
                pregunta = q,
                respuestaUsuario = ans,
                fragmentoTexto = fragText,
                documentTitle = state.documentTitle
            )

            _uiState.value = _uiState.value.copy(
                isEvaluatingAnswer = false,
                guidedEvaluationResult = eval
            )

            // Registrar en estadísticas y programar en Repaso Espaciado automáticamente
            studyRepository.recordStudySession(
                documentId = state.currentDocumentId,
                documentTitle = state.documentTitle,
                mode = "GUIDED",
                durationMinutes = 5,
                questionsAnswered = 1,
                questionsCorrect = if (eval.isUnderstood) 1 else 0,
                selfReflection = if (eval.isUnderstood) "Concepto comprendido" else "Requiere refuerzo"
            )

            // Auto-guardar en repaso espaciado
            studyRepository.saveOrUpdateCard(
                documentId = state.currentDocumentId,
                concept = q.concept,
                question = q.questionText,
                answer = eval.fullExplanation,
                sourcePage = q.pageNumber,
                intervalDays = if (eval.isUnderstood) 3 else 1,
                correctCount = if (eval.isUnderstood) 1 else 0,
                failCount = if (!eval.isUnderstood) 1 else 0
            )

            actualizarEstadisticasEstudio()
        }
    }

    fun toggleShowReviewAnswer() {
        _uiState.value = _uiState.value.copy(showReviewAnswer = !_uiState.value.showReviewAnswer)
    }

    fun calificarTarjetaRepaso(rating: Int) { // 0: No lo recordé, 1: Me costó, 2: Lo recordé
        val state = _uiState.value
        val card = state.dueCards.getOrNull(state.currentReviewCardIndex) ?: return

        val (nextInterval, nextEase) = learningEngine.calcularProximoRepaso(
            card.intervalDays,
            card.easeFactor,
            rating
        )

        viewModelScope.launch {
            studyRepository.recordReviewOutcome(
                cardId = card.id,
                isSuccess = rating > 0,
                newIntervalDays = nextInterval,
                newEaseFactor = nextEase
            )

            actualizarEstadisticasEstudio()

            val nextIndex = if (state.currentReviewCardIndex + 1 < state.dueCards.size) {
                state.currentReviewCardIndex + 1
            } else 0

            _uiState.value = _uiState.value.copy(
                currentReviewCardIndex = nextIndex,
                showReviewAnswer = false
            )
        }
    }

    fun generarCornellPaginaActual() {
        val state = _uiState.value
        val pagNum = obtenerPaginaActual()
        val pag = state.paginas.firstOrNull { it.numero == pagNum } ?: return

        _uiState.value = _uiState.value.copy(isGeneratingCornell = true)

        viewModelScope.launch {
            val note = learningEngine.generarNotasCornell(
                documentTitle = state.documentTitle,
                paginaTexto = pag.texto,
                pagina = pagNum
            )
            studyRepository.saveCornellNote(note)
            _uiState.value = _uiState.value.copy(
                activeCornellNote = note,
                isGeneratingCornell = false
            )
        }
    }

    fun generarCodificacionDualPaginaActual() {
        val state = _uiState.value
        val pagNum = obtenerPaginaActual()
        val pag = state.paginas.firstOrNull { it.numero == pagNum } ?: return

        _uiState.value = _uiState.value.copy(isGeneratingDualCoding = true)

        viewModelScope.launch {
            val map = learningEngine.generarCodificacionDual(
                documentTitle = state.documentTitle,
                texto = pag.texto,
                pagina = pagNum
            )
            _uiState.value = _uiState.value.copy(
                dualCodingMap = map,
                isGeneratingDualCoding = false
            )
        }
    }

    fun iniciarPomodoro() {
        pomodoroJob?.cancel()
        _uiState.value = _uiState.value.copy(isPomodoroActive = true)
        pomodoroJob = viewModelScope.launch {
            while (_uiState.value.isPomodoroActive && _uiState.value.pomodoroRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                val remaining = _uiState.value.pomodoroRemainingSeconds - 1
                if (remaining <= 0) {
                    val wasBreak = _uiState.value.pomodoroIsBreak
                    val newBreak = !wasBreak
                    _uiState.value = _uiState.value.copy(
                        pomodoroIsBreak = newBreak,
                        pomodoroRemainingSeconds = if (newBreak) 5 * 60 else 25 * 60,
                        isPomodoroActive = false,
                        voiceStatus = if (newBreak) "☕ Tiempo de descanso Pomodoro (5 min)." else "🎯 Momento de concentración en la lectura."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(pomodoroRemainingSeconds = remaining)
                }
            }
        }
    }

    fun pausarPomodoro() {
        pomodoroJob?.cancel()
        _uiState.value = _uiState.value.copy(isPomodoroActive = false)
    }

    fun reiniciarPomodoro(minutos: Int = 25) {
        pomodoroJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isPomodoroActive = false,
            pomodoroRemainingSeconds = minutos * 60,
            pomodoroIsBreak = false
        )
    }

    fun consultarTutorSocraticoNivel(nivel: String) {
        _uiState.value = _uiState.value.copy(socraticTutorLevel = nivel)
        val pagActual = obtenerPaginaActual()
        val fragActual = _uiState.value.fragmentosLectura.getOrNull(_uiState.value.indiceActual)?.texto?.take(180) ?: ""
        val prompt = when (nivel) {
            "Feynman" -> "Aplica la Técnica Feynman sobre lo que estoy leyendo en la página $pagActual. Explícalo con tus palabras como si fuera para un principiante total y dame una analogía cotidiana."
            "Sencillo" -> "Explícame en lenguaje muy sencillo y directo la idea central de la página $pagActual (\"$fragActual\")."
            "Universitario" -> "Realiza un análisis dogmático y profundo a nivel universitario de la disposición de la página $pagActual."
            "Ejemplo" -> "Dame un caso o ejemplo práctico hipotético para aplicar lo establecido en la página $pagActual."
            else -> "Explícame el concepto de la página $pagActual."
        }
        enviarPregunta(prompt)
    }

    private suspend fun actualizarEstadisticasEstudio() {
        val stats = studyRepository.computeStats()
        _uiState.value = _uiState.value.copy(studyStats = stats)
    }

    // ==========================================
    // TUTOR PERSONAL MULTIMODAL DE APRENDIZAJE
    // ==========================================

    fun generarLeccionMultimodalPaginaActual() {
        val state = _uiState.value
        val pagNum = obtenerPaginaActual()
        val pag = state.paginas.firstOrNull { it.numero == pagNum }
        val texto = pag?.texto ?: state.fragmentosLectura.getOrNull(state.indiceActual)?.texto ?: ""
        if (texto.isBlank()) return
        generarLeccionMultimodalParaTexto(state.documentTitle, texto, pagNum)
    }

    fun generarLeccionMultimodalFragmento(fragmento: FragmentoLectura) {
        val state = _uiState.value
        generarLeccionMultimodalParaTexto(state.documentTitle, fragmento.texto, fragmento.pagina)
    }

    fun generarLeccionMultimodalParaTexto(documentTitle: String, texto: String, pagina: Int) {
        detenerLecturaLeccion()
        _uiState.value = _uiState.value.copy(
            isGeneratingMultimodalLesson = true,
            showMultimodalLessonScreen = true,
            currentSpokenHighlightIndex = -1,
            isLessonSpeechPlaying = false
        )

        viewModelScope.launch {
            val lesson = learningEngine.transformarEnLeccionMultimodal(
                documentTitle = documentTitle,
                fragmentoTexto = texto,
                pagina = pagina
            )

            _uiState.value = _uiState.value.copy(
                activeMultimodalLesson = lesson,
                isGeneratingMultimodalLesson = false
            )
        }
    }

    fun abrirTutorMultimodal() {
        if (_uiState.value.activeMultimodalLesson == null) {
            generarLeccionMultimodalPaginaActual()
        } else {
            _uiState.value = _uiState.value.copy(showMultimodalLessonScreen = true)
        }
    }

    fun cerrarTutorMultimodal() {
        detenerLecturaLeccion()
        _uiState.value = _uiState.value.copy(showMultimodalLessonScreen = false)
    }

    fun seleccionarSeccionLeccion(index: Int) {
        detenerLecturaLeccion()
        val lesson = _uiState.value.activeMultimodalLesson ?: return
        if (index in lesson.sections.indices) {
            _uiState.value = _uiState.value.copy(
                activeMultimodalLesson = lesson.copy(activeSectionIndex = index),
                currentSpokenHighlightIndex = -1
            )
        }
    }

    fun setLessonSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 2.0f)
        _uiState.value = _uiState.value.copy(lessonSpeechRate = clamped)
        if (_uiState.value.isLessonSpeechPlaying) {
            val lesson = _uiState.value.activeMultimodalLesson
            val section = lesson?.sections?.getOrNull(lesson.activeSectionIndex)
            if (section != null) {
                reproducirSeccionLeccionSincronizada(section)
            }
        }
    }

    fun toggleReproduccionSeccionLeccion() {
        if (_uiState.value.isLessonSpeechPlaying) {
            detenerLecturaLeccion()
        } else {
            val lesson = _uiState.value.activeMultimodalLesson ?: return
            val section = lesson.sections.getOrNull(lesson.activeSectionIndex) ?: return
            reproducirSeccionLeccionSincronizada(section)
        }
    }

    fun reproducirSeccionLeccionSincronizada(section: MultimodalLessonSection) {
        detenerLecturaLeccion()
        ttsManager?.stop()

        _uiState.value = _uiState.value.copy(
            isLessonSpeechPlaying = true,
            currentSpokenHighlightIndex = if (section.timedHighlights.isNotEmpty()) 0 else -1
        )

        // Reproducir audio usando TTS del dispositivo
        ttsManager?.setSpeechRate(_uiState.value.lessonSpeechRate)
        ttsManager?.speakFragment(
            fragmentId = -888, // Identificador dedicado para tutor multimodal
            text = section.content
        )

        // Marcas de tiempo sincronizadas para resaltado visual continuo de lectura (karaoke visual)
        lessonSpeechJob = viewModelScope.launch {
            val highlights = section.timedHighlights
            val speedFactor = _uiState.value.lessonSpeechRate.coerceAtLeast(0.5f)
            for (idx in highlights.indices) {
                if (!_uiState.value.isLessonSpeechPlaying) break
                _uiState.value = _uiState.value.copy(currentSpokenHighlightIndex = idx)
                val duration = (highlights[idx].durationMs / speedFactor).toLong().coerceAtLeast(100L)
                kotlinx.coroutines.delay(duration)
            }
            if (_uiState.value.isLessonSpeechPlaying) {
                _uiState.value = _uiState.value.copy(
                    isLessonSpeechPlaying = false,
                    currentSpokenHighlightIndex = -1
                )
            }
        }
    }

    fun detenerLecturaLeccion() {
        lessonSpeechJob?.cancel()
        lessonSpeechJob = null
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            isLessonSpeechPlaying = false,
            currentSpokenHighlightIndex = -1
        )
    }

    fun responderEjercicioLeccion(sectionIndex: Int, exerciseIndex: Int, selectedOptionIndex: Int) {
        val lesson = _uiState.value.activeMultimodalLesson ?: return
        val section = lesson.sections.getOrNull(sectionIndex) ?: return
        val exercise = section.exercises.getOrNull(exerciseIndex) ?: return

        val isCorrect = selectedOptionIndex == exercise.correctIndex
        val updatedSection = section.copy(
            selectedOptionIndex = selectedOptionIndex,
            isExercisePassed = isCorrect,
            isCompleted = isCorrect // Desbloquea la continuación cuando es correcto
        )

        val updatedSections = lesson.sections.toMutableList()
        updatedSections[sectionIndex] = updatedSection

        val allCompleted = updatedSections.all { it.isCompleted }

        _uiState.value = _uiState.value.copy(
            activeMultimodalLesson = lesson.copy(
                sections = updatedSections,
                isFinished = allCompleted
            )
        )

        // Registrar progreso pedagógico en Room
        viewModelScope.launch {
            studyRepository.recordStudySession(
                documentId = _uiState.value.currentDocumentId,
                documentTitle = _uiState.value.documentTitle,
                mode = "MULTIMODAL_LESSON",
                durationMinutes = 3,
                questionsAnswered = 1,
                questionsCorrect = if (isCorrect) 1 else 0,
                selfReflection = if (isCorrect) "Comprensión verificada: ${section.title}" else "Revisión conceptual necesaria"
            )
            actualizarEstadisticasEstudio()
        }
    }

    fun toggleHintEjercicioLeccion(sectionIndex: Int) {
        val lesson = _uiState.value.activeMultimodalLesson ?: return
        val section = lesson.sections.getOrNull(sectionIndex) ?: return
        val updatedSection = section.copy(showHint = !section.showHint)
        val updatedSections = lesson.sections.toMutableList()
        updatedSections[sectionIndex] = updatedSection
        _uiState.value = _uiState.value.copy(
            activeMultimodalLesson = lesson.copy(sections = updatedSections)
        )
    }

    fun avanzarSiguienteSeccionLeccion() {
        detenerLecturaLeccion()
        val lesson = _uiState.value.activeMultimodalLesson ?: return
        val currentIdx = lesson.activeSectionIndex
        val nextIdx = currentIdx + 1
        if (nextIdx < lesson.sections.size) {
            _uiState.value = _uiState.value.copy(
                activeMultimodalLesson = lesson.copy(activeSectionIndex = nextIdx),
                currentSpokenHighlightIndex = -1
            )
        } else {
            _uiState.value = _uiState.value.copy(
                activeMultimodalLesson = lesson.copy(isFinished = true)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        lessonSpeechJob?.cancel()
        pomodoroJob?.cancel()
        speechHelper?.stopListening()
        ttsManager?.shutdown()
        pdfPageRenderer.close()
    }
}

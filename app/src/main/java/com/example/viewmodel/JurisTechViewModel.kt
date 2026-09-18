package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JurisTechAiService
import com.example.data.local.AppDatabase
import com.example.data.local.RecentDocumentEntity
import com.example.data.local.RecentDocumentRepository
import com.example.model.ChatMessage
import com.example.model.DocumentPage
import com.example.model.FragmentoLectura
import com.example.model.SampleDocument
import com.example.model.SampleDocumentRepository
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
    val speechRate: Float = 0.95f,
    val showContinueButton: Boolean = false,
    val isContinueEnabled: Boolean = false,
    val isProcessingPdf: Boolean = false,
    val pdfProgressPercent: Int = 0,
    val pdfProgressText: String = "",
    val pdfErrorMessage: String? = null,
    val recentDocuments: List<RecentDocumentEntity> = emptyList()
)

class JurisTechViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(JurisTechUiState())
    val uiState: StateFlow<JurisTechUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(application)
    private val repository = RecentDocumentRepository(database.recentDocumentDao())

    private val aiService = JurisTechAiService()
    private var ttsManager: TtsManager? = null

    init {
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

        // Observar la lista reactiva de documentos recientes almacenados en Room
        viewModelScope.launch {
            repository.recentDocuments.collect { recents ->
                _uiState.value = _uiState.value.copy(recentDocuments = recents)
            }
        }
    }

    fun cargarSample(sample: SampleDocument) {
        val sampleId = "sample_" + sample.title.hashCode().toString()
        cargarDocumentoCompleto(
            id = sampleId,
            titulo = sample.title,
            subtitulo = sample.subtitle,
            paginas = sample.pages,
            sourceType = "SAMPLE"
        )
    }

    fun cargarDocumentoPersonalizado(titulo: String, paginas: List<DocumentPage>, subtitulo: String = "Documento importado") {
        val docId = "doc_" + System.currentTimeMillis()
        cargarDocumentoCompleto(
            id = docId,
            titulo = titulo,
            subtitulo = subtitulo,
            paginas = paginas,
            sourceType = "IMPORTED"
        )
    }

    private fun cargarDocumentoCompleto(
        id: String,
        titulo: String,
        subtitulo: String,
        paginas: List<DocumentPage>,
        sourceType: String
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
            voiceStatus = "Documento listo para leer.",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Documento '$titulo' cargado y guardado en recientes (${paginas.size} páginas, ${fragmentos.size} fragmentos). Puedes comenzar la lectura con '▶ Leer' o hacer preguntas en cualquier momento."
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
                sourceType = sourceType
            )
        }
    }

    /**
     * Retoma la lectura y consulta de un documento guardado en Room
     * restaurando exactamente la página y fragmento donde el usuario lo dejó.
     */
    fun retomarDocumento(entity: RecentDocumentEntity) {
        ttsManager?.stop()

        var paginas = repository.parsePages(entity.pagesJson)
        if (paginas.isEmpty()) {
            // Si por alguna razón el json estuviera vacío, buscar coincidencia en samples
            val sampleMatch = SampleDocumentRepository.samples.firstOrNull { it.title == entity.title }
            if (sampleMatch != null) {
                paginas = sampleMatch.pages
            }
        }

        if (paginas.isEmpty()) return

        val fragmentos = SampleDocumentRepository.crearFragmentos(paginas)
        val indiceRestaurado = entity.lastFragmentIndex.coerceIn(0, maxOf(0, fragmentos.size - 1))
        val paginaRestaurada = fragmentos.getOrNull(indiceRestaurado)?.pagina ?: entity.lastPage

        _uiState.value = _uiState.value.copy(
            currentDocumentId = entity.id,
            documentTitle = entity.title,
            paginas = paginas,
            fragmentosLectura = fragmentos,
            indiceActual = indiceRestaurado,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            voiceStatus = "Lectura retomada en Página $paginaRestaurada (${indiceRestaurado + 1}/${fragmentos.size}).",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = listOf(
                ChatMessage(
                    remitente = "JurisTech AI",
                    texto = "Has retomado '${entity.title}'. Posición actual: Página $paginaRestaurada de ${paginas.size} (fragmento ${indiceRestaurado + 1}/${fragmentos.size}). Todo el contenido está disponible para consulta o lectura.",
                    isUser = false
                )
            )
        )

        // Actualizar fecha de acceso reciente en Room
        viewModelScope.launch {
            repository.updateReadingProgress(entity.id, paginaRestaurada, indiceRestaurado)
        }
    }

    /**
     * Elimina un documento de la lista de recientes en Room
     */
    fun eliminarDocumentoReciente(id: String) {
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
            _uiState.value = _uiState.value.copy(indiceActual = indice)
            persistirProgresoActual(indice)
            if (state.lecturaActiva) {
                leerFragmentoActual()
            } else {
                val fragmento = state.fragmentosLectura[indice]
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Posición: Página ${fragmento.pagina} (Fragmento ${indice + 1}/${state.fragmentosLectura.size})"
                )
            }
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
        ttsManager?.setSpeechRate(rate)
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

            agregarMensaje(remitente = "JurisTech AI", texto = respuesta, isUser = false)

            _uiState.value = _uiState.value.copy(
                isAskingAi = false,
                showContinueButton = true,
                isContinueEnabled = true,
                voiceStatus = "Pregunta respondida. Puedes hacer preguntas de seguimiento o pulsar CONTINUAR."
            )
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

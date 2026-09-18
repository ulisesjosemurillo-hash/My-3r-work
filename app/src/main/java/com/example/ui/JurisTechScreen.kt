package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurisTechScreen(
    viewModel: JurisTechViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showQuestionsScreen by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: uri.lastPathSegment ?: "Documento.pdf"
            viewModel.procesarArchivoPdf(uri, fileName)
        }
    }

    if (uiState.currentDocumentId.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            JurisTechLibraryScreen(
                uiState = uiState,
                viewModel = viewModel,
                onSelectDocument = { doc ->
                    viewModel.retomarDocumento(doc)
                    showQuestionsScreen = false
                },
                onAddFromFile = {
                    filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*"))
                }
            )

            if (uiState.isProcessingPdf || (uiState.pdfProgressPercent in 1..99)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JtSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = JtGreenPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.pdfProgressText,
                                color = JtPrimaryText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.pdfProgressPercent}%",
                                color = JtGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        BackHandler {
            if (showQuestionsScreen) {
                showQuestionsScreen = false
            } else {
                viewModel.cerrarDocumento()
            }
        }

        AnimatedContent(targetState = showQuestionsScreen, label = "ScreenTransition") { showQ ->
            if (showQ) {
                QuestionsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { showQuestionsScreen = false }
                )
            } else {
                ReaderScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { viewModel.cerrarDocumento() },
                    onOpenQuestions = { showQuestionsScreen = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onBack: () -> Unit,
    onOpenQuestions: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showIndexModal by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showChangeCoverDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReadingSettingsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var fragmentForOpinion by remember { mutableStateOf<FragmentoLectura?>(null) }
    var pendingAudioFragment by remember { mutableStateOf<FragmentoLectura?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAudioFragment?.let { frag ->
                viewModel.iniciarGrabacionAudio(frag.id)
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de micrófono para notas de voz", Toast.LENGTH_SHORT).show()
        }
        pendingAudioFragment = null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && uiState.currentDocumentId.isNotBlank()) {
            viewModel.cambiarCaratulaDocumento(uiState.currentDocumentId, uri.toString(), null)
            Toast.makeText(context, "Carátula actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.indiceActual) {
        if (uiState.fragmentosLectura.isNotEmpty() && uiState.indiceActual in uiState.fragmentosLectura.indices) {
            listState.animateScrollToItem(uiState.indiceActual)
        }
    }

    Scaffold(
        containerColor = JtBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.documentTitle,
                                color = JtPrimaryText,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val totalPages = if (uiState.paginas.isNotEmpty()) uiState.paginas.size else 1
                            val currentPage = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 1
                            Text(
                                text = "Pág. $currentPage de $totalPages",
                                color = JtSecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                        }
                    },
                    actions = {
                        // Botón buscar en el documento
                        IconButton(onClick = {
                            if (uiState.showInDocumentSearchBar) viewModel.cerrarBusquedaEnDocumento() else viewModel.abrirBusquedaEnDocumento()
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar en libro",
                                tint = if (uiState.showInDocumentSearchBar) JtGreenPrimary else JtPrimaryText
                            )
                        }

                        // Botón para guardar / marcar marcador
                        val isBookmarked = viewModel.esMarcadorActual()
                        IconButton(onClick = {
                            val currentFrag = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)
                            if (currentFrag != null) {
                                val snippet = currentFrag.texto.take(80)
                                viewModel.toggleMarcador(currentFrag.id, currentFrag.pagina, snippet)
                                val msg = if (isBookmarked) "Marcador eliminado" else "Página guardada en marcadores"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Guardar marcador",
                                tint = if (isBookmarked) JtGold else JtPrimaryText
                            )
                        }

                        // Botón de 3 puntos con menú de opciones
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = JtPrimaryText)
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(JtSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cambiar nombre del libro", color = JtPrimaryText) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = JtGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cambiar carátula", color = JtPrimaryText) },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = JtGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showChangeCoverDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ajustes de lectura y voz", color = JtPrimaryText) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = JtGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showReadingSettingsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Abrir Índice y Marcadores", color = JtPrimaryText) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = JtGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showIndexModal = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        val currentFrag = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)
                                        val isHighlighted = currentFrag != null && uiState.highlights.contains(currentFrag.id)
                                        Text(if (isHighlighted) "Quitar resaltado amarillo" else "Resaltar fragmento actual", color = JtPrimaryText)
                                    },
                                    leadingIcon = { Icon(Icons.Default.BorderColor, contentDescription = null, tint = JtGold) },
                                    onClick = {
                                        showMenu = false
                                        val currentFrag = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)
                                        if (currentFrag != null) {
                                            viewModel.toggleResaltado(currentFrag.id)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Agregar opinión / nota aquí", color = JtPrimaryText) },
                                    leadingIcon = { Icon(Icons.Default.RateReview, contentDescription = null, tint = JtGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        fragmentForOpinion = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)
                                    }
                                )
                                HorizontalDivider(color = JtBorder)
                                DropdownMenuItem(
                                    text = { Text("Reiniciar lectura al inicio", color = JtSecondaryText) },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = JtSecondaryText) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.saltarAFragmento(0)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar libro de la biblioteca", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cerrar libro", color = JtSecondaryText) },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = JtSecondaryText) },
                                    onClick = {
                                        showMenu = false
                                        onBack()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = JtBackground)
                )

                // Barra de búsqueda dentro del documento
                if (uiState.showInDocumentSearchBar) {
                    Surface(
                        color = JtSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.inDocumentSearchQuery,
                                onValueChange = { viewModel.buscarEnDocumentoActual(it) },
                                placeholder = { Text("Buscar palabra o artículo...", color = JtSecondaryText, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = JtPrimaryText,
                                    unfocusedTextColor = JtPrimaryText
                                )
                            )
                            if (uiState.inDocumentSearchResults.isNotEmpty()) {
                                val currentMatch = uiState.inDocumentSearchCurrentIndex + 1
                                val totalMatches = uiState.inDocumentSearchResults.size
                                Text("$currentMatch/$totalMatches", color = JtSecondaryText, fontSize = 12.sp)
                                IconButton(onClick = { viewModel.irAAnteriorResultado() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Anterior", tint = JtPrimaryText, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.irASiguienteResultado() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Siguiente", tint = JtPrimaryText, modifier = Modifier.size(18.dp))
                                }
                            } else if (uiState.inDocumentSearchQuery.isNotBlank()) {
                                Text("0 resultados", color = JtSecondaryText, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.cerrarBusquedaEnDocumento() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = JtSecondaryText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            ReaderBottomControls(
                uiState = uiState,
                viewModel = viewModel,
                onOpenQuestions = onOpenQuestions,
                onOpenIndex = { showIndexModal = true }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val totalPages = if (uiState.paginas.isNotEmpty()) uiState.paginas.size else 1
                    val currentPage = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 1

                    Surface(
                        color = JtBorder,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Página $currentPage de $totalPages",
                            color = JtSecondaryText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(uiState.fragmentosLectura.size) { index ->
                val fragmento = uiState.fragmentosLectura[index]
                val isCurrent = index == uiState.indiceActual
                val isHighlighted = uiState.highlights.contains(fragmento.id)
                val opinion = uiState.opinions.firstOrNull { it.fragmentId == fragmento.id }
                val audioComment = uiState.audioComments.firstOrNull { it.fragmentId == fragmento.id }
                val isRecording = uiState.isRecordingAudio && uiState.recordingFragmentId == fragmento.id
                val isPlayingAudio = audioComment != null && uiState.playingAudioPath == audioComment.audioPath

                ReaderFragmentItem(
                    fragmento = fragmento,
                    isCurrent = isCurrent,
                    isHighlighted = isHighlighted,
                    opinion = opinion,
                    audioComment = audioComment,
                    isRecording = isRecording,
                    isPlayingAudio = isPlayingAudio,
                    searchQuery = uiState.inDocumentSearchQuery,
                    fontSizeScale = uiState.fontSizeScale,
                    lineSpacing = uiState.lineSpacing,
                    activeHighlightColor = uiState.activeHighlightColorHex,
                    isUnderlineMode = uiState.isUnderlineMode,
                    onClick = { viewModel.saltarAFragmento(index) },
                    onToggleHighlight = { viewModel.toggleResaltado(fragmento.id) },
                    onSelectColorAndHighlight = { hexColor ->
                        viewModel.setActiveHighlightColor(hexColor)
                        if (!isHighlighted) {
                            viewModel.toggleResaltado(fragmento.id)
                        }
                    },
                    onCopyText = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Fragmento Jurídico", fragmento.texto))
                        Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    onShareText = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${fragmento.texto}\n\n— Extraído de ${uiState.documentTitle} (Pág. ${fragmento.pagina})")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir fragmento"))
                    },
                    onAddOpinion = { fragmentForOpinion = fragmento },
                    onDeleteOpinion = { if (opinion != null) viewModel.eliminarOpinion(opinion.id) },
                    onRecordAudioToggle = {
                        if (isRecording) {
                            viewModel.detenerGrabacionAudio(fragmento.id, fragmento.pagina, fragmento.texto.take(80))
                            Toast.makeText(context, "Nota de voz guardada", Toast.LENGTH_SHORT).show()
                        } else {
                            pendingAudioFragment = fragmento
                            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onPlayAudio = { audioPath -> viewModel.reproducirAudio(audioPath) },
                    onStopAudio = { viewModel.detenerReproduccionAudio() },
                    onDeleteAudio = { audioId ->
                        viewModel.eliminarAudioComentario(audioId)
                        Toast.makeText(context, "Nota de voz eliminada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Modal de Índice, Marcadores y Resaltados
    if (showIndexModal) {
        IndexAndBookmarksModal(
            uiState = uiState,
            onDismiss = { showIndexModal = false },
            onSelectPage = { pageNum ->
                viewModel.saltarAPagina(pageNum)
                showIndexModal = false
            },
            onSelectFragment = { fragIndex ->
                viewModel.saltarAFragmento(fragIndex)
                showIndexModal = false
            },
            onDeleteBookmark = { viewModel.eliminarMarcador(it) },
            onDeleteOpinion = { viewModel.eliminarOpinion(it) },
            onEditOpinion = { op ->
                fragmentForOpinion = uiState.fragmentosLectura.firstOrNull { it.id == op.fragmentId }
                showIndexModal = false
            },
            onSaveGeneralNotes = { notes ->
                viewModel.guardarNotasGeneralesDocumento(notes)
                Toast.makeText(context, "Notas generales guardadas", Toast.LENGTH_SHORT).show()
            },
            onPlayAudio = { viewModel.reproducirAudio(it) },
            onDeleteAudio = { viewModel.eliminarAudioComentario(it) }
        )
    }

    // Modal Renombrar libro dentro del lector
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(uiState.documentTitle) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre del libro", fontWeight = FontWeight.Bold, color = JtPrimaryText) },
            text = {
                Column {
                    Text("Ingresa el nuevo título:", color = JtSecondaryText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.cambiarNombreDocumento(uiState.currentDocumentId, newTitle)
                            showRenameDialog = false
                            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                    enabled = newTitle.isNotBlank()
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Cambiar Carátula dentro del lector
    if (showChangeCoverDialog) {
        val presetColors = listOf(
            0xFF1C3829 to "Verde JurisTech",
            0xFF1A2A3A to "Azul Marino Judicial",
            0xFF3E1F1F to "Burdeos Borgoña",
            0xFF2D2B1F to "Bronce Antiguo",
            0xFF181818 to "Negro Cuero"
        )

        AlertDialog(
            onDismissRequest = { showChangeCoverDialog = false },
            title = { Text("Personalizar Carátula", fontWeight = FontWeight.Bold, color = JtPrimaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Selecciona una foto de tu galería o un acabado de piel:",
                        color = JtSecondaryText,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            showChangeCoverDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir foto de la galería", color = Color.White)
                    }

                    HorizontalDivider(color = JtBorder)

                    Text("O elige un color clásico:", fontWeight = FontWeight.Medium, color = JtPrimaryText, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.forEach { (colorVal, _) ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(2.dp, JtGold, CircleShape)
                                    .clickable {
                                        viewModel.cambiarCaratulaDocumento(uiState.currentDocumentId, null, colorVal)
                                        showChangeCoverDialog = false
                                        Toast.makeText(context, "Color de carátula actualizado", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangeCoverDialog = false }) {
                    Text("Cerrar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Ajustes de Lectura y Voz
    if (showReadingSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showReadingSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajustes de Lectura y Voz", fontWeight = FontWeight.Bold, color = JtPrimaryText)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Tamaño del texto:", fontWeight = FontWeight.Medium, color = JtPrimaryText, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val sizes = listOf(0.85f to "Pequeño", 1.0f to "Normal", 1.2f to "Grande", 1.4f to "Extra")
                        sizes.forEach { (scale, label) ->
                            val isSel = (uiState.fontSizeScale - scale) in -0.05f..0.05f
                            Button(
                                onClick = { viewModel.setFontSizeScale(scale) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) JtGreenPrimary else JtBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else JtPrimaryText, fontSize = 11.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = JtBorder)

                    Text("Velocidad de lectura de voz:", fontWeight = FontWeight.Medium, color = JtPrimaryText, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val speeds = listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x", 2.0f to "2.0x")
                        speeds.forEach { (rate, label) ->
                            val isSel = (uiState.speechRate - rate) in -0.05f..0.05f
                            Button(
                                onClick = { viewModel.setSpeechRate(rate) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) JtGreenPrimary else JtBorder
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else JtPrimaryText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showReadingSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary)
                ) {
                    Text("Listo", color = Color.White)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Eliminar Libro
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar libro de la biblioteca?", fontWeight = FontWeight.Bold, color = JtPrimaryText) },
            text = {
                Text(
                    "Se eliminará \"${uiState.documentTitle}\" junto con todos sus marcadores, opiniones, notas y grabaciones de audio asociadas.",
                    color = JtSecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarDocumentoReciente(uiState.currentDocumentId)
                        showDeleteDialog = false
                        Toast.makeText(context, "Libro eliminado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Escribir / Editar Opinión personal
    fragmentForOpinion?.let { frag ->
        val existingNote = uiState.opinions.firstOrNull { it.fragmentId == frag.id }
        var opinionText by remember { mutableStateOf(existingNote?.opinion ?: "") }
        var selectedTag by remember { mutableStateOf(existingNote?.tag ?: "") }
        val tags = listOf("Jurisprudencia", "Artículo clave", "Doctrina", "Observación", "Excepción")

        AlertDialog(
            onDismissRequest = { fragmentForOpinion = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RateReview, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mi Opinión / Nota Jurídica", fontWeight = FontWeight.Bold, color = JtPrimaryText)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Página ${frag.pagina}: \"${frag.texto.take(70)}...\"",
                        color = JtSecondaryText,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    OutlinedTextField(
                        value = opinionText,
                        onValueChange = { opinionText = it },
                        placeholder = { Text("Escribe aquí tu análisis, interpretación o comentario sobre este párrafo...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Text("Clasificación (opcional):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = JtPrimaryText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.take(3).forEach { tag ->
                            val isSel = selectedTag == tag
                            Surface(
                                color = if (isSel) JtGreenPrimary else JtBorder,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    selectedTag = if (isSel) "" else tag
                                }
                            ) {
                                Text(
                                    tag,
                                    color = if (isSel) Color.White else JtSecondaryText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (opinionText.isNotBlank()) {
                            viewModel.guardarOpinion(frag.id, frag.pagina, frag.texto.take(100), opinionText.trim(), selectedTag)
                            fragmentForOpinion = null
                            Toast.makeText(context, "Opinión guardada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                    enabled = opinionText.isNotBlank()
                ) {
                    Text("Guardar Opinión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { fragmentForOpinion = null }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }
}

@Composable
fun ReaderFragmentItem(
    fragmento: FragmentoLectura,
    isCurrent: Boolean,
    isHighlighted: Boolean,
    opinion: FragmentOpinion?,
    audioComment: AudioComment?,
    isRecording: Boolean,
    isPlayingAudio: Boolean,
    searchQuery: String,
    fontSizeScale: Float,
    lineSpacing: Float = 1.5f,
    activeHighlightColor: String = "#FFF9C4",
    isUnderlineMode: Boolean = false,
    onClick: () -> Unit,
    onToggleHighlight: () -> Unit,
    onSelectColorAndHighlight: (String) -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit,
    onAddOpinion: () -> Unit,
    onDeleteOpinion: () -> Unit,
    onRecordAudioToggle: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit,
    onDeleteAudio: (String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val parsedHighlightColor = try {
        Color(android.graphics.Color.parseColor(activeHighlightColor))
    } catch (e: Exception) {
        Color(0xFFFFF9C4)
    }

    val normalBg = if (isCurrent) JtSurface else Color.Transparent
    val cardBg = if (isHighlighted) {
        if (isUnderlineMode) parsedHighlightColor.copy(alpha = 0.15f) else parsedHighlightColor
    } else normalBg

    val isSearchMatch = searchQuery.isNotBlank() && fragmento.texto.contains(searchQuery, ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = if (isSearchMatch) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(JtGold))
        } else if (isHighlighted) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(parsedHighlightColor.copy(alpha = 0.8f)))
        } else if (isCurrent) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(JtGreenPrimary))
        } else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header if highlighted
            if (isHighlighted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = parsedHighlightColor.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder)
                    ) {
                        Text(
                            if (isUnderlineMode) "✎ Subrayado jurídico" else "✎ Resaltado en color",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    TextButton(
                        onClick = { showColorPicker = !showColorPicker },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Cambiar color", fontSize = 11.sp, color = if (isHighlighted && !isUnderlineMode) Color(0xFF212121) else JtGreenPrimary)
                    }
                }
            }

            // Selector flotante de color de resaltado
            AnimatedVisibility(visible = showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(JtSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color:", fontSize = 11.sp, color = JtSecondaryText)
                    val palette = listOf(
                        "#FFF9C4" to Color(0xFFFFF9C4),
                        "#C8E6C9" to Color(0xFFC8E6C9),
                        "#BBDEFB" to Color(0xFFBBDEFB),
                        "#F8BBD0" to Color(0xFFF8BBD0),
                        "#FFE0B2" to Color(0xFFFFE0B2)
                    )
                    palette.forEach { (hex, col) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(1.dp, JtBorder, CircleShape)
                                .clickable {
                                    onSelectColorAndHighlight(hex)
                                    showColorPicker = false
                                }
                        )
                    }
                    TextButton(onClick = {
                        onToggleHighlight()
                        showColorPicker = false
                    }) {
                        Text("Quitar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SelectionContainer {
                Text(
                    text = fragmento.texto,
                    color = if (isHighlighted && !isUnderlineMode) Color(0xFF212121) else if (isCurrent) JtPrimaryText else JtSecondaryText,
                    fontSize = (17 * fontSizeScale).sp,
                    lineHeight = (26 * fontSizeScale * (lineSpacing / 1.5f)).sp,
                    fontFamily = FontFamily.Serif,
                    textDecoration = if (isHighlighted && isUnderlineMode) TextDecoration.Underline else TextDecoration.None
                )
            }

            // Toolbar de acciones por párrafo (Copiar, Compartir, Resaltar, Mi Opinión y Nota de Voz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Herramientas rápidas: Copiar y Compartir texto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopyText,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar texto",
                            tint = JtSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onShareText,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir fragmento",
                            tint = JtSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Acciones de anotación
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón resaltador
                    IconButton(
                        onClick = {
                            if (isHighlighted) {
                                showColorPicker = !showColorPicker
                            } else {
                                onToggleHighlight()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isHighlighted) Icons.Default.BorderColor else Icons.Outlined.BorderColor,
                            contentDescription = "Resaltar o cambiar color",
                            tint = if (isHighlighted) Color(0xFFE65100) else JtSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón nota escrita
                    IconButton(
                        onClick = onAddOpinion,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (opinion != null) Icons.Default.RateReview else Icons.Outlined.RateReview,
                            contentDescription = "Mi opinión",
                            tint = if (opinion != null) JtGreenPrimary else JtSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón nota de voz / micrófono
                    IconButton(
                        onClick = onRecordAudioToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else if (audioComment != null) Icons.Default.Mic else Icons.Outlined.Mic,
                            contentDescription = if (isRecording) "Detener grabación" else "Grabar nota de voz",
                            tint = if (isRecording) MaterialTheme.colorScheme.error else if (audioComment != null) JtGold else JtSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Indicador de grabación activa
            if (isRecording) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Grabando comentario de voz...",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onRecordAudioToggle, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Detener", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Tarjeta de comentario de audio reproducir / borrar
            if (audioComment != null && !isRecording) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF232B25),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtGreenPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPlayingAudio) onStopAudio() else onPlayAudio(audioComment.audioPath)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = if (isPlayingAudio) "Pausar" else "Reproducir",
                                    tint = JtGreenPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Nota de voz",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = JtPrimaryText
                                )
                                Text(
                                    "${audioComment.durationSeconds} seg",
                                    fontSize = 11.sp,
                                    color = JtSecondaryText
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteAudio(audioComment.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar nota de voz", tint = JtSecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Si hay una opinión registrada por el usuario, mostrar tarjeta de nota pegada
            if (opinion != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF2E2E20),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Comment, contentDescription = null, tint = JtGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mi Opinión / Nota:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JtGold)
                                if (opinion.tag.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = JtGreenPrimary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            opinion.tag,
                                            color = JtPrimaryText,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row {
                                IconButton(onClick = onAddOpinion, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = JtSecondaryText, modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = onDeleteOpinion, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = JtSecondaryText, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Text(
                            text = opinion.opinion,
                            fontSize = 13.sp,
                            color = JtPrimaryText,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderBottomControls(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onOpenQuestions: () -> Unit,
    onOpenIndex: () -> Unit
) {
    Surface(
        color = JtBackground,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val progress = if (uiState.fragmentosLectura.isNotEmpty()) {
                uiState.indiceActual.toFloat() / uiState.fragmentosLectura.size.toFloat()
            } else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = JtGreenPrimary,
                    trackColor = JtBorder
                )
                Spacer(modifier = Modifier.width(14.dp))
                val currentFrag = uiState.indiceActual + 1
                val totalFrag = if (uiState.fragmentosLectura.isEmpty()) 1 else uiState.fragmentosLectura.size
                Text("$currentFrag / $totalFrag", color = JtSecondaryText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de controles de reproducción y velocidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.fragmentoAnterior() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = JtGreenPrimary, modifier = Modifier.size(32.dp))
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(JtGreenPrimary)
                        .clickable {
                            if (uiState.lecturaActiva) viewModel.pausarManual() else viewModel.continuarLectura()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.lecturaActiva) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Reproducir/Pausar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.fragmentoSiguiente() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = JtGreenPrimary, modifier = Modifier.size(32.dp))
                }

                // Selector de velocidad dinámico
                TextButton(onClick = {
                    val nextRate = when (uiState.speechRate) {
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 0.75f
                    }
                    viewModel.setSpeechRate(nextRate)
                }) {
                    Text(
                        text = "${uiState.speechRate}x",
                        color = JtPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones Consulta Documental e Índice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenQuestions,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = JtPrimaryText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Consulta", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onOpenIndex,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = JtPrimaryText, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Índice", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun IndexAndBookmarksModal(
    uiState: JurisTechUiState,
    onDismiss: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onSelectFragment: (Int) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDeleteOpinion: (String) -> Unit,
    onEditOpinion: (FragmentOpinion) -> Unit,
    onSaveGeneralNotes: (String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onDeleteAudio: (String) -> Unit
) {
    var selectedIndexTab by remember { mutableStateOf(0) } // 0: Páginas, 1: Marcadores, 2: Anotaciones, 3: Historial
    var pageInputText by remember { mutableStateOf("") }
    var generalNotesText by remember { mutableStateOf(uiState.documentGeneralNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Contenido e Índice", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = JtPrimaryText)
                Spacer(modifier = Modifier.height(8.dp))
                ScrollableTabRow(
                    selectedTabIndex = selectedIndexTab,
                    containerColor = Color.Transparent,
                    contentColor = JtGreenPrimary,
                    edgePadding = 0.dp
                ) {
                    Tab(
                        selected = selectedIndexTab == 0,
                        onClick = { selectedIndexTab = 0 },
                        text = { Text("Páginas (${uiState.paginas.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedIndexTab == 1,
                        onClick = { selectedIndexTab = 1 },
                        text = { Text("Marcadores (${uiState.bookmarks.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedIndexTab == 2,
                        onClick = { selectedIndexTab = 2 },
                        text = { Text("Anotaciones (${uiState.opinions.size + uiState.audioComments.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedIndexTab == 3,
                        onClick = { selectedIndexTab = 3 },
                        text = { Text("Historial (${uiState.positionHistory.size})", fontSize = 12.sp) }
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                when (selectedIndexTab) {
                    0 -> {
                        // Lista de páginas + Salto exacto a página
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = pageInputText,
                                    onValueChange = { pageInputText = it.filter { char -> char.isDigit() } },
                                    placeholder = { Text("Pág. (1-${uiState.paginas.size})", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(
                                    onClick = {
                                        val p = pageInputText.toIntOrNull()
                                        if (p != null && p in 1..uiState.paginas.size) {
                                            onSelectPage(p)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                    enabled = pageInputText.isNotBlank()
                                ) {
                                    Text("Ir a página", fontSize = 12.sp)
                                }
                            }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.paginas.size) { index ->
                                    val pageNum = index + 1
                                    val pagePreview = uiState.paginas[index].texto.take(80)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectPage(pageNum) },
                                        colors = CardDefaults.cardColors(containerColor = JtBackground),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = JtGreenPrimary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "Pág. $pageNum",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "$pagePreview...",
                                                fontSize = 12.sp,
                                                color = JtSecondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Lista de marcadores funcionales
                        if (uiState.bookmarks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tienes marcadores guardados aún.\nToca el ícono de marcador en la barra superior del lector para guardar la posición.", textAlign = TextAlign.Center, color = JtSecondaryText, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.bookmarks) { bookmark ->
                                    val fragIndex = uiState.fragmentosLectura.indexOfFirst { it.id == bookmark.fragmentId }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (fragIndex >= 0) onSelectFragment(fragIndex) else onSelectPage(bookmark.pagina)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = JtBackground),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = JtGold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Página ${bookmark.pagina}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JtPrimaryText)
                                                Text(bookmark.snippet, fontSize = 12.sp, color = JtSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            IconButton(
                                                onClick = { onDeleteBookmark(bookmark.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Eliminar marcador", tint = JtSecondaryText, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Sección de Anotaciones: Notas generales + Notas de voz + Opiniones escritas
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Notas Generales del Documento
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = JtBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.EditNote, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Notas Generales del Documento", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JtPrimaryText)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = generalNotesText,
                                            onValueChange = { generalNotesText = it },
                                            placeholder = { Text("Escribe anotaciones generales, doctrina de referencia o resumen propio...", fontSize = 12.sp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { onSaveGeneralNotes(generalNotesText) },
                                            colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                            modifier = Modifier.align(Alignment.End),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Guardar notas", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Sección Notas de voz
                            if (uiState.audioComments.isNotEmpty()) {
                                item {
                                    Text("Notas de Voz (${uiState.audioComments.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JtPrimaryText, modifier = Modifier.padding(top = 4.dp))
                                }
                                items(uiState.audioComments) { audio ->
                                    val fragIndex = uiState.fragmentosLectura.indexOfFirst { it.id == audio.fragmentId }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { if (fragIndex >= 0) onSelectFragment(fragIndex) },
                                        colors = CardDefaults.cardColors(containerColor = JtBackground),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { onPlayAudio(audio.audioPath) }, modifier = Modifier.size(30.dp)) {
                                                    Icon(Icons.Default.PlayCircle, contentDescription = "Reproducir", tint = JtGreenPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("Pág. ${audio.pagina} - Nota de voz (${audio.durationSeconds}s)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JtPrimaryText)
                                                    Text("\"${audio.snippet}\"", fontSize = 11.sp, color = JtSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            IconButton(onClick = { onDeleteAudio(audio.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = JtSecondaryText, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Sección Opiniones y comentarios escritos
                            if (uiState.opinions.isNotEmpty()) {
                                item {
                                    Text("Comentarios por Párrafo (${uiState.opinions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JtPrimaryText, modifier = Modifier.padding(top = 4.dp))
                                }
                                items(uiState.opinions) { op ->
                                    val fragIndex = uiState.fragmentosLectura.indexOfFirst { it.id == op.fragmentId }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (fragIndex >= 0) onSelectFragment(fragIndex)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = JtBackground),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Pág. ${op.pagina} - Opinión", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JtGold)
                                                    if (op.tag.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = JtGreenPrimary.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                op.tag,
                                                                color = JtPrimaryText,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Row {
                                                    IconButton(onClick = { onEditOpinion(op) }, modifier = Modifier.size(22.dp)) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = JtSecondaryText, modifier = Modifier.size(14.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(onClick = { onDeleteOpinion(op.id) }, modifier = Modifier.size(22.dp)) {
                                                        Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = JtSecondaryText, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                            Text(op.opinion, fontSize = 13.sp, color = JtPrimaryText, modifier = Modifier.padding(vertical = 4.dp))
                                            Text("\"${op.textoFragmento}\"", fontSize = 11.sp, color = JtSecondaryText, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Historial de posiciones / saltos en el documento
                        if (uiState.positionHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Aún no hay historial de lectura registrado.\nA medida que leas y saltes en el documento, tus posiciones se registrarán aquí.", textAlign = TextAlign.Center, color = JtSecondaryText, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.positionHistory) { entry ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (entry.fragmentIndex in uiState.fragmentosLectura.indices) {
                                                    onSelectFragment(entry.fragmentIndex)
                                                } else {
                                                    onSelectPage(entry.pageNumber)
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = JtBackground),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.History, contentDescription = null, tint = JtGreenPrimary)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Pág. ${entry.pageNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JtPrimaryText)
                                                    val timeFormatted = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                                                    Text(timeFormatted, fontSize = 10.sp, color = JtSecondaryText)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(entry.snippet, fontSize = 11.sp, color = JtSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = JtSecondaryText)
            }
        },
        containerColor = JtSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onBack: () -> Unit
) {
    var mainTab by remember { mutableStateOf(0) } // 0: Consulta RAG, 1: Estudio, 2: IA Avanzada
    var studySubTab by remember { mutableStateOf(0) } // 0: Resumen, 1: Esquema, 2: Flashcards, 3: Examen, 4: Conceptos
    var advancedSubTab by remember { mutableStateOf(0) } // 0: Podcast, 1: Comparativa, 2: Dictamen

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var comparisonTopic by remember { mutableStateOf("Garantías y debido proceso") }
    var selectedComparisonDocId by remember {
        mutableStateOf(uiState.recentDocuments.firstOrNull { it.id != uiState.currentDocumentId }?.id ?: "")
    }

    val lastAiMessage = uiState.chatMessages.lastOrNull { !it.isUser && it.remitente == "JurisTech AI" && !it.texto.contains("cargado y guardado") }

    Scaffold(
        containerColor = JtBackground,
        topBar = {
            Column(modifier = Modifier.background(JtBackground)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Asistente & Estudio Jurídico", color = JtPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(uiState.documentTitle, color = JtSecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = JtBackground)
                )

                TabRow(
                    selectedTabIndex = mainTab,
                    containerColor = JtBackground,
                    contentColor = JtGreenPrimary
                ) {
                    Tab(
                        selected = mainTab == 0,
                        onClick = { mainTab = 0 },
                        text = { Text("Consulta RAG", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    )
                    Tab(
                        selected = mainTab == 1,
                        onClick = { mainTab = 1 },
                        text = { Text("Estudio", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    )
                    Tab(
                        selected = mainTab == 2,
                        onClick = { mainTab = 2 },
                        text = { Text("IA Avanzada", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (mainTab) {
                0 -> {
                    // TAB 0: CONSULTA RAG DOCUMENTAL
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        item {
                            Text(
                                "¿Qué deseas consultar?",
                                color = JtPrimaryText,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Búsqueda semántica sobre el texto íntegro con citas y salto directo a página.",
                                color = JtSecondaryText,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(JtSurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, JtBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text("¿Cuáles son los principios fundamentales o penas aplicables?", color = JtSecondaryText, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxSize(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = JtPrimaryText,
                                        unfocusedTextColor = JtPrimaryText
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (textInput.isNotBlank()) {
                                            viewModel.enviarPregunta(textInput)
                                            textInput = ""
                                            keyboardController?.hide()
                                        }
                                    })
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(38.dp)
                                        .background(if (textInput.isNotBlank()) JtGreenPrimary else JtSecondaryText.copy(alpha = 0.5f), CircleShape)
                                        .clickable(enabled = textInput.isNotBlank()) {
                                            viewModel.enviarPregunta(textInput)
                                            textInput = ""
                                            keyboardController?.hide()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        if (uiState.isAskingAi) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = JtGreenPrimary)
                                }
                            }
                        } else if (lastAiMessage != null) {
                            item {
                                Text(
                                    "Respuesta y Cita Textual",
                                    color = JtPrimaryText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = JtSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(JtBorder)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Article, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Fundamento legal encontrado",
                                                color = JtPrimaryText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = lastAiMessage.texto,
                                            color = JtSecondaryText,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp,
                                            fontFamily = FontFamily.Serif
                                        )
                                        Spacer(modifier = Modifier.height(18.dp))
                                        val pageRefText = if (lastAiMessage.referencedPages.isNotEmpty()) " (Pág. ${lastAiMessage.referencedPages.first()})" else ""
                                        OutlinedButton(
                                            onClick = {
                                                if (lastAiMessage.referencedFragmentIndex >= 0) {
                                                    viewModel.saltarAFragmento(lastAiMessage.referencedFragmentIndex)
                                                } else if (lastAiMessage.referencedPages.isNotEmpty()) {
                                                    viewModel.saltarAPagina(lastAiMessage.referencedPages.first())
                                                }
                                                onBack()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Ir exactamente al fragmento$pageRefText")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        item {
                            Text(
                                "Preguntas jurídicas frecuentes",
                                color = JtPrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val suggestions = listOf(
                                "¿Cuáles son los principios rectores consagrados en el Título Preliminar?",
                                "¿Qué se establece sobre el Principio de Legalidad y Lesividad?",
                                "¿Cuáles son las sanciones y tipos de penas aplicables?",
                                "¿Existe aplicación retroactiva de la ley más favorable?"
                            )

                            suggestions.forEach { q ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            textInput = q
                                            viewModel.enviarPregunta(q)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = JtSurface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(q, color = JtPrimaryText, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: HERRAMIENTAS DE ESTUDIO (Resumen, Esquema, Flashcards, Examen, Conceptos)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Subtabs selector
                        ScrollableTabRow(
                            selectedTabIndex = studySubTab,
                            containerColor = JtSurface,
                            contentColor = JtGreenPrimary,
                            edgePadding = 12.dp
                        ) {
                            listOf("Resumen", "Esquema", "Flashcards", "Modo Examen", "Conceptos Clave").forEachIndexed { index, title ->
                                Tab(
                                    selected = studySubTab == index,
                                    onClick = { studySubTab = index },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = if (studySubTab == index) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (studySubTab) {
                                0 -> {
                                    // 1. Resumen Ejecutivo
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Resumen Ejecutivo", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("SUMMARY") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.studySummary.isEmpty()) "Generar Resumen" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "SUMMARY") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.studySummary.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SelectionContainer {
                                                    Text(
                                                        text = uiState.studySummary,
                                                        color = JtPrimaryText,
                                                        fontSize = 14.sp,
                                                        lineHeight = 22.sp,
                                                        fontFamily = FontFamily.Serif,
                                                        modifier = Modifier.padding(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Pulsa 'Generar Resumen' para sintetizar los artículos y doctrinas clave del documento.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }

                                1 -> {
                                    // 2. Esquema Estructurado
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Esquema Estructurado", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("OUTLINE") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.studyOutline.isEmpty()) "Generar Esquema" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "OUTLINE") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.studyOutline.isNotEmpty()) {
                                        items(uiState.studyOutline) { line ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = line,
                                                    color = JtPrimaryText,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (line.startsWith("•") || line.contains(":")) FontWeight.Medium else FontWeight.Normal,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Crea un árbol temático jerárquico para estudiar la estructura de esta ley o código.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }

                                2 -> {
                                    // 3. Flashcards Interactivas
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Flashcards de Repaso", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("FLASHCARDS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.studyFlashcards.isEmpty()) "Crear Flashcards" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "FLASHCARDS") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.studyFlashcards.isNotEmpty()) {
                                        val card = uiState.studyFlashcards[uiState.flashcardCurrentIndex]
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (uiState.flashcardFlipped) Color(0xFF1B382B) else JtSurface
                                                ),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, JtGreenPrimary.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(220.dp)
                                                    .clickable { viewModel.voltearFlashcard() }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(20.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            if (uiState.flashcardFlipped) "RESPUESTA / FUNDAMENTO" else "PREGUNTA",
                                                            color = if (uiState.flashcardFlipped) JtGold else JtGreenPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            "${uiState.flashcardCurrentIndex + 1} de ${uiState.studyFlashcards.size}",
                                                            color = JtSecondaryText,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Text(
                                                        text = if (uiState.flashcardFlipped) card.respuesta else card.pregunta,
                                                        color = Color.White,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        lineHeight = 24.sp,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Text(
                                                        if (uiState.flashcardFlipped) "Ref: ${card.referenciaLegal} • Toca para voltear" else "Toca la tarjeta para ver la respuesta",
                                                        color = JtSecondaryText,
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.anteriorFlashcard() },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Anterior")
                                                }

                                                Button(
                                                    onClick = { viewModel.voltearFlashcard() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Voltear")
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.siguienteFlashcard() },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Siguiente")
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Genera tarjetas mnemotécnicas para memorizar conceptos y artículos cruciales.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }

                                3 -> {
                                    // 4. Modo Examen (Preguntas interactivas con A/B/C/D y evaluación)
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Simulador de Examen", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("EXAM") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.studyExamQuestions.isEmpty()) "Crear Examen" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "EXAM") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.studyExamQuestions.isNotEmpty()) {
                                        // Score banner
                                        if (uiState.examScore != null) {
                                            item {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = JtGreenPrimary.copy(alpha = 0.2f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, JtGreenPrimary)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Resultado Final", fontWeight = FontWeight.Bold, color = JtGreenPrimary, fontSize = 15.sp)
                                                            Text("${uiState.examScore?.first} de ${uiState.examScore?.second} correctas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JtPrimaryText)
                                                        }
                                                        Button(
                                                            onClick = { viewModel.reiniciarExamen() },
                                                            colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary)
                                                        ) {
                                                            Text("Reintentar", fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        items(uiState.studyExamQuestions) { question ->
                                            val selectedOpt = uiState.examUserAnswers[question.id]
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(question.pregunta, fontWeight = FontWeight.Bold, color = JtPrimaryText, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    question.opciones.forEachIndexed { optIdx, optText ->
                                                        val isSelected = selectedOpt == optIdx
                                                        val isCorrect = optIdx == question.indiceCorrecto
                                                        val optionBg = if (selectedOpt != null) {
                                                            if (isCorrect) Color(0xFF1B5E20) else if (isSelected) Color(0xFFB71C1C) else JtBackground
                                                        } else JtBackground

                                                        Surface(
                                                            color = optionBg,
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clickable(enabled = selectedOpt == null) {
                                                                    viewModel.responderPreguntaExamen(question.id, optIdx)
                                                                }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "${('A' + optIdx)}. ",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (selectedOpt != null && (isCorrect || isSelected)) Color.White else JtGreenPrimary
                                                                )
                                                                Text(
                                                                    text = optText,
                                                                    color = if (selectedOpt != null && (isCorrect || isSelected)) Color.White else JtPrimaryText,
                                                                    fontSize = 13.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (selectedOpt != null) {
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Surface(
                                                            color = JtBackground,
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(10.dp)) {
                                                                Text("Fundamento Jurídico:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = JtGold)
                                                                Text(question.explicacion, fontSize = 12.sp, color = JtSecondaryText)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Pon a prueba tus conocimientos con preguntas de opción múltiple auto-evaluadas.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }

                                4 -> {
                                    // 5. Conceptos Clave
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Conceptos Clave", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("CONCEPTS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.studyConcepts.isEmpty()) "Extraer Conceptos" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "CONCEPTS") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.studyConcepts.isNotEmpty()) {
                                        items(uiState.studyConcepts) { (term, def) ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(term, fontWeight = FontWeight.Bold, color = JtGold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(def, color = JtSecondaryText, fontSize = 13.sp, lineHeight = 20.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Extrae automáticamente términos legales y su significado según este texto.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: IA AVANZADA (Podcast Resumen, Comparativa entre 2 Leyes, Dictamen Formal)
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = advancedSubTab,
                            containerColor = JtSurface,
                            contentColor = JtGreenPrimary,
                            edgePadding = 12.dp
                        ) {
                            listOf("Podcast Resumen", "Comparativa de Leyes", "Dictamen Formal").forEachIndexed { index, title ->
                                Tab(
                                    selected = advancedSubTab == index,
                                    onClick = { advancedSubTab = index },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = if (advancedSubTab == index) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (advancedSubTab) {
                                0 -> {
                                    // 1. Podcast Resumen con Reproductor de Audio
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Podcast Resumen en Audio", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("PODCAST") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.podcastText.isEmpty()) "Generar Podcast" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "PODCAST") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.podcastText.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(18.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("JurisTech Audio Podcast", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = JtPrimaryText)
                                                            Text(if (uiState.isPodcastPlaying) "Reproduciendo con voz..." else "Listo para escuchar", fontSize = 12.sp, color = JtGreenPrimary)
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                if (uiState.isPodcastPlaying) {
                                                                    viewModel.detenerResumenPodcast()
                                                                } else {
                                                                    viewModel.reproducirResumenPodcast()
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .background(JtGreenPrimary, CircleShape)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (uiState.isPodcastPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                contentDescription = "Play/Pause Podcast",
                                                                tint = Color.White
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(14.dp))
                                                    Text("Guion narrativo:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = JtGold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(uiState.podcastText, color = JtSecondaryText, fontSize = 13.sp, lineHeight = 21.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Convierte el documento en un episodio explicativo con narración por voz sintetizada.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }

                                1 -> {
                                    // 2. Comparativa entre 2 leyes o códigos
                                    item {
                                        Text("Comparativa Multi-Documental", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Compara '${uiState.documentTitle}' con otra ley de tu biblioteca para detectar contradicciones y armonizaciones.", color = JtSecondaryText, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        val otherDocs = uiState.recentDocuments.filter { it.id != uiState.currentDocumentId }
                                        if (otherDocs.isEmpty()) {
                                            Text("No hay otros documentos en la biblioteca para comparar. Agrega otro libro o código primero.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                        } else {
                                            Text("Selecciona el segundo documento:", fontSize = 12.sp, color = JtPrimaryText, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            otherDocs.forEach { doc ->
                                                val isSelected = selectedComparisonDocId == doc.id
                                                Surface(
                                                    color = if (isSelected) JtGreenPrimary else JtSurface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clickable { selectedComparisonDocId = doc.id }
                                                ) {
                                                    Text(
                                                        doc.title,
                                                        color = if (isSelected) Color.White else JtPrimaryText,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedTextField(
                                                value = comparisonTopic,
                                                onValueChange = { comparisonTopic = it },
                                                label = { Text("Tema jurídico a confrontar") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Button(
                                                onClick = {
                                                    if (selectedComparisonDocId.isNotBlank()) {
                                                        viewModel.compararDocumentos(selectedComparisonDocId, comparisonTopic)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isComparingDocs && selectedComparisonDocId.isNotBlank(),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(if (uiState.isComparingDocs) "Comparando leyes..." else "Ejecutar Análisis Comparativo")
                                            }
                                        }
                                    }

                                    if (uiState.isComparingDocs) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.comparisonResult.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                SelectionContainer {
                                                    Text(
                                                        text = uiState.comparisonResult,
                                                        color = JtPrimaryText,
                                                        fontSize = 13.sp,
                                                        lineHeight = 21.sp,
                                                        fontFamily = FontFamily.Serif,
                                                        modifier = Modifier.padding(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    // 3. Dictamen / Informe Jurídico Formal
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Dictamen Jurídico Formal", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = JtPrimaryText)
                                            Button(
                                                onClick = { viewModel.generarHerramientaEstudio("REPORT") },
                                                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                                enabled = !uiState.isGeneratingStudyTool
                                            ) {
                                                Text(if (uiState.legalReportText.isEmpty()) "Emitir Dictamen" else "Regenerar", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    if (uiState.isGeneratingStudyTool && uiState.studyToolType == "REPORT") {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = JtGreenPrimary)
                                            }
                                        }
                                    } else if (uiState.legalReportText.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    SelectionContainer {
                                                        Text(
                                                            text = uiState.legalReportText,
                                                            color = JtPrimaryText,
                                                            fontSize = 13.sp,
                                                            lineHeight = 22.sp,
                                                            fontFamily = FontFamily.Serif
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        item {
                                            Text("Genera un informe técnico estructurado con antecedentes, fundamentos legales vigentes y conclusiones aplicables.", color = JtSecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

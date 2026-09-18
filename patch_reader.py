import sys

code = """package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel
import androidx.compose.foundation.lazy.rememberLazyListState

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
                recentDocuments = uiState.recentDocuments,
                onSelectDocument = { doc ->
                    viewModel.retomarDocumento(doc)
                    showQuestionsScreen = false
                },
                onAddDocument = {
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
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.indiceActual) {
        if (uiState.fragmentosLectura.isNotEmpty() && uiState.indiceActual in uiState.fragmentosLectura.indices) {
            listState.animateScrollToItem(uiState.indiceActual)
        }
    }

    Scaffold(
        containerColor = JtBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.documentTitle,
                        color = JtPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Marcadores", tint = JtPrimaryText)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = JtPrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JtBackground)
            )
        },
        bottomBar = {
            ReaderBottomControls(uiState = uiState, viewModel = viewModel, onOpenQuestions = onOpenQuestions)
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            items(uiState.fragmentosLectura.size) { index ->
                val fragmento = uiState.fragmentosLectura[index]
                val isCurrent = index == uiState.indiceActual
                
                Text(
                    text = fragmento.texto,
                    color = if (isCurrent) JtPrimaryText else JtSecondaryText,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.clickable { viewModel.saltarAFragmento(index) }
                )
            }
        }
    }
}

@Composable
fun ReaderBottomControls(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onOpenQuestions: () -> Unit
) {
    Surface(
        color = JtBackground,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
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
                Spacer(modifier = Modifier.width(16.dp))
                val currentFrag = uiState.indiceActual + 1
                val totalFrag = if (uiState.fragmentosLectura.isEmpty()) 1 else uiState.fragmentosLectura.size
                Text("$currentFrag / $totalFrag", color = JtSecondaryText, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                
                TextButton(onClick = {
                    val newRate = if (uiState.speechRate == 1.0f) 1.5f else if (uiState.speechRate == 1.5f) 2.0f else 1.0f
                    viewModel.setSpeechRate(newRate)
                }) {
                    Text("${uiState.speechRate}x", color = JtPrimaryText, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenQuestions,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = JtPrimaryText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulta documental")
                }
                
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = JtPrimaryText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Índice")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onBack: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    
    val lastAiMessage = uiState.chatMessages.lastOrNull { !it.isUser && it.remitente == "JurisTech AI" && !it.texto.contains("cargado y guardado") }
    
    Scaffold(
        containerColor = JtBackground,
        topBar = {
            TopAppBar(
                title = { Text("Consulta documental", color = JtPrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JtBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text(
                    "¿Qué deseas consultar?",
                    color = JtPrimaryText,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Haz una pregunta sobre el contenido de este documento. La respuesta se buscará directamente en el texto.",
                    color = JtSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Big text area for input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(JtSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, JtBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("¿Cuál es el plazo para presentar un recurso de apelación?", color = JtSecondaryText, fontSize = 14.sp) },
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
                    
                    // Send button inside text area at bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp)
                            .background(if (textInput.isNotBlank()) JtGreenPrimary else JtSecondaryText.copy(alpha=0.5f), CircleShape)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            if (uiState.isAskingAi) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JtGreenPrimary)
                    }
                }
            } else if (lastAiMessage != null) {
                item {
                    Text(
                        "Resultado",
                        color = JtPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JtSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(JtBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Article, contentDescription = null, tint = JtPrimaryText, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fragmento extraído",
                                    color = JtPrimaryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "“${lastAiMessage.texto}”",
                                color = JtSecondaryText,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = { onBack() }, // Navigates back to reader
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ver en documento")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            // Preguntas recientes mockup (since we don't have a recent questions DB yet, just styling it as requested)
            item {
                Text(
                    "Preguntas recientes",
                    color = JtPrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val recentMock = listOf(
                    "¿Quién puede interponer el recurso?",
                    "¿Qué delitos son imprescriptibles?",
                    "¿Qué dice sobre la reincidencia?"
                )
                
                recentMock.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = JtSecondaryText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(q, color = JtSecondaryText, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/JurisTechScreen.kt", "w") as f:
    f.write(code)

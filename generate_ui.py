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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.FragmentoLectura
import com.example.ui.theme.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun formatRelativeTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        diff < 0L -> "Ahora mismo"
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days == 1L -> "Ayer"
        days < 7 -> "Hace $days d"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
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

        BoxWithConstraints(modifier = modifier.fillMaxSize().background(JtBackground)) {
            val isWideScreen = maxWidth >= 840.dp
            
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(2f)) {
                        ReaderScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { viewModel.cerrarDocumento() },
                            onOpenQuestions = { showQuestionsScreen = true },
                            isWideScreen = true
                        )
                    }
                    VerticalDivider(color = JtBorder)
                    Box(modifier = Modifier.weight(1.2f)) {
                        QuestionsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { showQuestionsScreen = false },
                            isWideScreen = true
                        )
                    }
                }
            } else {
                AnimatedContent(targetState = showQuestionsScreen, label = "ScreenTransition") { showQ ->
                    if (showQ) {
                        QuestionsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { showQuestionsScreen = false },
                            isWideScreen = false
                        )
                    } else {
                        ReaderScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { viewModel.cerrarDocumento() },
                            onOpenQuestions = { showQuestionsScreen = true },
                            isWideScreen = false
                        )
                    }
                }
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
    onOpenQuestions: () -> Unit,
    isWideScreen: Boolean
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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.documentTitle,
                            color = JtPrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val totalPages = if (uiState.paginas.isNotEmpty()) uiState.paginas.size else 1
                        val currentPage = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 1
                        Text(
                            text = "Página \$currentPage de \$totalPages",
                            color = JtSecondaryText,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    if (!isWideScreen) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQuestions) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = "Consultar", tint = JtPrimaryText)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Marcadores", tint = JtPrimaryText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = JtBackground)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.fragmentosLectura.size) { index ->
                val fragmento = uiState.fragmentosLectura[index]
                val isCurrent = index == uiState.indiceActual
                
                Text(
                    text = fragmento.texto,
                    color = if (isCurrent) JtPrimaryText else JtSecondaryText,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
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
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                Text("\$currentFrag / \$totalFrag", color = JtSecondaryText, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.fragmentoAnterior() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = JtGreenPrimary, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                IconButton(
                    onClick = { viewModel.fragmentoSiguiente() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = JtGreenPrimary, modifier = Modifier.size(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val newRate = if (uiState.speechRate == 1.0f) 1.5f else if (uiState.speechRate == 1.5f) 2.0f else 1.0f
                    viewModel.setSpeechRate(newRate)
                }) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = JtSecondaryText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("\${uiState.speechRate}x Velocidad", color = JtSecondaryText)
                }
                
                TextButton(onClick = onOpenQuestions) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = JtSecondaryText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Consultar", color = JtSecondaryText)
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
    onBack: () -> Unit,
    isWideScreen: Boolean
) {
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }
    
    Scaffold(
        containerColor = JtBackground,
        topBar = {
            TopAppBar(
                title = { Text("Consulta documental", color = JtPrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isWideScreen) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = JtPrimaryText)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JtBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header instruction
                item {
                    Text(
                        "Pregunta sobre este documento",
                        color = JtPrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Obtén respuestas directamente del contenido.",
                        color = JtSecondaryText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Show chat messages but styled as documentary results
                items(uiState.chatMessages.filter { it.remitente != "JurisTech AI" || !it.texto.contains("cargado y guardado") }) { msg ->
                    if (msg.remitente == "Usuario") {
                        // User question
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(JtBorder)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = msg.texto,
                                    color = JtPrimaryText,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // System answer
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Resultado",
                                color = JtPrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = JtSurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Article, contentDescription = null, tint = JtGold, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Información extraída",
                                            color = JtPrimaryText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = msg.texto,
                                        color = JtSecondaryText,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (uiState.isAskingAi) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = JtGreenPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            
            // Input area
            Surface(
                color = JtSurface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("¿Qué deseas consultar?", color = JtSecondaryText) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = JtBackground,
                            unfocusedContainerColor = JtBackground,
                            focusedBorderColor = JtGreenPrimary,
                            unfocusedBorderColor = JtBorder,
                            focusedTextColor = JtPrimaryText,
                            unfocusedTextColor = JtPrimaryText
                        ),
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.enviarPregunta(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (textInput.isNotBlank()) JtGreenPrimary else JtBorder)
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
                            tint = if (textInput.isNotBlank()) Color.White else JtSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/JurisTechScreen.kt", "w") as f:
    f.write(code)

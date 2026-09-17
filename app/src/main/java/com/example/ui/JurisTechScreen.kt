package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.FragmentoLectura
import com.example.model.SampleDocumentRepository
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.EmeraldBadgeBg
import com.example.ui.theme.Indigo300
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo700
import com.example.ui.theme.Indigo900
import com.example.ui.theme.IndigoHighlight
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import android.provider.OpenableColumns
import androidx.compose.material.icons.filled.Close
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.data.local.RecentDocumentEntity
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Rose300 = Color(0xFFFDA4AF)

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
    var showLoadDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // File picker launcher para PDFs y archivos de texto plano
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: uri.lastPathSegment ?: "Documento.pdf"
            viewModel.procesarArchivoPdf(uri, fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate900,
        topBar = {
            HeaderBar(
                uiState = uiState,
                onOpenLoadDialog = { showLoadDialog = true },
                onClearAll = { showClearConfirmDialog = true }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 840.dp

            if (isWideScreen) {
                // Layout de pantalla ancha / tablet
                WideScreenLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onPickPdfDirectly = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*"))
                    },
                    onOpenLoadDialog = { showLoadDialog = true }
                )
            } else {
                // Layout móvil adaptativo con acceso rápido a PDF
                MobileScreenLayout(
                    uiState = uiState,
                    viewModel = viewModel,
                    onPickPdfDirectly = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*"))
                    },
                    onOpenLoadDialog = { showLoadDialog = true }
                )
            }
        }
    }

    // Diálogo de confirmación para vaciar toda la información
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Vaciar toda la información",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar todos los datos y dejar la aplicación en blanco? Podrás ingresar tu nuevo documento desde cero.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate300)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.limpiarTodaLaInformacion()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Eliminar todo", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                ) {
                    Text("Cancelar", color = Slate200)
                }
            },
            containerColor = Slate800,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showLoadDialog) {
        LoadDocumentDialog(
            recentDocuments = uiState.recentDocuments,
            currentDocumentId = uiState.currentDocumentId,
            onDismiss = { showLoadDialog = false },
            onSelectSample = { sample ->
                viewModel.cargarSample(sample)
                showLoadDialog = false
            },
            onSelectRecent = { entity, resumePlay ->
                viewModel.retomarDocumento(entity)
                if (resumePlay) {
                    viewModel.iniciarLectura()
                }
                showLoadDialog = false
            },
            onDeleteRecent = { id ->
                viewModel.eliminarDocumentoReciente(id)
            },
            onImportText = { title, text ->
                viewModel.procesarTextoImportado(title, text)
                showLoadDialog = false
            },
            onPickFile = {
                showLoadDialog = false
                filePickerLauncher.launch(arrayOf("application/pdf", "text/plain", "*/*"))
            }
        )
    }
}

@Composable
fun HeaderBar(
    uiState: JurisTechUiState,
    onOpenLoadDialog: () -> Unit,
    onClearAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate800)
            .border(width = 1.dp, color = Slate700)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Indigo600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📖", fontSize = 20.sp)
                }

                Column {
                    Text(
                        text = "JurisTech Reader AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    )
                    Text(
                        text = "Lector inteligente de documentos",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Badge de estado "● Documento cargado"
                if (uiState.paginas.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .testTag("status_badge")
                            .clip(CircleShape)
                            .background(EmeraldBadgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "● Documento cargado",
                            color = Emerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botón para vaciar todo y dejar en blanco
                if (uiState.paginas.isNotEmpty() || uiState.recentDocuments.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("btn_clear_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Vaciar toda la información",
                            tint = Slate400
                        )
                    }
                }

                IconButton(
                    onClick = onOpenLoadDialog,
                    modifier = Modifier.testTag("btn_open_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Cargar o cambiar documento",
                        tint = Indigo400
                    )
                }
            }
        }
    }
}

@Composable
fun WideScreenLayout(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onPickPdfDirectly: () -> Unit,
    onOpenLoadDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Columna Izquierda (1/3)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UploadDocumentCard(
                uiState = uiState,
                onPickPdfDirectly = onPickPdfDirectly,
                onOpenLoadDialog = onOpenLoadDialog,
                onDismissError = { viewModel.limpiarError() },
                onSelectRecent = { entity -> viewModel.retomarDocumento(entity) }
            )

            VoiceReaderCard(
                uiState = uiState,
                viewModel = viewModel
            )

            PositionCard(uiState = uiState)
        }

        // Columna Derecha (2/3)
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1.2f)) {
                DocumentViewerCard(
                    uiState = uiState,
                    onFragmentClick = { index -> viewModel.saltarAFragmento(index) },
                    onPickPdfDirectly = onPickPdfDirectly
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ChatCard(
                    uiState = uiState,
                    onSendMessage = { pregunta -> viewModel.enviarPregunta(pregunta) },
                    onClearChat = { viewModel.limpiarChat() },
                    onJumpToFragment = { index -> viewModel.saltarAFragmento(index) }
                )
            }
        }
    }
}

@Composable
fun MobileScreenLayout(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onPickPdfDirectly: () -> Unit,
    onOpenLoadDialog: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Tab row para alternar vistas en pantalla móvil
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate800,
            contentColor = Indigo400,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Indigo500
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("📖 Documento") },
                selectedContentColor = Indigo400,
                unselectedContentColor = Slate400
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬 Preguntas")
                        if (uiState.isAskingAi) {
                            Spacer(Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Indigo400,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                selectedContentColor = Indigo400,
                unselectedContentColor = Slate400
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("🔊 Lector & Carga") },
                selectedContentColor = Indigo400,
                unselectedContentColor = Slate400
            )
        }

        // Contenido según la pestaña seleccionada
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    DocumentViewerCard(
                        uiState = uiState,
                        onFragmentClick = { index -> viewModel.saltarAFragmento(index) },
                        onPickPdfDirectly = onPickPdfDirectly
                    )
                }
                1 -> {
                    ChatCard(
                        uiState = uiState,
                        onSendMessage = { pregunta -> viewModel.enviarPregunta(pregunta) },
                        onClearChat = { viewModel.limpiarChat() },
                        onJumpToFragment = { index -> viewModel.saltarAFragmento(index) }
                    )
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            UploadDocumentCard(
                                uiState = uiState,
                                onPickPdfDirectly = onPickPdfDirectly,
                                onOpenLoadDialog = onOpenLoadDialog,
                                onDismissError = { viewModel.limpiarError() },
                                onSelectRecent = { entity -> viewModel.retomarDocumento(entity) }
                            )
                        }
                        item {
                            VoiceReaderCard(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                        item {
                            PositionCard(uiState = uiState)
                        }
                    }
                }
            }
        }

        // Barra inferior de control de audio permanente
        PersistentAudioBottomBar(
            uiState = uiState,
            viewModel = viewModel,
            onGoToChat = { selectedTab = 1 }
        )
    }
}

@Composable
fun UploadDocumentCard(
    uiState: JurisTechUiState,
    onPickPdfDirectly: () -> Unit,
    onOpenLoadDialog: () -> Unit,
    onDismissError: () -> Unit,
    onSelectRecent: (RecentDocumentEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄 Cargar documento",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Indigo400
                    )
                )
                if (uiState.recentDocuments.isNotEmpty()) {
                    Text(
                        text = "Recientes (${uiState.recentDocuments.size})",
                        color = Indigo400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenLoadDialog() }
                            .padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecciona un libro o documento PDF.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate400,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Caja con borde punteado/destacado para seleccionar archivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate850)
                    .border(
                        width = 1.5.dp,
                        color = if (uiState.paginas.isNotEmpty()) Indigo500 else Slate600,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onPickPdfDirectly() }
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📚", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.fileNameLabel.isNotBlank()) uiState.fileNameLabel else "Seleccionar PDF",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate200
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.paginas.isNotEmpty()) "${uiState.paginas.size} páginas cargadas · Toca para cambiar" else "Toca aquí para seleccionar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Barra de progreso animada al cargar/procesar PDF
            AnimatedVisibility(visible = uiState.isProcessingPdf || (uiState.pdfProgressPercent in 1..99)) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.pdfProgressText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate300,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "${uiState.pdfProgressPercent}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Indigo400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.pdfProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Indigo500,
                        trackColor = Slate700
                    )
                }
            }

            // Mensaje de Error
            AnimatedVisibility(visible = uiState.pdfErrorMessage != null) {
                uiState.pdfErrorMessage?.let { errorMsg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Rose500.copy(alpha = 0.12f))
                            .border(width = 1.dp, color = Rose500.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMsg,
                                color = Rose300,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar error",
                                    tint = Rose300,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Opciones adicionales (Pegar texto plano o ver ejemplos)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Pegar texto o usar ejemplos",
                    color = Indigo400,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { onOpenLoadDialog() }
                        .padding(vertical = 4.dp)
                )
            }

            // Sección de Documentos Recientes con Room
            if (uiState.recentDocuments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕒 Retomar recientes guardados",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Slate300,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Explorar",
                        color = Indigo400,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onOpenLoadDialog() }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.recentDocuments.take(3).forEach { doc ->
                        val isCurrent = doc.id == uiState.currentDocumentId
                        val percent = if (doc.totalFragments > 0) {
                            ((doc.lastFragmentIndex + 1).toFloat() / doc.totalFragments * 100).toInt().coerceIn(0, 100)
                        } else 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Slate900 else Slate850)
                                .border(
                                    width = 1.dp,
                                    color = if (isCurrent) Indigo500 else Slate700,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectRecent(doc) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCurrent) {
                                            Text(
                                                text = "● ACTUAL  ",
                                                color = Emerald400,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isCurrent) Indigo300 else Slate200,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Pág. ${doc.lastPage}/${doc.totalPages} ($percent%) • ${formatRelativeTimestamp(doc.lastAccessedTimestamp)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate400,
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onSelectRecent(doc) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrent) Indigo600 else Slate700
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Activo" else "Retomar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceReaderCard(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔊 Lector de voz",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald400
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "La lectura continúa automáticamente hasta que pulses \"Pregunta\" o \"Pausar\".",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate400,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Botones Leer y Pausar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.iniciarLectura() },
                    enabled = uiState.fragmentosLectura.isNotEmpty() && !uiState.lecturaActiva,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        disabledContainerColor = Emerald600.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_play")
                ) {
                    Text(
                        text = "▶ Leer",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { viewModel.pausarManual() },
                    enabled = uiState.lecturaActiva,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Rose600,
                        disabledContainerColor = Rose600.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_pause")
                ) {
                    Text(
                        text = "⏸ Pausar",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón Pregunta
            Button(
                onClick = { viewModel.activarPregunta() },
                enabled = uiState.fragmentosLectura.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo600,
                    disabledContainerColor = Indigo600.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_pregunta")
            ) {
                Text(
                    text = "❓ PREGUNTA",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Botones Anterior / Siguiente fragmento
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.fragmentoAnterior() },
                    enabled = uiState.indiceActual > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate700,
                        disabledContainerColor = Slate850
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("⏮ Anterior", fontSize = 12.sp, color = if (uiState.indiceActual > 0) Slate200 else Slate500)
                }

                Button(
                    onClick = { viewModel.fragmentoSiguiente() },
                    enabled = uiState.fragmentosLectura.isNotEmpty() && uiState.indiceActual < uiState.fragmentosLectura.size - 1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate700,
                        disabledContainerColor = Slate850
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("Siguiente ⏭", fontSize = 12.sp, color = if (uiState.fragmentosLectura.isNotEmpty() && uiState.indiceActual < uiState.fragmentosLectura.size - 1) Slate200 else Slate500)
                }
            }

            // Botón Continuar (visible cuando se pausa la lectura)
            AnimatedVisibility(visible = uiState.showContinueButton) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.continuarLectura() },
                        enabled = uiState.isContinueEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald600,
                            disabledContainerColor = Emerald600.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_continuar")
                    ) {
                        Text(
                            text = "▶ CONTINUAR",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estado de voz
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.voiceStatus,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (uiState.lecturaActiva) Emerald400 else Slate400,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de velocidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Velocidad:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.sp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.8f, 0.95f, 1.1f, 1.25f).forEach { rate ->
                        val isSelected = kotlin.math.abs(uiState.speechRate - rate) < 0.05f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Indigo600 else Slate700)
                                .clickable { viewModel.setSpeechRate(rate) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${rate}x",
                                color = if (isSelected) Color.White else Slate300,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionCard(uiState: JurisTechUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 Posición de lectura",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Slate300
                    )
                )

                if (uiState.paginas.isNotEmpty()) {
                    val pagActual = uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 1
                    Text(
                        text = "Página $pagActual de ${uiState.paginas.size}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Indigo400, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.paginas.isEmpty()) {
                Text(
                    text = "Sin documento",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 12.sp)
                )
            } else {
                val currentFrag = uiState.indiceActual + 1
                val totalFrags = uiState.fragmentosLectura.size
                Text(
                    text = "Página ${uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 0} · Fragmento $currentFrag de $totalFrags",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val progress = if (totalFrags > 0) (currentFrag.toFloat() / totalFrags.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Indigo500,
                    trackColor = Slate700
                )
            }
        }
    }
}

@Composable
fun DocumentViewerCard(
    uiState: JurisTechUiState,
    onFragmentClick: (Int) -> Unit,
    onPickPdfDirectly: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Auto-scroll para seguir el fragmento activo
    LaunchedEffect(uiState.indiceActual) {
        if (uiState.indiceActual in uiState.fragmentosLectura.indices) {
            listState.animateScrollToItem(index = uiState.indiceActual)
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📖 Documento",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate200
                        )
                    )
                    if (uiState.lecturaActiva) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Indigo600)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LEYENDO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (uiState.paginas.isNotEmpty()) {
                    Text(
                        text = "${uiState.paginas.size} páginas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (uiState.fragmentosLectura.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Carga un documento PDF para comenzar.",
                                color = Slate500,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onPickPdfDirectly,
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Seleccionar PDF", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.fragmentosLectura) { fragmento ->
                            val isActive = fragmento.id == uiState.indiceActual
                            val isFirstOfPage = fragmento.id == 0 ||
                                (uiState.fragmentosLectura.getOrNull(fragmento.id - 1)?.pagina != fragmento.pagina)

                            Column {
                                if (isFirstOfPage) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate800)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "PÁGINA ${fragmento.pagina}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Indigo400
                                        )
                                    }
                                }

                                FragmentItem(
                                    fragmento = fragmento,
                                    isActive = isActive,
                                    onClick = { onFragmentClick(fragmento.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FragmentItem(
    fragmento: FragmentoLectura,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) IndigoHighlight else Color.Transparent,
        label = "fragBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Indicador lateral idéntico a .lectura-activa de CSS (border-left 3px solid #818cf8)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(if (isActive) Indigo400 else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = fragmento.texto,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isActive) Color.White else Slate300,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

@Composable
fun ChatCard(
    uiState: JurisTechUiState,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit = {},
    onJumpToFragment: (Int) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Sugerencias inteligentes de preguntas de seguimiento según el documento y contexto
    val quickFollowUps = remember(uiState.documentTitle, uiState.chatMessages.size) {
        if (uiState.chatMessages.size <= 1) {
            listOf(
                "¿Cuál es el objeto principal de este contrato?",
                "¿Qué obligaciones y penalizaciones se establecen?",
                "¿Qué ley y jurisdicción aplican?",
                "Resume los puntos clave de todo el documento"
            )
        } else {
            listOf(
                "¿En qué página o cláusula se especifica eso?",
                "¿Qué excepciones o limitaciones existen?",
                "¿Quién asume la responsabilidad si se incumple?",
                "¿Cuál es el plazo o vigencia exacta?"
            )
        }
    }

    // Auto-scroll al recibir o enviar mensaje
    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate700))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💬 Preguntas sobre el documento",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Indigo400
                        )
                    )
                    if (uiState.paginas.isNotEmpty()) {
                        // Badge indicativo de contexto completo y seguimiento
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Indigo900.copy(alpha = 0.6f))
                                .border(width = 1.dp, color = Indigo600.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Doc. Completo (${uiState.paginas.size} pág)",
                                color = Indigo300,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isAskingAi) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Indigo400,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analizando...",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (uiState.chatMessages.size > 1) {
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Reiniciar historial de chat",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de mensajes del chat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (uiState.chatMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pulsa \"PREGUNTA\" durante la lectura para hacer una consulta.",
                            color = Slate500,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.chatMessages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                onJumpToPage = { targetPage ->
                                    val targetIndex = uiState.fragmentosLectura.indexOfFirst { it.pagina == targetPage }
                                    if (targetIndex >= 0) {
                                        onJumpToFragment(targetIndex)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Barra horizontal de sugerencias / follow-ups rápidos
            if (!uiState.isAskingAi) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sugerencias:",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    quickFollowUps.take(2).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate850)
                                .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    onSendMessage(suggestion)
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = Indigo300,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input y botón de enviar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text("Escribe tu pregunta...", color = Slate500, fontSize = 12.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate900,
                        unfocusedContainerColor = Slate900,
                        focusedBorderColor = Indigo500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !uiState.isAskingAi) {
                                onSendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Button(
                    onClick = {
                        if (inputText.isNotBlank() && !uiState.isAskingAi) {
                            onSendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = inputText.isNotBlank() && !uiState.isAskingAi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600,
                        disabledContainerColor = Indigo600.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_send")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onJumpToPage: (Int) -> Unit = {}
) {
    val isUser = message.isUser || message.remitente == "Tú"
    val isSystem = message.remitente == "Sistema"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .border(width = 1.dp, color = Slate700, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.texto,
                    color = Indigo300,
                    fontSize = 11.sp
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) Indigo600 else Slate800)
                    .border(
                        width = 1.dp,
                        color = if (isUser) Indigo500 else Slate700,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUser) "Tú" else "JurisTech AI (Gemini)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color.White.copy(alpha = 0.9f) else Indigo400
                        )

                        if (!isUser) {
                            Text(
                                text = "Contexto Global",
                                fontSize = 9.sp,
                                color = Emerald400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message.texto,
                        color = Slate100,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PersistentAudioBottomBar(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onGoToChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate800)
            .border(width = 1.dp, color = Slate700)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.voiceStatus,
                    color = if (uiState.lecturaActiva) Emerald400 else Slate300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Pág ${uiState.fragmentosLectura.getOrNull(uiState.indiceActual)?.pagina ?: 0} (${uiState.indiceActual + 1}/${uiState.fragmentosLectura.size})",
                    color = Slate500,
                    fontSize = 10.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.showContinueButton) {
                    Button(
                        onClick = { viewModel.continuarLectura() },
                        enabled = uiState.isContinueEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("▶ Continuar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.activarPregunta(); onGoToChat() },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("❓ Preguntar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.lecturaActiva) {
                    IconButton(
                        onClick = { viewModel.pausarManual() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Rose600)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pausar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.iniciarLectura() },
                        enabled = uiState.fragmentosLectura.isNotEmpty(),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Emerald600)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Leer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadDocumentDialog(
    recentDocuments: List<RecentDocumentEntity>,
    currentDocumentId: String,
    onDismiss: () -> Unit,
    onSelectSample: (com.example.model.SampleDocument) -> Unit,
    onSelectRecent: (RecentDocumentEntity, resumePlay: Boolean) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onImportText: (String, String) -> Unit,
    onPickFile: () -> Unit
) {
    var selectedDialogTab by remember {
        mutableIntStateOf(if (recentDocuments.isNotEmpty()) 0 else 2)
    }
    var pasteTitle by remember { mutableStateOf("") }
    var pasteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text("📄 Cargar o Retomar Documento", color = Slate100, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedDialogTab,
                    containerColor = Slate850,
                    contentColor = Indigo400
                ) {
                    Tab(
                        selected = selectedDialogTab == 0,
                        onClick = { selectedDialogTab = 0 },
                        text = { Text("🕒 Recientes (${recentDocuments.size})", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedDialogTab == 1,
                        onClick = { selectedDialogTab = 1 },
                        text = { Text("Pegar", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedDialogTab == 2,
                        onClick = { selectedDialogTab = 2 },
                        text = { Text("Archivo", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    when (selectedDialogTab) {
                        0 -> {
                            if (recentDocuments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📂", fontSize = 36.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Sin documentos recientes",
                                            fontWeight = FontWeight.Bold,
                                            color = Slate200,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Todos los documentos que abras se guardarán automáticamente en tu base de datos Room para que puedas retomar la lectura o consulta cuando desees.",
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(recentDocuments, key = { it.id }) { doc ->
                                        val isCurrent = doc.id == currentDocumentId
                                        val progressFraction = if (doc.totalFragments > 0) {
                                            ((doc.lastFragmentIndex + 1).toFloat() / doc.totalFragments).coerceIn(0f, 1f)
                                        } else 0f
                                        val percentInt = (progressFraction * 100).toInt()

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCurrent) Slate900 else Slate850
                                            ),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(
                                                    if (isCurrent) Indigo500 else Slate700
                                                )
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        if (isCurrent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Emerald600.copy(alpha = 0.25f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    "ACTUAL",
                                                                    color = Emerald400,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                        }
                                                        Text(
                                                            text = doc.title,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCurrent) Indigo300 else Slate100,
                                                            fontSize = 13.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { onDeleteRecent(doc.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar de recientes",
                                                            tint = Slate500,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                if (doc.subtitle.isNotBlank()) {
                                                    Text(
                                                        text = doc.subtitle,
                                                        color = Slate400,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Barra de progreso de lectura Room
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Página ${doc.lastPage} de ${doc.totalPages} (fragmento ${doc.lastFragmentIndex + 1}/${doc.totalFragments})",
                                                        color = Emerald400,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = "$percentInt% • ${formatRelativeTimestamp(doc.lastAccessedTimestamp)}",
                                                        color = Slate400,
                                                        fontSize = 10.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { progressFraction },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = if (isCurrent) Indigo400 else Emerald500,
                                                    trackColor = Slate700
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onSelectRecent(doc, true) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Retomar lectura", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = { onSelectRecent(doc, false) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.QuestionAnswer,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = Indigo300
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Consultar", fontSize = 11.sp, color = Indigo300, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pasteTitle,
                                    onValueChange = { pasteTitle = it },
                                    placeholder = { Text("Título del documento", color = Slate500, fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900,
                                        focusedTextColor = Slate100,
                                        unfocusedTextColor = Slate100
                                    )
                                )
                                OutlinedTextField(
                                    value = pasteText,
                                    onValueChange = { pasteText = it },
                                    placeholder = { Text("Pega aquí el contenido del texto jurídico o contrato...", color = Slate500, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Slate900,
                                        unfocusedContainerColor = Slate900,
                                        focusedTextColor = Slate100,
                                        unfocusedTextColor = Slate100
                                    )
                                )
                                Button(
                                    onClick = {
                                        val title = if (pasteTitle.isNotBlank()) pasteTitle else "Documento pegado"
                                        onImportText(title, pasteText)
                                    },
                                    enabled = pasteText.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cargar texto legal")
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Abre cualquier documento de texto (.txt) guardado en tu dispositivo:",
                                    color = Slate300,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onPickFile,
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Explorar archivos del dispositivo")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Slate400)
            }
        }
    )
}

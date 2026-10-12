package com.example.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.tts.ActiveEngineType
import com.example.tts.TtsMode
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel

/**
 * Barra de control de modo de vista del lector:
 * Estructurada / Página Original / Lado a Lado.
 */
@Composable
fun ReaderViewModeSelector(
    currentMode: ReaderViewMode,
    onSelectMode: (ReaderViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderViewMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        val icon = when (mode) {
                            ReaderViewMode.STRUCTURED -> Icons.AutoMirrored.Filled.FormatListBulleted
                            ReaderViewMode.ORIGINAL_PAGE -> Icons.Default.PictureAsPdf
                            ReaderViewMode.SPLIT_DUAL -> Icons.Default.VerticalSplit
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

/**
 * Barra de control de voz dual: Online Natural vs Local Offline + Modo Automático.
 */
@Composable
fun TtsVoiceModeBar(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onOpenVoiceSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Indicador del motor activo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isOnline = uiState.activeTtsEngineType == ActiveEngineType.ONLINE
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.activeTtsEngineType.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF2E7D32) else Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Badge de conexión
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.isNetworkAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = if (uiState.isNetworkAvailable) "En línea" else "Sin conexión",
                                    fontSize = 10.sp,
                                    color = if (uiState.isNetworkAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when (uiState.ttsMode) {
                                TtsMode.AUTO -> "Modo: Automático (conmutación inteligente)"
                                TtsMode.ONLINE_NATURAL -> "Modo: Voz Natural Online"
                                TtsMode.LOCAL_OFFLINE -> "Modo: Voz Local Offline (Cero internet)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botón para cambiar modo de voz y velocidad
                IconButton(onClick = onOpenVoiceSettings) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Ajustes de Voz y Modo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Controles de reproducción rápida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.retrocederFragmentoAnterior() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Párrafo anterior",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.repetirFragmentoActual() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Repetir párrafo",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.avanzarSiguienteFragmento() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente párrafo",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botón central Reproducir / Pausar
                Button(
                    onClick = {
                        if (uiState.lecturaActiva) {
                            viewModel.pausarManual()
                        } else {
                            viewModel.iniciarLectura()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.lecturaActiva) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.lecturaActiva) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.lecturaActiva) "Pausar" else "Escuchar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.lecturaActiva) "Pausar" else "Escuchar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de configuración completa de voz:
 * Selector de Modo (Automático / Natural Online / Local Offline),
 * velocidad de habla, selección de voces disponibles e instrucciones para voz offline si faltan.
 */
@Composable
fun TtsSettingsDialog(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SpatialAudio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuración de Voz")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Modo de Síntesis de Voz",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                TtsMode.values().forEach { mode ->
                    val isSelected = uiState.ttsMode == mode
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.setTtsMode(mode) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setTtsMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Velocidad de Lectura: ${String.format("%.2fx", uiState.speechRate)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = uiState.speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.7f..2.0f,
                    steps = 12
                )

                Spacer(modifier = Modifier.height(10.dp))
                if (uiState.availableTtsVoices.isNotEmpty()) {
                    Text(
                        text = "Voz Activa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    uiState.availableTtsVoices.take(6).forEach { voiceName ->
                        val isSelected = uiState.activeTtsVoice == voiceName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setVoiceByName(voiceName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setVoiceByName(voiceName) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = voiceName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Elementos Estructurales en Lectura",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val currentConfig = uiState.readingAnnouncements

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anunciar títulos y capítulos", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = currentConfig.announceHeaders,
                        onCheckedChange = {
                            viewModel.actualizarConfiguracionLectura(currentConfig.copy(announceHeaders = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lectura accesible de tablas", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = currentConfig.announceTables,
                        onCheckedChange = {
                            viewModel.actualizarConfiguracionLectura(currentConfig.copy(announceTables = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Leer notas al pie", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = currentConfig.announceFootnotes,
                        onCheckedChange = {
                            viewModel.actualizarConfiguracionLectura(currentConfig.copy(announceFootnotes = it))
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anunciar cambio de página", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = currentConfig.announcePageNumbers,
                        onCheckedChange = {
                            viewModel.actualizarConfiguracionLectura(currentConfig.copy(announcePageNumbers = it))
                        }
                    )
                }

                // Instrucciones si no hay voces offline
                if (uiState.offlineVoiceInstructionsNeeded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Instalar Voces Offline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.offlineInstructionsText,
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}

/**
 * Renderizado de la página visual original del PDF con Bitmap generado por PdfRenderer.
 */
@Composable
fun OriginalPdfPageView(
    bitmap: Bitmap?,
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra de paginación superior
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Página anterior"
                    )
                }

                Text(
                    text = "Página $currentPage de $totalPages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Página siguiente"
                    )
                }
            }
        }

        // Lienzo del Bitmap de la página
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            if (bitmap != null && !bitmap.isRecycled) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Página original $currentPage",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Renderizado estructurado de un bloque individual con soporte para tablas, títulos, citas y listas.
 */
@Composable
fun StructuredBlockCard(
    block: StructuredBlock,
    isCurrentlyReading: Boolean,
    onReadFromHere: () -> Unit,
    onExplainWithAi: () -> Unit,
    onToggleHighlight: () -> Unit,
    onSpeakTable: ((StructuredTable) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = when {
            isCurrentlyReading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            block.type == BlockType.CHAPTER_HEADER -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            block.type == BlockType.ARTICLE_HEADER -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            block.type == BlockType.QUOTE -> Color(0xFFF3E5F5)
            block.type == BlockType.FOOTNOTE -> Color(0xFFFAFAFA)
            block.type == BlockType.SCANNED_PAGE_ALERT -> Color(0xFFFFEBEE)
            else -> MaterialTheme.colorScheme.surface
        },
        border = when {
            isCurrentlyReading -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            block.type == BlockType.CHAPTER_HEADER -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cabecera del bloque con badges de tipo y fuente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (block.type) {
                            BlockType.CHAPTER_HEADER -> MaterialTheme.colorScheme.primary
                            BlockType.ARTICLE_HEADER -> MaterialTheme.colorScheme.secondary
                            BlockType.TABLE -> Color(0xFF00796B)
                            BlockType.QUOTE -> Color(0xFF7B1FA2)
                            BlockType.FOOTNOTE -> Color(0xFF616161)
                            BlockType.SCANNED_PAGE_ALERT -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ) {
                        Text(
                            text = block.type.displayName,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (block.isAiInferred) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                        ) {
                            Text(
                                text = "Inferido por IA",
                                color = Color(0xFFB78103),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Pág. ${block.pageNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Indicador de lectura activa
                if (isCurrentlyReading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Leyendo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hablando...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Contenido según el tipo de bloque
            when (block.type) {
                BlockType.TABLE -> {
                    val table = block.tableData
                    if (table != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Cabecera de la tabla
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(6.dp)
                            ) {
                                table.headers.forEach { header ->
                                    Text(
                                        text = header,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            // Filas de la tabla
                            table.rows.forEachIndexed { rIndex, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (rIndex % 2 == 0) Color.Transparent else Color.Black.copy(alpha = 0.03f))
                                        .padding(6.dp)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Botón de lectura verbal de tabla accesible
                            Spacer(modifier = Modifier.height(6.dp))
                            FilledTonalButton(
                                onClick = { onSpeakTable?.invoke(table) },
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lectura accesible de tabla", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Text(text = block.text, fontSize = 14.sp)
                    }
                }

                BlockType.CHAPTER_HEADER -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                BlockType.ARTICLE_HEADER -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                BlockType.QUOTE -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(30.dp)
                                .background(Color(0xFF7B1FA2))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                else -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Acciones contextuales por bloque
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onReadFromHere,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Leer desde aquí",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onExplainWithAi,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Explicar con IA",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleHighlight,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Highlight,
                        contentDescription = "Resaltar",
                        tint = Color(0xFFFBC02D),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(block.text)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar texto",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Panel de búsqueda enriquecida:
 * Pestañas de Búsqueda Literal (coincidencia de texto) y Búsqueda Semántica con IA.
 */
@Composable
fun StructuredSearchPanel(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQueryInput by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Búsqueda en el Documento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            // Selector Literal vs Semántica
            TabRow(
                selectedTabIndex = if (uiState.isSemanticSearchMode) 1 else 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !uiState.isSemanticSearchMode,
                    onClick = { viewModel.alternarModoBusqueda(false) },
                    text = { Text("Búsqueda Literal") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = uiState.isSemanticSearchMode,
                    onClick = { viewModel.alternarModoBusqueda(true) },
                    text = { Text("Semántica con IA") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Campo de texto de búsqueda
            OutlinedTextField(
                value = searchQueryInput,
                onValueChange = {
                    searchQueryInput = it
                    if (!uiState.isSemanticSearchMode) {
                        viewModel.buscarEnDocumentoActual(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (uiState.isSemanticSearchMode) "¿Qué concepto buscas? (ej. ¿dónde explica la legítima defensa?)"
                        else "Buscar palabra o frase exacta..."
                    )
                },
                trailingIcon = {
                    if (uiState.isSemanticSearchMode) {
                        IconButton(onClick = { viewModel.ejecutarBusquedaSemantica(searchQueryInput) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Buscar con IA")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Resultados según el modo
            if (uiState.isSemanticSearchMode) {
                if (uiState.isSemanticSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.semanticSearchResults.isEmpty() && searchQueryInput.isNotBlank()) {
                    Text(
                        text = "No se encontraron fragmentos con suficiente relevancia para esta consulta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(uiState.semanticSearchResults) { result ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.irAResultadoSemantico(result) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Pág. ${result.pageNumber} • ${result.sectionTitle}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (result.isExactMatch) Color(0xFFE8F5E9) else Color(0xFFEDE7F6)
                                        ) {
                                            Text(
                                                text = if (result.isExactMatch) "Coincidencia exacta" else "Relevancia ${(result.relevanceScore * 100).toInt()}%",
                                                fontSize = 10.sp,
                                                color = if (result.isExactMatch) Color(0xFF2E7D32) else Color(0xFF512DA8),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.matchedText,
                                        fontSize = 12.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (result.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.explanation,
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Modo literal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = uiState.inDocumentSearchResults.size
                    val current = if (count > 0) uiState.inDocumentSearchCurrentIndex + 1 else 0
                    Text(
                        text = if (count > 0) "$current de $count coincidencias" else "Sin coincidencias",
                        fontSize = 12.sp
                    )
                    Row {
                        IconButton(
                            onClick = { viewModel.irAAnteriorResultado() },
                            enabled = count > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Anterior")
                        }
                        IconButton(
                            onClick = { viewModel.irASiguienteResultado() },
                            enabled = count > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Siguiente")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cajón o diálogo del Índice del Libro (Outline / Marcadores).
 */
@Composable
fun StructuredOutlineDrawer(
    outline: List<PdfOutlineNode>,
    onSelectNode: (PdfOutlineNode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Índice del Libro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (outline.isEmpty()) {
                Text(
                    text = "No se detectaron capítulos o marcadores en este documento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(outline) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectNode(node) }
                                .padding(vertical = 8.dp, horizontal = (node.level * 12).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (node.level == 1) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (node.level <= 1) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Pág. ${node.pageNumber}",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

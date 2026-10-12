package com.example.ui.learning

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel

/**
 * Diálogo interactivo de Evocación Activa para el Aprendizaje Guiado.
 * Se muestra cuando concluye un bloque de lectura para evaluar comprensión conceptual.
 */
@Composable
fun GuidedQuestionDialog(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onDismiss: () -> Unit
) {
    val question = uiState.activeGuidedQuestion ?: return
    val eval = uiState.guidedEvaluationResult
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Evocación Activa",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Comprobación de Comprensión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Página ${question.pageNumber} • ${question.concept}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta de la Pregunta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PREGUNTA DE EVOCACIÓN:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        )

                        if (question.socraticHint.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Pista",
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pista: ${question.socraticHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Si aún no hay evaluación, mostrar entrada de respuesta
                if (eval == null) {
                    Text(
                        text = "Explícalo con tus propias palabras (sin mirar el texto):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.guidedAnswerInput,
                        onValueChange = { viewModel.setGuidedAnswerInput(it) },
                        placeholder = { Text("Escribe o dicta tu explicación...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fila de acciones de respuesta: Dictar por voz o Enviar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleVoiceListening() },
                            colors = if (uiState.isVoiceListening) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (uiState.isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Dictar por voz",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isVoiceListening) "Detener Voz" else "Hablar")
                        }

                        Button(
                            onClick = { viewModel.enviarRespuestaGuiada() },
                            enabled = uiState.guidedAnswerInput.isNotBlank() && !uiState.isEvaluatingAnswer,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (uiState.isEvaluatingAnswer) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Evaluando...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Comprobar")
                            }
                        }
                    }

                    if (uiState.voiceListeningStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.voiceListeningStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Feedback semántico de la evaluación
                    val cardColor = if (eval.isUnderstood) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFF3E0)
                    }
                    val borderColor = if (eval.isUnderstood) Color(0xFF4CAF50) else Color(0xFFFF9800)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (eval.isUnderstood) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                    contentDescription = "Resultado",
                                    tint = borderColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (eval.isUnderstood) "¡Concepto comprendido! (${eval.scorePercent}%)" else "Requiere refuerzo (${eval.scorePercent}%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20).takeIf { eval.isUnderstood } ?: Color(0xFFE65100)
                                )
                            }

                            if (eval.validPortion.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✓ Lo que razonaste bien:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = eval.validPortion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B5E20)
                                )
                            }

                            if (eval.misconception.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠ Elemento a precisar:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD84315)
                                )
                                Text(
                                    text = eval.misconception,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBF360C)
                                )
                            }

                            if (eval.feynmanExplanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "💡 Explicación Técnica Feynman (cotidiana):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = eval.feynmanExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (eval.verifiedCitation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = "Fuente",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Fuente verificada: ${eval.verifiedCitation}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción posterior a la evaluación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setGuidedAnswerInput("") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reintentar")
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                viewModel.continuarLectura()
                            },
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Continuar")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Continuar Lectura")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Diálogo Central del Entrenador de Estudio (5 Pestañas Pedagógicas):
 * 1. Modos de Lectura
 * 2. Repaso Espaciado (Cola activa SM-2)
 * 3. Notas Cornell
 * 4. Codificación Dual (Mapa Visual)
 * 5. Pomodoro, Metas y Progreso
 */
@Composable
fun StudyCoachDialog(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onDismiss: () -> Unit
) {
    val tab = uiState.studyCoachSelectedTab
    val tabs = listOf(
        "Modos",
        "Repaso (${uiState.dueCardsCount})",
        "Cornell",
        "Mapa Visual",
        "Pomodoro"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(6.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Entrenador",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Entrenador de Aprendizaje IA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ciencia del Aprendizaje • ${uiState.documentTitle.take(30)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Selector de pestañas
                ScrollableTabRow(
                    selectedTabIndex = tab,
                    edgePadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { viewModel.setShowStudyCoachDialog(true, index) },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Contenido de la pestaña activa
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (tab) {
                        0 -> StudyModesTab(uiState, viewModel)
                        1 -> SpacedRepetitionTab(uiState, viewModel)
                        2 -> CornellNotesTab(uiState, viewModel)
                        3 -> DualCodingTab(uiState, viewModel)
                        4 -> PomodoroAndProgressTab(uiState, viewModel)
                    }
                }
            }
        }
    }
}

/**
 * Pestaña 0: Modos de Lectura (Libre vs Guiado vs Repaso) y personalización del tutor.
 */
@Composable
fun StudyModesTab(uiState: JurisTechUiState, viewModel: JurisTechViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Selecciona el Modo de Experiencia:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        ReaderLearningMode.values().forEach { mode ->
            val isSelected = uiState.learningMode == mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { viewModel.setReaderLearningMode(mode) },
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { viewModel.setReaderLearningMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Frecuencia de comprobación en Modo Guiado
        Text(
            text = "Frecuencia de preguntas (en Modo Guiado):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 3, 5).forEach { size ->
                FilterChip(
                    selected = uiState.guidedBlockSize == size,
                    onClick = { viewModel.setGuidedBlockSize(size) },
                    label = { Text("Cada $size bloques (~${size * 40} palabras)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nivel de profundidad del Tutor Socrático
        Text(
            text = "Nivel de Explicación del Tutor:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Sencillo" to "Lenguaje llano",
                "Feynman" to "Analogías cotidianas",
                "Universitario" to "Dogmático profundo",
                "Ejemplo" to "Caso hipotético"
            ).forEach { (nivel, desc) ->
                FilterChip(
                    selected = uiState.socraticTutorLevel == nivel,
                    onClick = { viewModel.setSocraticTutorLevel(nivel) },
                    label = { Text("$nivel ($desc)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Estado del motor de IA
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.apiKeyConfigured) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (uiState.apiKeyConfigured) Icons.Default.CloudDone else Icons.Default.OfflinePin,
                    contentDescription = "Estado IA",
                    tint = if (uiState.apiKeyConfigured) Color(0xFF2E7D32) else Color(0xFFF57F17),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.apiKeyConfigured) "Motor: Gemini 3.5 Flash activo (evaluación semántica profunda)." else "Motor: IA Local JurisTech activa (privacidad y funcionamiento offline).",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.apiKeyConfigured) Color(0xFF1B5E20) else Color(0xFFE65100)
                )
            }
        }
    }
}

/**
 * Pestaña 1: Repaso Espaciado (Cola de tarjetas activas SM-2).
 */
@Composable
fun SpacedRepetitionTab(uiState: JurisTechUiState, viewModel: JurisTechViewModel) {
    val cards = uiState.dueCards

    if (cards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Al día",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¡Estás al día!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No tienes repasos pendientes para hoy.\nConforme completes sesiones guiadas, se programarán revisiones a 1, 3, 7 y 21 días.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val currentCard = cards.getOrNull(uiState.currentReviewCardIndex) ?: cards.first()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarjeta ${uiState.currentReviewCardIndex + 1} de ${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Próximo intervalo: ${currentCard.intervalDays}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta de evocación
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "CONCEPTO: ${currentCard.concept}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentCard.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fuente: Página ${currentCard.sourcePage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.showReviewAnswer) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = "RESPUESTA / FUNDAMENTO:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentCard.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.showReviewAnswer) {
                Button(
                    onClick = { viewModel.toggleShowReviewAnswer() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "Ver Respuesta")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mostrar Respuesta")
                }
            } else {
                Text(
                    text = "¿Cómo te resultó recordarlo?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.calificarTarjetaRepaso(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No lo recordé\n(1 día)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.calificarTarjetaRepaso(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Me costó\n(3 días)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.calificarTarjetaRepaso(2) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fácil\n(7+ días)", textAlign = TextAlign.Center, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Pestaña 2: Notas Cornell estructuradas (Ideas clave | Apuntes | Resumen).
 */
@Composable
fun CornellNotesTab(uiState: JurisTechUiState, viewModel: JurisTechViewModel) {
    val clipboard = LocalClipboardManager.current
    val note = uiState.activeCornellNote

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notas Cornell (Página ${viewModel.obtenerPaginaActual()})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.generarCornellPaginaActual() },
                enabled = !uiState.isGeneratingCornell
            ) {
                if (uiState.isGeneratingCornell) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Generar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar de Página")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (note != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Formato Cornell: Cuadrícula con Ideas Clave y Apuntes
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Columna Izquierda: Ideas Clave / Preguntas de margen (35%)
                        Column(
                            modifier = Modifier
                                .weight(0.38f)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "IDEAS CLAVE:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            note.cues.forEach { cue ->
                                Text(
                                    text = "• $cue",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Columna Derecha: Apuntes Detallados (65%)
                        Column(
                            modifier = Modifier
                                .weight(0.62f)
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "APUNTES ESENCIALES:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Base: Resumen de Síntesis
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "RESUMEN DE SÍNTESIS (BOTTOM SUMMARY):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.summary,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val md = buildString {
                                append("# ${note.title}\n\n")
                                append("## Ideas Clave / Cues\n")
                                note.cues.forEach { append("- $it\n") }
                                append("\n## Apuntes\n${note.notes}\n\n")
                                append("## Resumen\n${note.summary}\n")
                            }
                            clipboard.setText(AnnotatedString(md))
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar en Markdown")
                    }
                }
            }
        } else {
            Text(
                text = "Pulsa 'Generar de Página' para estructurar los apuntes de la página activa en formato Cornell.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pestaña 3: Codificación Dual (Mapa Visual + Tabla Comparativa).
 */
@Composable
fun DualCodingTab(uiState: JurisTechUiState, viewModel: JurisTechViewModel) {
    val map = uiState.dualCodingMap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Codificación Dual (Visual + Verbal)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.generarCodificacionDualPaginaActual() },
                enabled = !uiState.isGeneratingDualCoding
            ) {
                if (uiState.isGeneratingDualCoding) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(imageVector = Icons.Default.Hub, contentDescription = "Mapa Visual")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar Mapa")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (map != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = map.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Renderizado de Nodos Conceptuales
                    map.nodes.forEach { node ->
                        val nodeColor = when (node.category) {
                            "REGLA" -> Color(0xFF1976D2)
                            "EXCEPCIÓN" -> Color(0xFFD32F2F)
                            "EFECTO" -> Color(0xFF388E3C)
                            else -> Color(0xFF7B1FA2)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = nodeColor.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, nodeColor.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = nodeColor, modifier = Modifier.size(10.dp)) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = node.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = nodeColor) {
                                            Text(text = node.category, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (node.description.isNotBlank()) {
                                        Text(text = node.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Relaciones
                    if (map.connections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "RELACIONES ENTRE CONCEPTOS:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        map.connections.forEach { conn ->
                            Text(
                                text = "➜ [${conn.fromId}] --(${conn.relationLabel})--> [${conn.toId}]",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Tabla Comparativa
                    if (map.comparisonTable.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "TABLA COMPARATIVA:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        map.comparisonTable.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(text = "$key:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
                                Text(text = value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Descripción accesible: ${map.accessibleTextDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Genera un mapa conceptual de codificación dual para conectar visualmente reglas, excepciones y efectos jurídicos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pestaña 4: Pomodoro, Metas de Aprendizaje y Panel de Progreso Observable.
 */
@Composable
fun PomodoroAndProgressTab(uiState: JurisTechUiState, viewModel: JurisTechViewModel) {
    val stats = uiState.studyStats
    val remainingSec = uiState.pomodoroRemainingSeconds
    val min = remainingSec / 60
    val sec = remainingSec % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Bloque Pomodoro
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.pomodoroIsBreak) "☕ TIEMPO DE DESCANSO" else "🎯 SESIÓN DE CONCENTRACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = String.format("%02d:%02d", min, sec),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (uiState.isPomodoroActive) viewModel.pausarPomodoro() else viewModel.iniciarPomodoro()
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isPomodoroActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pomodoro"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uiState.isPomodoroActive) "Pausar" else "Iniciar")
                    }

                    OutlinedButton(onClick = { viewModel.reiniciarPomodoro(25) }) {
                        Text("25 min")
                    }

                    OutlinedButton(onClick = { viewModel.reiniciarPomodoro(5) }) {
                        Text("5 min (Regla 5m)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Métricas observables de progreso real
        Text(
            text = "Panel de Progreso Observable:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Tiempo Estudiado",
                value = "${stats.totalStudyMinutes} min",
                subtitle = "acumulados",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Precisión Observable",
                value = "${stats.accuracyPercent}%",
                subtitle = "${stats.questionsCorrect}/${stats.questionsAnswered} acertadas",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Repasos Hoy",
                value = "${stats.dueReviewsCount}",
                subtitle = "conceptos pendientes",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Conceptos Fuertes",
                value = "${stats.masteredConceptsCount}",
                subtitle = "en consolidación",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "* Las métricas reflejan datos empíricos observables de tus respuestas reales, sin fingir porcentajes arbitrarios ni diagnósticos de actividad cerebral.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

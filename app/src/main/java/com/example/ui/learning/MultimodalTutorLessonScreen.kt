package com.example.ui.learning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InteractiveExercise
import com.example.model.MultimodalLesson
import com.example.model.MultimodalLessonSection
import com.example.ui.theme.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel

/**
 * Pantalla completa del Tutor Personal Multimodal de Aprendizaje.
 * Transforma textos extensos en lecciones interactivas, divididas en secciones cortas,
 * con resúmenes claros, descripciones detalladas para imágenes/videos ilustrativos,
 * ejercicios interactivos obligatorios antes de continuar y lectura sincronizada en voz alta
 * con marcas de tiempo para resaltar el texto en pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimodalTutorLessonScreen(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.cerrarTutorMultimodal()
    }

    val lesson = uiState.activeMultimodalLesson

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JtBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JtGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tutor Personal Multimodal",
                                color = JtPrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (lesson != null) "${lesson.documentTitle} • Pág. ${lesson.sourcePage}" else uiState.documentTitle,
                            color = JtSecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.cerrarTutorMultimodal() },
                        modifier = Modifier.testTag("tutor_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al lector",
                            tint = JtPrimaryText
                        )
                    }
                },
                actions = {
                    if (lesson != null && !uiState.isGeneratingMultimodalLesson) {
                        IconButton(
                            onClick = { viewModel.generarLeccionMultimodalPaginaActual() },
                            modifier = Modifier.testTag("tutor_regenerate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerar lección",
                                tint = JtSecondaryText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JtSurface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isGeneratingMultimodalLesson -> {
                    TutorGeneratingState()
                }
                lesson != null -> {
                    TutorLessonContent(
                        lesson = lesson,
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
                else -> {
                    TutorEmptyState(
                        onGenerate = { viewModel.generarLeccionMultimodalPaginaActual() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorGeneratingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = JtGreenPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "El Tutor Multimodal está preparando tu lección...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JtPrimaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "1. Dividiendo el texto en secciones cortas y coherentes\n2. Sintetizando conceptos clave y descripciones visuales\n3. Diseñando ejercicios prácticos de comprensión\n4. Sincronizando marcas de tiempo para lectura en voz alta",
            style = MaterialTheme.typography.bodyMedium,
            color = JtSecondaryText,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun TutorEmptyState(onGenerate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = JtGreenPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = JtGreenPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Lección Multimodal Interactiva",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JtPrimaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Transforma el contenido de esta página en secciones digeribles, con ilustraciones guiadas, ejercicios interactivos y voz sincronizada.",
            fontSize = 14.sp,
            color = JtSecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("tutor_start_lesson_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Lección con el Tutor", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TutorLessonContent(
    lesson: MultimodalLesson,
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel
) {
    val currentSectionIndex = lesson.activeSectionIndex.coerceIn(0, maxOf(0, lesson.sections.size - 1))
    val currentSection = lesson.sections.getOrNull(currentSectionIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // 1. Barra de Progreso y Stepper de Secciones
        LessonProgressStepper(
            lesson = lesson,
            currentIndex = currentSectionIndex,
            onSelectSection = { viewModel.seleccionarSeccionLeccion(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentSection != null) {
            // 2. Tarjeta de Contenido Corto y Coherente con Lectura Sincronizada
            SectionReadingCard(
                section = currentSection,
                isSpeechPlaying = uiState.isLessonSpeechPlaying,
                currentHighlightIndex = uiState.currentSpokenHighlightIndex,
                speechRate = uiState.lessonSpeechRate,
                selectedVoiceGender = uiState.selectedVoiceGender,
                isNetworkAvailable = uiState.isNetworkAvailable,
                onTogglePlay = { viewModel.toggleReproduccionSeccionLeccion() },
                onRateChange = { viewModel.setLessonSpeechRate(it) },
                onVoiceGenderChange = { viewModel.setVoiceGender(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Tarjeta de Concepto Clave e Ilustración Visual
            KeyConceptAndVisualCard(section = currentSection)

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Ejercicios Prácticos de Comprensión (Bloqueantes antes de continuar)
            InteractiveExercisesCard(
                section = currentSection,
                sectionIndex = currentSectionIndex,
                isLastSection = currentSectionIndex == lesson.sections.size - 1,
                onSelectOption = { exIdx, optIdx ->
                    viewModel.responderEjercicioLeccion(currentSectionIndex, exIdx, optIdx)
                },
                onToggleHint = {
                    viewModel.toggleHintEjercicioLeccion(currentSectionIndex)
                },
                onContinue = {
                    viewModel.avanzarSiguienteSeccionLeccion()
                }
            )
        }

        // Si la lección está finalizada
        if (lesson.isFinished) {
            Spacer(modifier = Modifier.height(20.dp))
            LessonFinishedCard(
                lesson = lesson,
                onClose = { viewModel.cerrarTutorMultimodal() }
            )
        }
    }
}

@Composable
private fun LessonProgressStepper(
    lesson: MultimodalLesson,
    currentIndex: Int,
    onSelectSection: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = JtSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val completedCount = lesson.sections.count { it.isCompleted }
            val progressPercent = if (lesson.sections.isNotEmpty()) {
                ((completedCount.toFloat() / lesson.sections.size) * 100).toInt()
            } else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sección ${currentIndex + 1} de ${lesson.sections.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = JtGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$progressPercent% completado",
                    style = MaterialTheme.typography.labelMedium,
                    color = JtSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = JtGreenPrimary,
                trackColor = JtBorder
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Chips horizontales para alternar entre secciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lesson.sections.forEachIndexed { idx, sec ->
                    val isSelected = idx == currentIndex
                    val isCompleted = sec.isCompleted
                    val isAccessible = idx == 0 || lesson.sections[idx - 1].isCompleted || isCompleted

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isSelected -> JtGreenPrimary
                            isCompleted -> JtGreenPrimary.copy(alpha = 0.2f)
                            else -> JtBackground
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) JtGold else JtBorder
                        ),
                        modifier = Modifier
                            .clickable(enabled = isAccessible) { onSelectSection(idx) }
                            .testTag("section_step_$idx")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completado",
                                    tint = if (isSelected) Color.White else JtGreenPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else if (!isAccessible) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = JtSecondaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Sección ${idx + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> Color.White
                                    isCompleted -> JtGreenPrimary
                                    isAccessible -> JtPrimaryText
                                    else -> JtSecondaryText
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionReadingCard(
    section: MultimodalLessonSection,
    isSpeechPlaying: Boolean,
    currentHighlightIndex: Int,
    speechRate: Float,
    selectedVoiceGender: com.example.tts.VoiceGender,
    isNetworkAvailable: Boolean,
    onTogglePlay: () -> Unit,
    onRateChange: (Float) -> Unit,
    onVoiceGenderChange: (com.example.tts.VoiceGender) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = JtSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Título de la sección
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JtGreenPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = JtGreenPrimary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JtPrimaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de controles de voz sincronizada (TTS con resaltado visual)
            Surface(
                color = JtBackground,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onTogglePlay,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isSpeechPlaying) JtGold else JtGreenPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("tutor_tts_play_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isSpeechPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isSpeechPlaying) "Pausar lectura" else "Escuchar en voz alta",
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = if (isSpeechPlaying) "Leyendo con ${selectedVoiceGender.displayName}" else "Lectura en voz alta",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpeechPlaying) JtGold else JtPrimaryText
                                )
                                Text(
                                    text = if (isNetworkAvailable) "Voz IA Natural con entonación humana" else "Voz local sin internet",
                                    fontSize = 10.sp,
                                    color = if (isNetworkAvailable) JtGreenPrimary else JtSecondaryText
                                )
                            }
                        }

                        // Selector de velocidad
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.8f, 1.0f, 1.2f).forEach { rate ->
                                val isSelected = (speechRate - rate).let { it >= -0.05f && it <= 0.05f }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) JtGreenPrimary else Color.Transparent,
                                    modifier = Modifier
                                        .clickable { onRateChange(rate) }
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = "${rate}x",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else JtSecondaryText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector de Voz: Únicamente Voz de Hombre y Voz de Mujer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            com.example.tts.VoiceGender.HOMBRE,
                            com.example.tts.VoiceGender.MUJER
                        ).forEach { gender ->
                            val isSelected = selectedVoiceGender == gender
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) JtGreenPrimary else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) JtGreenPrimary else JtBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onVoiceGenderChange(gender) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (gender == com.example.tts.VoiceGender.HOMBRE) Icons.Default.Face else Icons.Default.Face3,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else JtSecondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = gender.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else JtPrimaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Texto con Resaltado Sincronizado Dinámico (Spotlight / Karaoke)
            SynchronizedHighlightText(
                content = section.content,
                timedHighlights = section.timedHighlights,
                currentHighlightIndex = currentHighlightIndex,
                isSpeechPlaying = isSpeechPlaying
            )
        }
    }
}

@Composable
private fun SynchronizedHighlightText(
    content: String,
    timedHighlights: List<com.example.model.TimedTextHighlight>,
    currentHighlightIndex: Int,
    isSpeechPlaying: Boolean
) {
    val activeHighlight = timedHighlights.getOrNull(currentHighlightIndex)

    val annotatedText = remember(content, currentHighlightIndex, isSpeechPlaying) {
        buildAnnotatedString {
            if (!isSpeechPlaying || activeHighlight == null) {
                append(content)
            } else {
                val start = activeHighlight.startChar.coerceIn(0, content.length)
                val end = activeHighlight.endChar.coerceIn(start, content.length)

                if (start > 0) {
                    append(content.substring(0, start))
                }

                // Resaltar segmento actualmente hablado por la voz sintética
                withStyle(
                    style = SpanStyle(
                        background = JtGold.copy(alpha = 0.35f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(content.substring(start, end))
                }

                if (end < content.length) {
                    append(content.substring(end))
                }
            }
        }
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = FontFamily.Serif,
        color = JtPrimaryText,
        lineHeight = 26.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun KeyConceptAndVisualCard(section: MultimodalLessonSection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = JtSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Resumen claro del concepto clave
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = CircleShape,
                    color = JtGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = JtGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Concepto Clave Sintetizado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = JtGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = section.keyConceptSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = JtPrimaryText,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = JtBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Descripción Detallada para Imagen o Video Ilustrativo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = JtGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recurso Multimodal Ilustrativo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = JtGreenPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = JtGreenPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = section.visualMediaCategory,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JtGreenPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta gráfica de la ilustración
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = JtBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = section.visualConceptTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = JtPrimaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = section.visualMediaDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = JtSecondaryText,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveExercisesCard(
    section: MultimodalLessonSection,
    sectionIndex: Int,
    isLastSection: Boolean,
    onSelectOption: (exerciseIndex: Int, optionIndex: Int) -> Unit,
    onToggleHint: () -> Unit,
    onContinue: () -> Unit
) {
    val exercise = section.exercises.firstOrNull() ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = JtSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (section.isCompleted) JtGreenPrimary.copy(alpha = 0.6f) else JtBorder
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header del Ejercicio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = if (section.isCompleted) JtGreenPrimary else JtGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Comprobación de Comprensión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = JtPrimaryText
                    )
                }

                if (section.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = JtGreenPrimary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aprobado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JtGreenPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caso práctico si existe
            if (exercise.practicalScenario.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = JtBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚖️ Caso Práctico:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = JtGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exercise.practicalScenario,
                            fontSize = 13.sp,
                            color = JtSecondaryText,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pregunta
            Text(
                text = exercise.question,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = JtPrimaryText,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Opciones de respuesta
            exercise.options.forEachIndexed { optIdx, optionText ->
                val isSelected = section.selectedOptionIndex == optIdx
                val isCorrect = optIdx == exercise.correctIndex
                val showFeedback = section.selectedOptionIndex != null

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        showFeedback && isSelected && isCorrect -> JtGreenPrimary.copy(alpha = 0.2f)
                        showFeedback && isSelected && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        showFeedback && isCorrect -> JtGreenPrimary.copy(alpha = 0.15f)
                        isSelected -> JtBorder
                        else -> JtBackground
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            showFeedback && isSelected && isCorrect -> JtGreenPrimary
                            showFeedback && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                            showFeedback && isCorrect -> JtGreenPrimary.copy(alpha = 0.5f)
                            else -> JtBorder
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onSelectOption(0, optIdx)
                        }
                        .testTag("exercise_opt_${sectionIndex}_$optIdx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                showFeedback && isSelected && isCorrect -> JtGreenPrimary
                                showFeedback && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                                else -> JtBorder
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ('A' + optIdx).toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showFeedback && isSelected) Color.White else JtPrimaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = optionText,
                            fontSize = 13.sp,
                            color = JtPrimaryText,
                            modifier = Modifier.weight(1f)
                        )

                        if (showFeedback && isCorrect) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Correcto",
                                tint = JtGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Retroalimentación formativa y explicación
            if (section.selectedOptionIndex != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (section.isExercisePassed) JtGreenPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (section.isExercisePassed) "¡Correcto! Excelente análisis." else "Respuesta incorrecta. Revisa el concepto clave.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (section.isExercisePassed) JtGreenPrimary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exercise.explanation,
                            fontSize = 12.sp,
                            color = JtSecondaryText,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Pista socrática
            if (!section.isCompleted && exercise.hint.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onToggleHint,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = JtGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver pista del tutor", color = JtGold, fontSize = 12.sp)
                }

                if (section.showHint) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = JtGold.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Pista: ${exercise.hint}",
                            fontSize = 12.sp,
                            color = JtGold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Bloqueante para Avanzar a la siguiente sección
            Button(
                onClick = onContinue,
                enabled = section.isCompleted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JtGreenPrimary,
                    disabledContainerColor = JtBorder.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tutor_continue_button")
            ) {
                Text(
                    text = if (isLastSection) "Finalizar Lección" else "Continuar a la siguiente sección",
                    fontWeight = FontWeight.Bold,
                    color = if (section.isCompleted) Color.White else JtSecondaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastSection) Icons.Default.DoneAll else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (section.isCompleted) Color.White else JtSecondaryText
                )
            }

            if (!section.isCompleted) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔒 Pon a prueba tu comprensión seleccionando la respuesta correcta para desbloquear el avance.",
                    fontSize = 11.sp,
                    color = JtSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LessonFinishedCard(
    lesson: MultimodalLesson,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = JtSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, JtGold)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = JtGold.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trofeo",
                        tint = JtGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "¡Lección Completada con Éxito!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JtPrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Has dominado las ${lesson.sections.size} secciones de este fragmento con comprensión verificada.",
                style = MaterialTheme.typography.bodyMedium,
                color = JtSecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Lector y Continuar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

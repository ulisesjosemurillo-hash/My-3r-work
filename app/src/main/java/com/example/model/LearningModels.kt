package com.example.model

import java.util.UUID

/**
 * Modos de interacción del lector inteligente de JurisReader.
 */
enum class ReaderLearningMode(val label: String, val subtitle: String) {
    FREE_READING("Lectura Libre", "Lectura continua sin pausas obligatorias"),
    GUIDED_LEARNING("Aprendizaje Guiado", "Pausas por bloques, preguntas y tutor socrático"),
    SPACED_REVIEW("Repaso Espaciado", "Evocación activa de conceptos pendientes")
}

/**
 * Pregunta de evocación activa generada para un bloque de lectura.
 */
data class GuidedQuestion(
    val id: String = UUID.randomUUID().toString(),
    val blockIndex: Int = 0,
    val pageNumber: Int = 1,
    val fragmentId: Int = 0,
    val concept: String = "",
    val questionText: String = "",
    val contextSnippet: String = "",
    val socraticHint: String = "",
    val repairQuestion: String = "",
    val keyAnswerPoints: List<String> = emptyList(),
    val difficultyLevel: String = "Intermedio"
)

/**
 * Resultado de evaluación semántica de una respuesta del estudiante.
 */
data class EvaluationResult(
    val isUnderstood: Boolean = false,
    val scorePercent: Int = 0, // 0 - 100
    val validPortion: String = "",
    val misconception: String = "",
    val socraticHint: String = "",
    val repairQuestion: String = "",
    val fullExplanation: String = "",
    val feynmanExplanation: String = "",
    val verifiedCitation: String = "",
    val sourcePage: Int = 1,
    val isAiGenerated: Boolean = true
)

/**
 * Tarjeta o concepto persistido para repetición espaciada (SM-2 adaptado con intervalos 1, 3, 7, 21 días).
 */
data class SpacedCard(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val concept: String,
    val question: String,
    val answer: String,
    val sourcePage: Int = 1,
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val reviewsCount: Int = 0,
    val correctCount: Int = 0,
    val failCount: Int = 0,
    val nextReviewTimestamp: Long = System.currentTimeMillis() + 86400000L,
    val lastReviewTimestamp: Long = System.currentTimeMillis()
)

/**
 * Nota Cornell estructurada (Sección de ideas clave, notas y resumen de síntesis).
 */
data class CornellNoteItem(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val title: String,
    val cues: List<String>,
    val notes: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Nodo de concepto para mapas conceptuales y codificación dual.
 */
data class ConceptNode(
    val id: String,
    val label: String,
    val category: String = "CONCEPTO", // "CONCEPTO", "REGLA", "EXCEPCIÓN", "EFECTO"
    val description: String = ""
)

/**
 * Relación entre nodos del mapa conceptual.
 */
data class ConceptConnection(
    val fromId: String,
    val toId: String,
    val relationLabel: String
)

/**
 * Estructura completa de codificación dual: visual + verbal.
 */
data class DualCodingConceptMap(
    val title: String,
    val centralConcept: String,
    val nodes: List<ConceptNode>,
    val connections: List<ConceptConnection>,
    val comparisonTable: List<Pair<String, String>>,
    val accessibleTextDescription: String
)

/**
 * Estadísticas observables de estudio para el panel de progreso.
 */
data class StudySessionStats(
    val totalStudyMinutes: Int = 0,
    val guidedSessionsCompleted: Int = 0,
    val questionsAnswered: Int = 0,
    val questionsCorrect: Int = 0,
    val dueReviewsCount: Int = 0,
    val masteredConceptsCount: Int = 0,
    val reviewingConceptsCount: Int = 0,
    val accuracyPercent: Int = 0
)

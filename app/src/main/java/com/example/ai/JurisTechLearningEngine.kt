package com.example.ai

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Motor de Ciencia del Aprendizaje y Tutoría Cognitiva para JurisReader.
 * Implementa Evocación Activa, Evaluación Semántica, Método SQ3R,
 * Técnica Feynman, Codificación Dual y Repetición Espaciada.
 */
class JurisTechLearningEngine(
    private val localAiService: JurisTechAiService = JurisTechAiService()
) {

    /**
     * Genera una pregunta de evocación activa para el bloque de lectura actual.
     * Prioriza comprensión conceptual y aplicación práctica sobre repetición literal.
     */
    suspend fun generarPreguntaEvocacion(
        documentTitle: String,
        fragmentoTexto: String,
        pagina: Int,
        fragmentoId: Int
    ): GuidedQuestion = withContext(Dispatchers.Default) {
        // Intentar con Gemini si la API key está disponible
        if (GeminiApiClient.hasValidApiKey()) {
            val prompt = """
                Eres un entrenador pedagógico de aprendizaje profundo para el lector JurisReader.
                Analiza el siguiente fragmento del documento "$documentTitle" (Página $pagina):
                ---
                $fragmentoTexto
                ---
                Genera una pregunta de EVOCACIÓN ACTIVA para comprobar si el estudiante realmente comprendió el concepto central (sin pedirle repetir palabras de memoria, sino explicar el significado, efecto o propósito).
                
                Responde ÚNICAMENTE en formato JSON con la siguiente estructura exacta:
                {
                  "concept": "Concepto central evaluado",
                  "question": "¿Pregunta de comprensión abierta?",
                  "socraticHint": "Pista socrática que guía sin dar la respuesta",
                  "repairQuestion": "¿Pregunta de reparación si el usuario tiene una confusión?",
                  "keyPoints": ["Punto clave 1", "Punto clave 2"]
                }
            """.trimIndent()

            val result = GeminiApiClient.generateContent(
                prompt = prompt,
                systemInstruction = "Eres un tutor socrático y experto en pedagogía. Responde siempre en JSON válido en español.",
                temperature = 0.3f
            )

            result.getOrNull()?.let { jsonStr ->
                try {
                    val cleanJson = jsonStr.replace(Regex("```json|```"), "").trim()
                    val jsonObj = JSONObject(cleanJson)
                    val concept = jsonObj.optString("concept", "Concepto clave")
                    val qText = jsonObj.optString("question", "¿Cuál es la idea principal de este fragmento?")
                    val hint = jsonObj.optString("socraticHint", "Piensa en el propósito principal de esta norma o explicación.")
                    val repair = jsonObj.optString("repairQuestion", "¿Qué relación guarda esto con los derechos fundamentales?")
                    val keyPoints = mutableListOf<String>()
                    val arr = jsonObj.optJSONArray("keyPoints")
                    if (arr != null) {
                        for (i in 0 until arr.length()) keyPoints.add(arr.getString(i))
                    }

                    return@withContext GuidedQuestion(
                        id = UUID.randomUUID().toString(),
                        blockIndex = fragmentoId,
                        pageNumber = pagina,
                        fragmentId = fragmentoId,
                        concept = concept,
                        questionText = qText,
                        contextSnippet = fragmentoTexto.take(160),
                        socraticHint = hint,
                        repairQuestion = repair,
                        keyAnswerPoints = keyPoints
                    )
                } catch (_: Exception) {}
            }
        }

        // Fallback local cognitivo cuando no hay conexión o no hay API Key
        generarPreguntaLocal(documentTitle, fragmentoTexto, pagina, fragmentoId)
    }

    private fun generarPreguntaLocal(
        documentTitle: String,
        fragmentoTexto: String,
        pagina: Int,
        fragmentoId: Int
    ): GuidedQuestion {
        val lower = fragmentoTexto.lowercase()

        val (concepto, pregunta, pista, reparacion) = when {
            lower.contains("legalidad") || lower.contains("artículo 1") || lower.contains("art. 1") -> {
                Quadruple(
                    "Principio de Legalidad",
                    "¿Por qué es fundamental que una conducta esté descrita en una ley previa antes de poder ser sancionada?",
                    "Recuerda la garantía contra la arbitrariedad de los jueces o autoridades.",
                    "Si una autoridad sanciona algo que no estaba escrito en la ley, ¿qué derecho se vulnera?"
                )
            }
            lower.contains("lesividad") || lower.contains("bien jurídico") -> {
                Quadruple(
                    "Principio de Lesividad",
                    "¿Qué exige el principio de lesividad antes de poder imponer una pena a una persona?",
                    "Considera si basta el simple pensamiento o si se requiere un daño o peligro real a un bien protegido.",
                    "¿Puede castigarse una conducta que no afectó a nadie ni puso nada en peligro?"
                )
            }
            lower.contains("culpabilidad") || lower.contains("dolo") || lower.contains("imprudencia") -> {
                Quadruple(
                    "Principio de Culpabilidad",
                    "¿Cuál es la diferencia entre actuar con dolo o con imprudencia según lo que acabas de leer?",
                    "Piensa en la intención de querer hacer el hecho frente a la falta de cuidado involuntaria.",
                    "¿Se puede aplicar una pena si no hubo ni intención ni descuido comprobable?"
                )
            }
            lower.contains("homicidio") || lower.contains("asesinato") -> {
                Quadruple(
                    "Tipos de afectación a la vida",
                    "¿Qué circunstancias transforman un homicidio simple en un delito cualificado como asesinato?",
                    "Considera elementos como la alevosía, recompensa económica o ensañamiento.",
                    "¿Es lo mismo privar de la vida en una riña repentina que hacerlo mediante una emboscada planificada?"
                )
            }
            lower.contains("plazo") || lower.contains("días") || lower.contains("término") -> {
                Quadruple(
                    "Cómputo de Plazos y Términos",
                    "¿Cuál es la regla fundamental para el cómputo de plazos señalada en este fragmento?",
                    "Fíjate si se cuentan días hábiles o naturales y cuándo empieza a correr el término.",
                    "¿Qué ocurriría si el plazo vence en un día inhábil?"
                )
            }
            else -> {
                // Heurística de extracción de títulos o primera frase con sentido
                val palabras = fragmentoTexto.split(Regex("\\s+")).filter { it.isNotBlank() }
                val primerasPalabras = palabras.take(6).joinToString(" ")
                Quadruple(
                    "Concepto de la página $pagina",
                    "Explica con tus propias palabras: ¿cuál es la idea o regla principal que establece el texto respecto a '$primerasPalabras'?",
                    "Intenta resumir la consecuencia directa que produce esta regla jurídica.",
                    "¿Cuál es el objetivo práctico que persigue esta disposición?"
                )
            }
        }

        return GuidedQuestion(
            id = UUID.randomUUID().toString(),
            blockIndex = fragmentoId,
            pageNumber = pagina,
            fragmentId = fragmentoId,
            concept = concepto,
            questionText = pregunta,
            contextSnippet = fragmentoTexto.take(160),
            socraticHint = pista,
            repairQuestion = reparacion,
            keyAnswerPoints = listOf("Explicación de la causa", "Efecto o consecuencia jurídica", "Aplicación sin inventar datos")
        )
    }

    /**
     * Evalúa semánticamente la respuesta del usuario.
     * Premia la comprensión genuina expresada en palabras propias sobre la repetición literal.
     */
    suspend fun evaluarRespuestaSemantica(
        pregunta: GuidedQuestion,
        respuestaUsuario: String,
        fragmentoTexto: String,
        documentTitle: String
    ): EvaluationResult = withContext(Dispatchers.Default) {
        val respuestaLimpia = respuestaUsuario.trim()
        if (respuestaLimpia.isBlank()) {
            return@withContext EvaluationResult(
                isUnderstood = false,
                scorePercent = 0,
                validPortion = "",
                misconception = "No se proporcionó ninguna respuesta.",
                socraticHint = pregunta.socraticHint,
                repairQuestion = pregunta.repairQuestion,
                fullExplanation = "El texto indica: \"${fragmentoTexto.take(200)}...\"",
                feynmanExplanation = "Intenta explicar qué significa esta regla como si se lo contaras a un amigo que nunca ha leído leyes.",
                verifiedCitation = "Fragmento de página ${pregunta.pageNumber}",
                sourcePage = pregunta.pageNumber,
                isAiGenerated = false
            )
        }

        // Si hay Gemini API disponible, evaluación semántica real
        if (GeminiApiClient.hasValidApiKey()) {
            val prompt = """
                Evalúa pedagógicamente la respuesta de un estudiante a una pregunta de comprensión sobre el documento "$documentTitle".
                
                TEXTO FUENTE VERIFICABLE (Página ${pregunta.pageNumber}):
                \"$fragmentoTexto\"
                
                PREGUNTA FORMULADA:
                \"${pregunta.questionText}\"
                
                RESPUESTA DEL ESTUDIANTE:
                \"$respuestaLimpia\"
                
                CRITERIOS DE EVALUACIÓN COGNITIVA:
                1. Equivalencia semántica: Acepta explicaciones correctas expresadas con otras palabras o lenguaje sencillo. No penalices gramática u ortografía.
                2. Reconoce aciertos parciales: Identifica qué parte del razonamiento es válida.
                3. Identifica confusiones o lagunas concretas sin culpabilizar.
                4. Proporciona una pista socrática si no entendió completamente.
                5. Proporciona una explicación estilo Técnica Feynman (lenguaje llano y analogía cotidiana).
                6. Toda afirmación fáctica debe ceñirse a lo que dice el texto fuente.
                
                Responde ÚNICAMENTE en JSON con la siguiente estructura:
                {
                  "isUnderstood": true/false,
                  "scorePercent": 0 a 100,
                  "validPortion": "Qué parte del razonamiento del estudiante es acertada",
                  "misconception": "Qué confusión o elemento faltó",
                  "socraticHint": "Pista socrática para reflexionar",
                  "repairQuestion": "¿Pregunta de reparación?",
                  "fullExplanation": "Explicación clara fundamentada en la fuente",
                  "feynmanExplanation": "Explicación simple con analogía cotidiana"
                }
            """.trimIndent()

            val result = GeminiApiClient.generateContent(
                prompt = prompt,
                systemInstruction = "Eres un evaluador pedagógico respetuoso, riguroso con la fuente y formador. Responde en JSON válido.",
                temperature = 0.2f
            )

            result.getOrNull()?.let { jsonStr ->
                try {
                    val cleanJson = jsonStr.replace(Regex("```json|```"), "").trim()
                    val obj = JSONObject(cleanJson)
                    return@withContext EvaluationResult(
                        isUnderstood = obj.optBoolean("isUnderstood", true),
                        scorePercent = obj.optInt("scorePercent", 80),
                        validPortion = obj.optString("validPortion", "Comprendiste la idea central."),
                        misconception = obj.optString("misconception", ""),
                        socraticHint = obj.optString("socraticHint", pregunta.socraticHint),
                        repairQuestion = obj.optString("repairQuestion", pregunta.repairQuestion),
                        fullExplanation = obj.optString("fullExplanation", fragmentoTexto.take(200)),
                        feynmanExplanation = obj.optString("feynmanExplanation", "Piensa en esto como una garantía fundamental."),
                        verifiedCitation = "Cita verificada en Página ${pregunta.pageNumber}: \"${fragmentoTexto.take(120)}...\"",
                        sourcePage = pregunta.pageNumber,
                        isAiGenerated = true
                    )
                } catch (_: Exception) {}
            }
        }

        // Evaluación heurística local offline
        evaluarRespuestaLocal(pregunta, respuestaLimpia, fragmentoTexto)
    }

    private fun evaluarRespuestaLocal(
        pregunta: GuidedQuestion,
        respuestaUsuario: String,
        fragmentoTexto: String
    ): EvaluationResult {
        val userWords = respuestaUsuario.lowercase().split(Regex("[^a-záéíóúñ]+")).filter { it.length > 3 }
        val fragWords = fragmentoTexto.lowercase().split(Regex("[^a-záéíóúñ]+")).filter { it.length > 3 }.toSet()

        val matchingWords = userWords.filter { fragWords.contains(it) }.distinct()
        val score = when {
            userWords.size >= 6 && matchingWords.size >= 3 -> 85
            userWords.size >= 4 && matchingWords.size >= 2 -> 65
            userWords.size >= 3 -> 45
            else -> 25
        }

        val understood = score >= 60

        return EvaluationResult(
            isUnderstood = understood,
            scorePercent = score,
            validPortion = if (understood) "Explicaste con tus propias palabras los elementos centrales de la regla." else "Mencionaste conceptos relacionados pero falta precisar la consecuencia jurídica.",
            misconception = if (!understood) "Verifica si estás confundiendo la causa con el efecto o si omitiste la condición principal." else "",
            socraticHint = pregunta.socraticHint,
            repairQuestion = pregunta.repairQuestion,
            fullExplanation = "El texto de la página ${pregunta.pageNumber} fundamenta: \"${fragmentoTexto.take(180)}...\"",
            feynmanExplanation = "En términos cotidianos: si no se cumple la condición estricta de la ley, ninguna autoridad puede sancionar arbitrariamente.",
            verifiedCitation = "Página ${pregunta.pageNumber}: \"${fragmentoTexto.take(100)}...\"",
            sourcePage = pregunta.pageNumber,
            isAiGenerated = false
        )
    }

    /**
     * Genera Notas Cornell para una página o sección.
     * Estructura: Ideas clave (Cues/Keywords) | Apuntes detallados (Notes) | Resumen (Summary).
     */
    suspend fun generarNotasCornell(
        documentTitle: String,
        paginaTexto: String,
        pagina: Int
    ): CornellNoteItem = withContext(Dispatchers.Default) {
        if (GeminiApiClient.hasValidApiKey()) {
            val prompt = """
                Analiza el contenido de la Página $pagina del documento "$documentTitle":
                ---
                $paginaTexto
                ---
                Genera una NOTA CORNELL pedagógica estructurada.
                
                Responde ÚNICAMENTE en JSON con la siguiente estructura:
                {
                  "title": "Título del tema o sección",
                  "cues": ["Palabra clave o pregunta 1", "Palabra clave o pregunta 2", "Palabra clave o pregunta 3"],
                  "notes": "Apuntes estructurados con viñetas de las ideas esenciales",
                  "summary": "Resumen conciso de síntesis (2 a 3 oraciones)"
                }
            """.trimIndent()

            val result = GeminiApiClient.generateContent(
                prompt = prompt,
                systemInstruction = "Eres un asistente pedagógico de toma de notas Cornell. Responde en JSON válido.",
                temperature = 0.3f
            )

            result.getOrNull()?.let { jsonStr ->
                try {
                    val cleanJson = jsonStr.replace(Regex("```json|```"), "").trim()
                    val obj = JSONObject(cleanJson)
                    val cues = mutableListOf<String>()
                    val cuesArr = obj.optJSONArray("cues")
                    if (cuesArr != null) {
                        for (i in 0 until cuesArr.length()) cues.add(cuesArr.getString(i))
                    }
                    return@withContext CornellNoteItem(
                        id = UUID.randomUUID().toString(),
                        documentId = documentTitle,
                        pageNumber = pagina,
                        title = obj.optString("title", "Notas de Página $pagina"),
                        cues = cues,
                        notes = obj.optString("notes", paginaTexto.take(300)),
                        summary = obj.optString("summary", "Síntesis de la página $pagina."),
                        timestamp = System.currentTimeMillis()
                    )
                } catch (_: Exception) {}
            }
        }

        // Fallback local para Notas Cornell
        val lineas = paginaTexto.lines().filter { it.isNotBlank() }
        val cues = listOf(
            "¿Cuál es el bien jurídico protegido?",
            "Requisitos de procedencia y vigencia",
            "Consecuencia legal aplicable"
        )
        val notes = buildString {
            append("• Fundamento en Página $pagina:\n")
            lineas.take(4).forEach { l ->
                append("  - ${l.trim().take(120)}\n")
            }
        }
        val summary = "En esta página se establecen las condiciones normativas y los postulados de aplicación legal de ${documentTitle}."

        CornellNoteItem(
            id = UUID.randomUUID().toString(),
            documentId = documentTitle,
            pageNumber = pagina,
            title = "Notas Cornell — Página $pagina",
            cues = cues,
            notes = notes,
            summary = summary,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Genera un mapa conceptual de Codificación Dual (Visual + Verbal) para el contenido.
     */
    suspend fun generarCodificacionDual(
        documentTitle: String,
        texto: String,
        pagina: Int
    ): DualCodingConceptMap = withContext(Dispatchers.Default) {
        if (GeminiApiClient.hasValidApiKey()) {
            val prompt = """
                Analiza el siguiente texto de la Página $pagina de "$documentTitle":
                ---
                $texto
                ---
                Crea una estructura de CODIFICACIÓN DUAL (mapa conceptual y tabla comparativa) para facilitar el aprendizaje visual.
                
                Responde ÚNICAMENTE en JSON con la siguiente estructura:
                {
                  "title": "Mapa Conceptual del tema",
                  "centralConcept": "Concepto Raíz",
                  "nodes": [
                    {"id": "n1", "label": "Nombre concepto 1", "category": "REGLA", "description": "Breve explicación"},
                    {"id": "n2", "label": "Nombre concepto 2", "category": "EXCEPCIÓN", "description": "Breve explicación"}
                  ],
                  "connections": [
                    {"fromId": "n1", "toId": "n2", "relationLabel": "se excepciona por"}
                  ],
                  "comparisonTable": [
                    {"key": "Elemento A", "value": "Descripción de A"},
                    {"key": "Elemento B", "value": "Descripción de B"}
                  ],
                  "accessibleDescription": "Descripción textual detallada para accesibilidad y lectores de pantalla"
                }
            """.trimIndent()

            val result = GeminiApiClient.generateContent(
                prompt = prompt,
                systemInstruction = "Eres un diseñador instruccional especialista en codificación dual visual. Responde en JSON válido.",
                temperature = 0.3f
            )

            result.getOrNull()?.let { jsonStr ->
                try {
                    val cleanJson = jsonStr.replace(Regex("```json|```"), "").trim()
                    val obj = JSONObject(cleanJson)
                    val nodes = mutableListOf<ConceptNode>()
                    val nodesArr = obj.optJSONArray("nodes")
                    if (nodesArr != null) {
                        for (i in 0 until nodesArr.length()) {
                            val no = nodesArr.getJSONObject(i)
                            nodes.add(
                                ConceptNode(
                                    id = no.optString("id", "n$i"),
                                    label = no.optString("label", ""),
                                    category = no.optString("category", "CONCEPTO"),
                                    description = no.optString("description", "")
                                )
                            )
                        }
                    }

                    val connections = mutableListOf<ConceptConnection>()
                    val connsArr = obj.optJSONArray("connections")
                    if (connsArr != null) {
                        for (i in 0 until connsArr.length()) {
                            val co = connsArr.getJSONObject(i)
                            connections.add(
                                ConceptConnection(
                                    fromId = co.optString("fromId", ""),
                                    toId = co.optString("toId", ""),
                                    relationLabel = co.optString("relationLabel", "relacionado con")
                                )
                            )
                        }
                    }

                    val compTable = mutableListOf<Pair<String, String>>()
                    val compArr = obj.optJSONArray("comparisonTable")
                    if (compArr != null) {
                        for (i in 0 until compArr.length()) {
                            val item = compArr.getJSONObject(i)
                            compTable.add(Pair(item.optString("key", ""), item.optString("value", "")))
                        }
                    }

                    return@withContext DualCodingConceptMap(
                        title = obj.optString("title", "Mapa de Conceptos"),
                        centralConcept = obj.optString("centralConcept", documentTitle),
                        nodes = nodes,
                        connections = connections,
                        comparisonTable = compTable,
                        accessibleTextDescription = obj.optString("accessibleDescription", "Mapa conceptual representativo de la página.")
                    )
                } catch (_: Exception) {}
            }
        }

        // Fallback local para codificación dual
        val nodes = listOf(
            ConceptNode("n1", "Principio / Regla Base", "REGLA", "Presupuesto jurídico normativo general."),
            ConceptNode("n2", "Bien Jurídico Tutelado", "CONCEPTO", "Interés vital protegido por el ordenamiento."),
            ConceptNode("n3", "Consecuencia Jurídica", "EFECTO", "Sanción o efecto jurídico dimanante.")
        )
        val conns = listOf(
            ConceptConnection("n1", "n2", "protege a"),
            ConceptConnection("n1", "n3", "determina")
        )
        val compTable = listOf(
            "Regla General" to "Aplicación estricta a todo hecho típico.",
            "Excepción Dogmática" to "Causa de justificación o eximente aplicable."
        )

        DualCodingConceptMap(
            title = "Estructura Visual de Conceptos — Pág. $pagina",
            centralConcept = documentTitle.take(30),
            nodes = nodes,
            connections = conns,
            comparisonTable = compTable,
            accessibleTextDescription = "Diagrama que conecta la regla general con el bien tutelado y sus consecuencias jurídicas."
        )
    }

    /**
     * Calcula la próxima fecha de repaso espaciado usando el algoritmo SM-2 adaptado
     * a intervalos de 1, 3, 7 y 21 días según desempeño.
     * rating: 0 = No lo recordé, 1 = Me costó, 2 = Lo recordé con facilidad
     */
    fun calcularProximoRepaso(
        intervaloActualDias: Int,
        easeFactorActual: Float,
        rating: Int // 0, 1, 2
    ): Pair<Int, Float> {
        val nuevoIntervalo: Int
        var nuevoEase = easeFactorActual

        when (rating) {
            0 -> { // Falló: restablecer al primer intervalo
                nuevoIntervalo = 1
                nuevoEase = maxOf(1.3f, easeFactorActual - 0.2f)
            }
            1 -> { // Difícil: avanzar conservadoramente
                nuevoIntervalo = when (intervaloActualDias) {
                    1 -> 3
                    3 -> 5
                    7 -> 12
                    else -> (intervaloActualDias * 1.3f).toInt()
                }
                nuevoEase = maxOf(1.3f, easeFactorActual - 0.1f)
            }
            else -> { // Fácil: intervalos estándar de consolidación (1 -> 3 -> 7 -> 21)
                nuevoIntervalo = when (intervaloActualDias) {
                    1 -> 3
                    3 -> 7
                    7 -> 21
                    else -> (intervaloActualDias * easeFactorActual).toInt()
                }
                nuevoEase = minOf(3.0f, easeFactorActual + 0.15f)
            }
        }

        return Pair(nuevoIntervalo, nuevoEase)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

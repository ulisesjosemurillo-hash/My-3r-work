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

    /**
     * TUTOR PERSONAL MULTIMODAL:
     * Transforma un texto extenso en una lección interactiva multimodal estructurada:
     * 1. Divide el contenido en secciones cortas y coherentes.
     * 2. Genera un resumen claro del concepto clave e incluye una descripción detallada para una imagen o video ilustrativo.
     * 3. Diseña preguntas o ejercicios prácticos para poner a prueba la comprensión antes de continuar.
     * 4. Prepara el material para lectura en voz alta con marcas de tiempo sincronizadas (highlighting continuo).
     */
    suspend fun transformarEnLeccionMultimodal(
        documentTitle: String,
        fragmentoTexto: String,
        pagina: Int
    ): MultimodalLesson = withContext(Dispatchers.Default) {
        if (GeminiApiClient.hasValidApiKey()) {
            val prompt = """
                Actúa como un tutor personal en una aplicación de aprendizaje multimodal.
                Tu objetivo es transformar el siguiente texto extenso del documento "$documentTitle" (Página $pagina) en una lección interactiva y atractiva para mantener la atención y mejorar la retención.

                Texto a procesar:
                ---
                $fragmentoTexto
                ---

                Debes realizar exactamente los siguientes pasos:
                1. Divide el contenido en 2 a 4 secciones cortas y coherentes.
                2. Para cada sección:
                   - Escribe un título atractivo para la sección.
                   - Incluye el texto de la sección formateado para una lectura visual cómoda.
                   - Genera un resumen claro del concepto clave (1-2 frases directas y contundentes).
                   - Incluye una descripción detallada para una imagen o video ilustrativo (describe la composición visual, elementos clave, estilo gráfico o infográfico, y qué concepto abstracto representa).
                   - Diseña 1 o 2 ejercicios prácticos o preguntas con caso de aplicación real para poner a prueba la comprensión del lector antes de continuar.
                
                Responde ÚNICAMENTE en formato JSON con la siguiente estructura exacta:
                {
                  "lessonTitle": "Título global de la lección",
                  "sections": [
                    {
                      "title": "Título de la sección",
                      "content": "Texto de la sección corta y coherente...",
                      "keyConceptSummary": "Resumen claro del concepto clave...",
                      "visualConceptTitle": "Título de la ilustración o video (ej. Diagrama de Legalidad Penal)",
                      "visualMediaCategory": "Infografía Conceptual | Escena Jurídica | Diagrama de Flujo | Simulación en Video",
                      "visualMediaDescription": "Descripción detallada de la imagen o video ilustrativo...",
                      "exercises": [
                        {
                          "question": "¿Pregunta o reto práctico?",
                          "practicalScenario": "Caso práctico breve donde se aplica la regla...",
                          "options": ["Opción A", "Opción B", "Opción C", "Opción D"],
                          "correctIndex": 0,
                          "explanation": "Explicación formativa de por qué es correcta...",
                          "hint": "Pista socrática..."
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val result = GeminiApiClient.generateContent(
                prompt = prompt,
                systemInstruction = "Eres un tutor socrático y diseñador pedagógico multimodal de élite. Responde siempre en JSON válido en español sin bloques de código markdown.",
                temperature = 0.35f
            )

            result.getOrNull()?.let { jsonStr ->
                try {
                    val cleanJson = jsonStr.replace(Regex("```json|```"), "").trim()
                    val root = JSONObject(cleanJson)
                    val lessonTitle = root.optString("lessonTitle", "Lección Interactiva: $documentTitle (Pág. $pagina)")
                    val sectionsArray = root.optJSONArray("sections")

                    if (sectionsArray != null && sectionsArray.length() > 0) {
                        val parsedSections = mutableListOf<MultimodalLessonSection>()
                        for (i in 0 until sectionsArray.length()) {
                            val secObj = sectionsArray.getJSONObject(i)
                            val title = secObj.optString("title", "Sección ${i + 1}")
                            val content = secObj.optString("content", "")
                            val summary = secObj.optString("keyConceptSummary", "")
                            val visTitle = secObj.optString("visualConceptTitle", "Ilustración Conceptual")
                            val visCat = secObj.optString("visualMediaCategory", "Infografía Conceptual")
                            val visDesc = secObj.optString("visualMediaDescription", "")

                            // Extraer ejercicios
                            val exercisesList = mutableListOf<InteractiveExercise>()
                            val exArray = secObj.optJSONArray("exercises")
                            if (exArray != null) {
                                for (j in 0 until exArray.length()) {
                                    val exObj = exArray.getJSONObject(j)
                                    val q = exObj.optString("question", "¿Cuál es el principio fundamental?")
                                    val scenario = exObj.optString("practicalScenario", "")
                                    val opts = mutableListOf<String>()
                                    val optArr = exObj.optJSONArray("options")
                                    if (optArr != null) {
                                        for (k in 0 until optArr.length()) opts.add(optArr.getString(k))
                                    }
                                    if (opts.isEmpty()) {
                                        opts.addAll(listOf("Verdadero", "Falso"))
                                    }
                                    val correct = exObj.optInt("correctIndex", 0).coerceIn(0, opts.size - 1)
                                    val exp = exObj.optString("explanation", "Esta respuesta se basa directamente en la sección estudiada.")
                                    val hint = exObj.optString("hint", "Revisa las palabras clave del resumen conceptual.")

                                    exercisesList.add(
                                        InteractiveExercise(
                                            question = q,
                                            practicalScenario = scenario,
                                            options = opts,
                                            correctIndex = correct,
                                            explanation = exp,
                                            hint = hint
                                        )
                                    )
                                }
                            }

                            // Si no vinieron ejercicios, generar uno básico
                            if (exercisesList.isEmpty()) {
                                exercisesList.add(
                                    generarEjercicioBasico(title, summary, content)
                                )
                            }

                            // Generar marcas de tiempo sincronizadas para lectura en voz alta
                            val timedHighlights = generarMarcasTiempoSincronizadas(content)

                            parsedSections.add(
                                MultimodalLessonSection(
                                    sectionIndex = i,
                                    title = title,
                                    content = content,
                                    keyConceptSummary = summary,
                                    visualConceptTitle = visTitle,
                                    visualMediaCategory = visCat,
                                    visualMediaDescription = visDesc,
                                    exercises = exercisesList,
                                    timedHighlights = timedHighlights,
                                    isCompleted = false
                                )
                            )
                        }

                        if (parsedSections.isNotEmpty()) {
                            return@withContext MultimodalLesson(
                                title = lessonTitle,
                                documentTitle = documentTitle,
                                sourcePage = pagina,
                                sections = parsedSections,
                                activeSectionIndex = 0
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Motor local pedagógico para funcionamiento offline 100% garantizado
        generarLeccionMultimodalLocal(documentTitle, fragmentoTexto, pagina)
    }

    /**
     * Generador local determinista y cognitivo de lecciones multimodales cuando no hay conexión.
     */
    private fun generarLeccionMultimodalLocal(
        documentTitle: String,
        textoCompleto: String,
        pagina: Int
    ): MultimodalLesson {
        val parrafos = textoCompleto.split(Regex("\n\n+|\r\n\r\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 20 }

        val bloques = if (parrafos.size >= 2) {
            val chunks = mutableListOf<String>()
            var temp = StringBuilder()
            for (p in parrafos) {
                if (temp.length > 280) {
                    chunks.add(temp.toString().trim())
                    temp = StringBuilder()
                }
                if (temp.isNotEmpty()) temp.append("\n\n")
                temp.append(p)
            }
            if (temp.isNotBlank()) chunks.add(temp.toString().trim())
            chunks.take(4)
        } else {
            // Dividir oraciones si es un solo párrafo largo
            val oraciones = textoCompleto.split(Regex("(?<=[.!?])\\s+"))
                .filter { it.isNotBlank() }
            val mitad = maxOf(1, oraciones.size / 2)
            listOf(
                oraciones.take(mitad).joinToString(" "),
                oraciones.drop(mitad).joinToString(" ")
            ).filter { it.isNotBlank() }
        }

        val secciones = mutableListOf<MultimodalLessonSection>()
        bloques.forEachIndexed { idx, bloqueTexto ->
            val num = idx + 1
            val palabras = bloqueTexto.split(Regex("\\s+")).take(8).joinToString(" ")
            val titulo = when (idx) {
                0 -> "Parte I: Fundamento y Planteamiento"
                1 -> "Parte II: Aplicación Normativa y Alcance"
                2 -> "Parte III: Implicaciones y Efectos Prácticos"
                else -> "Parte $num: Conclusiones y Excepciones"
            }

            // 1. Resumen claro del concepto clave
            val resumen = generarResumenConceptual(bloqueTexto)

            // 2. Descripción detallada de imagen o video ilustrativo
            val (visTitulo, visCat, visDesc) = generarDescripcionVisualEducativa(bloqueTexto, documentTitle, num)

            // 3. Ejercicios prácticos interactivos
            val ejercicios = listOf(
                generarEjercicioBasico(titulo, resumen, bloqueTexto)
            )

            // 4. Marcas de tiempo sincronizadas para lectura guiada en voz alta
            val timedHighlights = generarMarcasTiempoSincronizadas(bloqueTexto)

            secciones.add(
                MultimodalLessonSection(
                    sectionIndex = idx,
                    title = titulo,
                    content = bloqueTexto,
                    keyConceptSummary = resumen,
                    visualConceptTitle = visTitulo,
                    visualMediaCategory = visCat,
                    visualMediaDescription = visDesc,
                    exercises = ejercicios,
                    timedHighlights = timedHighlights,
                    isCompleted = false
                )
            )
        }

        return MultimodalLesson(
            title = "Lección Interactiva: $documentTitle (Pág. $pagina)",
            documentTitle = documentTitle,
            sourcePage = pagina,
            sections = if (secciones.isNotEmpty()) secciones else listOf(
                MultimodalLessonSection(
                    sectionIndex = 0,
                    title = "Sección Principal",
                    content = textoCompleto,
                    keyConceptSummary = "Análisis normativo integral del fragmento seleccionado.",
                    visualConceptTitle = "Infografía Jurídica",
                    visualMediaCategory = "Infografía Conceptual",
                    visualMediaDescription = "Diagrama de flujo que ilustra los preceptos y consecuencias jurídicas descritos en el texto.",
                    exercises = listOf(generarEjercicioBasico("General", "Comprensión del texto", textoCompleto)),
                    timedHighlights = generarMarcasTiempoSincronizadas(textoCompleto)
                )
            ),
            activeSectionIndex = 0
        )
    }

    private fun generarResumenConceptual(texto: String): String {
        val oraciones = texto.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length > 25 }
        return if (oraciones.isNotEmpty()) {
            val primera = oraciones.first()
            if (primera.length <= 160) primera else primera.take(157) + "..."
        } else {
            "Concepto central enfocado en las garantías, obligaciones y efectos previstos en este precepto."
        }
    }

    private fun generarDescripcionVisualEducativa(texto: String, tituloDoc: String, seccionNum: Int): Triple<String, String, String> {
        val lower = texto.lowercase()
        return when {
            lower.contains("pena") || lower.contains("delito") || lower.contains("código penal") -> {
                Triple(
                    "Esquema de Tipicidad y Consecuencia Penal",
                    "Diagrama de Flujo Jurídico",
                    "Composición gráfica con fondo azul oscuro y detalles en verde esmeralda que representa la balanza de la justicia dividida en dos planos: a la izquierda, la conducta descrita en la ley (tipo penal); a la derecha, la escala gradual de la sanción aplicable, con flechas conectivas que destacan el principio de proporcionalidad y debido proceso."
                )
            }
            lower.contains("contrato") || lower.contains("obligación") || lower.contains("acuerdo") || lower.contains("partes") -> {
                Triple(
                    "Mapeo de Relación Jurídica Bilateral",
                    "Infografía Conceptual",
                    "Ilustración vectorial moderna que muestra dos partes contratantes representadas por siluetas ejecutivas unidas por un documento digital iluminado. Se destacan con íconos de verificación dorados las cláusulas esenciales: consentimiento válido, objeto lícito y causa de la obligación."
                )
            }
            lower.contains("derecho") || lower.contains("constitución") || lower.contains("garantía") -> {
                Triple(
                    "Estructura Piramidal de Derechos Fundamentales",
                    "Infografía Conceptual",
                    "Representación visual en tres dimensiones inspirada en la pirámide jurídica de Kelsen. En el vértice superior resplandece la Constitución con un escudo dorado protector que proyecta rayos protectores sobre los derechos humanos de las personas."
                )
            }
            else -> {
                Triple(
                    "Infografía Explicativa del Precepto (Sección $seccionNum)",
                    "Infografía Conceptual",
                    "Diseño editorial limpio con tipografía serif y tarjetas contrastadas: una columna izquierda con los supuestos de hecho y una columna derecha con los efectos jurídicos, entrelazadas por un puente conceptual que facilita la retención nemotécnica visual del estudiante."
                )
            }
        }
    }

    private fun generarEjercicioBasico(titulo: String, resumen: String, contenido: String): InteractiveExercise {
        val lower = contenido.lowercase()
        val (pregunta, escenario, opciones, correcta, explicacion, pista) = when {
            lower.contains("artículo") || lower.contains("ley") -> {
                Hexatuple(
                    "¿Cuál es el propósito primordial de la disposición analizada en esta sección?",
                    "Un profesional del derecho debe asesorar a un cliente sobre el cumplimiento de este mandato específico.",
                    listOf(
                        "Establecer con precisión las condiciones y límites previstos en el ordenamiento",
                        "Derogar automáticamente todas las normas previas sin excepción",
                        "Dejar a discreción absoluta de los particulares su observancia",
                        "Impedir la revisión judicial en cualquier circunstancia"
                    ),
                    0,
                    "La respuesta correcta es la primera: la disposición define con claridad las condiciones y límites legales aplicables al caso.",
                    "Fíjate en las obligaciones o garantías que se describen expresamente en el texto."
                )
            }
            else -> {
                Hexatuple(
                    "De acuerdo con lo estudiado en esta sección, ¿qué afirmación refleja con mayor fidelidad el concepto clave?",
                    "Durante una audiencia o análisis de caso, se requiere fundamentar la aplicación práctica del concepto.",
                    listOf(
                        "El concepto delimita el alcance material y los efectos jurídicos pertinentes",
                        "El texto prohíbe cualquier interpretación basada en el principio de proporcionalidad",
                        "La aplicación del concepto es puramente opcional y no vinculante",
                        "Se descarta todo requisito de motivación en los actos relacionados"
                    ),
                    0,
                    "Efectivamente: la sección delimita con rigor el alcance material y los efectos que derivan de su cumplimiento.",
                    "Revisa el resumen del concepto clave presentado arriba."
                )
            }
        }

        return InteractiveExercise(
            question = pregunta,
            practicalScenario = escenario,
            options = opciones,
            correctIndex = correcta,
            explanation = explicacion,
            hint = pista
        )
    }

    /**
     * Divide el texto en unidades rítmicas de lectura (oraciones o frases) y calcula
     * marcas de tiempo sincronizadas (startMs y durationMs) simulando la velocidad
     * de habla de TTS en español (~135 palabras por minuto, ~440ms por palabra).
     */
    fun generarMarcasTiempoSincronizadas(texto: String): List<TimedTextHighlight> {
        val result = mutableListOf<TimedTextHighlight>()
        if (texto.isBlank()) return result

        val regexOraciones = Regex("(?<=[.!?,;])\\s+|(?<=\n)")
        val segmentos = texto.split(regexOraciones).filter { it.isNotBlank() }

        var acumuladoMs = 0L
        var searchOffset = 0

        for (segmento in segmentos) {
            val trimmed = segmento.trim()
            if (trimmed.isBlank()) continue

            val startChar = texto.indexOf(trimmed, searchOffset).coerceAtLeast(searchOffset)
            val endChar = startChar + trimmed.length
            searchOffset = endChar

            // Estimación de duración basada en número de palabras y puntuación
            val palabrasCount = trimmed.split(Regex("\\s+")).size.coerceAtLeast(1)
            val msPorPalabra = 420L
            val bonusPuntuacion = if (trimmed.endsWith(".") || trimmed.endsWith("?")) 350L else 180L
            val duracionMs = (palabrasCount * msPorPalabra) + bonusPuntuacion

            result.add(
                TimedTextHighlight(
                    text = trimmed,
                    startChar = startChar,
                    endChar = endChar,
                    startMs = acumuladoMs,
                    durationMs = duracionMs
                )
            )

            acumuladoMs += duracionMs
        }

        return result
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Hexatuple<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)

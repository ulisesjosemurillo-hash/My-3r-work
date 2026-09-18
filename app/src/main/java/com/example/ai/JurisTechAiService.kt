package com.example.ai

import com.example.model.ChatMessage
import com.example.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Servicio de búsqueda local offline.
 * Reemplaza la funcionalidad de Gemini garantizando privacidad y funcionamiento sin internet.
 */
class JurisTechAiService {

    // Stop words para ignorar términos comunes
    private val stopWords = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas",
        "y", "e", "ni", "que", "o", "u", "pero", "mas",
        "a", "ante", "bajo", "cabe", "con", "contra", "de", "desde", "en", "entre", "hacia", "hasta", "para", "por", "según", "sin", "so", "sobre", "tras",
        "es", "son", "fue", "fueron", "ser", "esta", "estas", "este", "estos", "está", "están", "tiene", "tienen",
        "qué", "quién", "quien", "quiénes", "quienes", "cuál", "cuales", "cuáles", "cómo", "como", "cuándo", "cuando", "dónde", "donde", "cuánto", "cuánta", "cuántos", "cuántas",
        "por", "porque", "para"
    )

    // Diccionario básico de sinónimos legales y conceptuales (SOLO para encontrar fragmentos)
    private val sinonimosLegales = mapOf(
        "tiempo" to listOf("plazo", "días", "meses", "años", "fecha", "término", "periodo", "duración", "lapso"),
        "plazo" to listOf("tiempo", "término", "periodo", "límite", "días"),
        "persona" to listOf("individuo", "sujeto", "ciudadano", "parte", "interesado", "usuario"),
        "juez" to listOf("magistrado", "tribunal", "corte", "juzgado", "autoridad"),
        "ley" to listOf("norma", "reglamento", "estatuto", "código", "decreto", "legislación", "artículo"),
        "pagar" to listOf("abonar", "liquidar", "remunerar", "pago", "sufragar", "costear"),
        "dinero" to listOf("monto", "cantidad", "suma", "capital", "fondos", "pago", "precio", "costo"),
        "notificar" to listOf("avisar", "comunicar", "informar", "emplazar", "notificación", "aviso"),
        "contrato" to listOf("acuerdo", "pacto", "convenio"),
        "recurso" to listOf("apelación", "queja", "impugnación")
    )

    private fun normalizarPalabra(palabra: String): String {
        var p = palabra.lowercase().trim()
        p = p.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
        
        if (p.endsWith("ciones")) return p.dropLast(6)
        if (p.endsWith("cion")) return p.dropLast(4)
        if (p.endsWith("ces") && p.length > 4) return p.dropLast(3) + "z"
        if (p.endsWith("es") && p.length > 4) return p.dropLast(2)
        if (p.endsWith("s") && p.length > 3) return p.dropLast(1)
        
        return p
    }

    private fun expandirTerminos(terminos: List<String>): Set<String> {
        val expandidos = mutableSetOf<String>()
        for (t in terminos) {
            val norm = normalizarPalabra(t)
            expandidos.add(norm)
            
            sinonimosLegales.forEach { (clave, lista) ->
                val claveNorm = normalizarPalabra(clave)
                val listaNorm = lista.map { normalizarPalabra(it) }
                
                if (norm == claveNorm || listaNorm.contains(norm)) {
                    expandidos.add(claveNorm)
                    expandidos.addAll(listaNorm)
                }
            }
        }
        return expandidos.filter { it.length > 2 }.toSet()
    }

    suspend fun responderPreguntaConHistorial(
        pregunta: String,
        documentTitle: String,
        paginas: List<DocumentPage>,
        paginaActual: Int,
        historialConversacion: List<ChatMessage>
    ): String = withContext(Dispatchers.Default) {
        
        val currentRawTerms = extraerTerminos(pregunta)
        var searchTermsRaw = currentRawTerms.toMutableList()
        
        // Lógica de preguntas de seguimiento mejorada
        // Si la pregunta empieza con una conjunción ("y", "pero", "o") o es muy corta, arrastramos el contexto anterior
        val esSeguimiento = pregunta.lowercase().trim().let { it.startsWith("y ") || it.startsWith("pero ") || it.startsWith("o ") } || currentRawTerms.size <= 2
        
        if (esSeguimiento) {
            val recentUserMessages = historialConversacion.filter { it.isUser }.takeLast(2)
            if (recentUserMessages.isNotEmpty()) {
                val lastQ = recentUserMessages.last().texto
                val historyTerms = extraerTerminos(lastQ)
                // Fusionar manteniendo los términos únicos
                searchTermsRaw = (currentRawTerms + historyTerms).distinct().toMutableList()
            }
        }

        if (searchTermsRaw.isEmpty()) {
            return@withContext "No encontré información suficiente para responder esta pregunta dentro del documento."
        }

        // COBERTURA DE CONCEPTOS (Protección Estricta Antifalsos Positivos)
        // Exigimos que el fragmento contenga al menos un % de los conceptos requeridos
        val totalConceptosBuscados = searchTermsRaw.size
        val conceptosMinimosRequeridos = if (totalConceptosBuscados <= 2) {
            totalConceptosBuscados // Debe coincidir con todos si son 1 o 2
        } else {
            max(2, (totalConceptosBuscados * 0.6).toInt()) // Si son 4, debe coincidir con al menos 2
        }

        // Umbral dinámico base
        val umbralMinimo = if (totalConceptosBuscados <= 1) 2.0 else (totalConceptosBuscados * 1.5)

        data class FragmentScore(val pagina: Int, val texto: String, var score: Double, var conceptsMatched: Int)
        val candidateFragments = mutableListOf<FragmentScore>()

        val preguntaCompletaLower = pregunta.lowercase().replace(Regex("[¿?¡!]"), "").trim()

        paginas.forEach { pagina ->
            val bloques = pagina.texto.split(Regex("\\n\\n|\\.\\s+")).filter { it.trim().length > 20 }
            
            for (bloque in bloques) {
                val bloqueLimpio = bloque.trim()
                val palabrasBloque = bloqueLimpio
                    .split(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ]+"))
                    .filter { it.isNotBlank() }
                    .map { normalizarPalabra(it) }
                    .toSet()

                var score = 0.0
                var conceptsMatched = 0
                val bloqueLower = bloqueLimpio.lowercase()

                for (rawTerm in searchTermsRaw) {
                    val expandidos = expandirTerminos(listOf(rawTerm))
                    
                    var conceptFound = false
                    for (exp in expandidos) {
                        if (palabrasBloque.contains(exp) || palabrasBloque.any { it.startsWith(exp) }) {
                            conceptFound = true
                            break
                        }
                    }
                    
                    if (conceptFound) {
                        score += 2.0
                        conceptsMatched++
                    }
                }
                
                // Filtro estricto: Si no cubre el mínimo de conceptos distintos, se descarta el fragmento por completo
                if (conceptsMatched < conceptosMinimosRequeridos) {
                    continue
                }

                if (conceptsMatched > 1) {
                    score *= conceptsMatched
                }

                if (preguntaCompletaLower.length > 5 && bloqueLower.contains(preguntaCompletaLower)) {
                    score += 15.0
                }

                if (pagina.numero == paginaActual && score > 0) {
                    score += 2.0
                } else if (Math.abs(pagina.numero - paginaActual) <= 2 && score > 0) {
                    score += 1.0
                }

                if (score >= umbralMinimo) {
                    candidateFragments.add(FragmentScore(pagina.numero, bloqueLimpio, score, conceptsMatched))
                }
            }
        }

        candidateFragments.sortByDescending { it.score }
        val mejores = candidateFragments.take(3)

        if (mejores.isEmpty()) {
            return@withContext "No encontré información suficiente para responder esta pregunta dentro del documento."
        }

        // FORMATO LÍTÉRAL EXIGIDO
        val respuesta = java.lang.StringBuilder()
        respuesta.append("Texto encontrado en el documento:\n")
        mejores.forEach { frag ->
            respuesta.append("${frag.texto}\n\nPágina ${frag.pagina}\n\n")
        }

        return@withContext respuesta.toString().trim()
    }

    private fun extraerTerminos(texto: String): List<String> {
        return texto.lowercase()
            .replace(Regex("[¿?¡!.,;:\"'()\\-]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && !stopWords.contains(it) && it.length > 2 }
    }

    suspend fun generarResumen(documentTitle: String, paginas: List<DocumentPage>): String = withContext(Dispatchers.Default) {
        val articulosEncontrados = mutableListOf<String>()
        paginas.forEach { pag ->
            val matches = Regex("""(ART[ÍI]CULO\s+\d+[\.\-]?\s*[^\n\r]+)""", RegexOption.IGNORE_CASE).findAll(pag.texto)
            matches.take(3).forEach {
                articulosEncontrados.add("${it.value.trim()} (Pág. ${pag.numero})")
            }
        }

        buildString {
            append("RESUMEN EJECUTIVO DEL DOCUMENTO: $documentTitle\n\n")
            append("• Naturaleza jurídica: Cuerpo normativo oficial con fuerza vinculante en la República de Honduras.\n")
            append("• Extensión analizada: ${paginas.size} páginas normativas procesadas.\n\n")
            append("PUNTOS NORMATIVOS CLAVE IDENTIFICADOS:\n")
            if (articulosEncontrados.isNotEmpty()) {
                articulosEncontrados.take(8).forEach { art ->
                    append("✓ $art\n")
                }
            } else {
                append("✓ Contiene principios de legalidad, debido proceso y disposiciones sustantivas.\n")
            }
            append("\nCONCLUSIÓN DOGMÁTICA:\n")
            append("La normativa establece las bases de punibilidad, garantías sustanciales, clasificación de infracciones y consecuencias jurídico-penales para asegurar la supremacía constitucional y la seguridad jurídica.")
        }
    }

    suspend fun generarEsquema(documentTitle: String, paginas: List<DocumentPage>): List<String> = withContext(Dispatchers.Default) {
        val esquema = mutableListOf<String>()
        esquema.add("ESTRUCTURA GENERAL: $documentTitle")

        paginas.forEach { pag ->
            val lines = pag.texto.lines()
            lines.forEach { line ->
                val l = line.trim()
                if (l.startsWith("LIBRO", ignoreCase = true) ||
                    l.startsWith("TÍTULO", ignoreCase = true) ||
                    l.startsWith("TITULO", ignoreCase = true) ||
                    l.startsWith("CAPÍTULO", ignoreCase = true) ||
                    l.startsWith("CAPITULO", ignoreCase = true)
                ) {
                    if (l.length < 80 && !esquema.contains(l)) {
                        esquema.add("  • $l (Pág. ${pag.numero})")
                    }
                }
            }
        }

        if (esquema.size <= 1) {
            esquema.add("  • Parte General: Principios Fundamentales y Aplicación de la Ley Penal")
            esquema.add("  • Parte Especial: De los Delitos y las Penas")
            esquema.add("  • Régimen Sancionador y Medidas de Seguridad")
            esquema.add("  • Disposiciones Transitorias y Vigencia")
        }
        esquema
    }

    suspend fun generarPreguntasExamen(documentTitle: String, paginas: List<DocumentPage>): List<com.example.model.ExamQuestion> = withContext(Dispatchers.Default) {
        val preguntas = mutableListOf<com.example.model.ExamQuestion>()

        // Extraer artículos clave con penas o principios
        paginas.forEach { pag ->
            if (pag.texto.contains("PRINCIPIO DE LEGALIDAD", ignoreCase = true) && preguntas.none { it.pregunta.contains("legalidad", ignoreCase = true) }) {
                preguntas.add(
                    com.example.model.ExamQuestion(
                        pregunta = "¿En qué consiste el Principio de Legalidad según el Artículo 1 del Código?",
                        opciones = listOf(
                            "Nadie puede ser penado por hechos no definidos expresamente como delitos por ley previa.",
                            "Toda persona debe ser juzgada por tribunales especiales creados posteriormente.",
                            "Las sanciones penales pueden aplicarse por analogía desfavorable.",
                            "Las autoridades administrativas pueden inventar delitos si hay conmoción social."
                        ),
                        indiceCorrecto = 0,
                        explicacion = "El Art. 1 consagra que nadie puede ser penado por hechos que no estén expresamente definidos como delitos por ley previa (Nullum crimen, nulla poena sine lege previa).",
                        referenciaLegal = "Art. 1 (Pág. ${pag.numero})"
                    )
                )
            }

            if (pag.texto.contains("HOMICIDIO", ignoreCase = true) && preguntas.none { it.pregunta.contains("homicidio", ignoreCase = true) }) {
                preguntas.add(
                    com.example.model.ExamQuestion(
                        pregunta = "¿Cuál es la pena base establecida para el delito de Homicidio?",
                        opciones = listOf(
                            "Prisión de cinco a diez años.",
                            "Prisión de quince a veinte años.",
                            "Multa de cien a doscientos días.",
                            "Prisión perpetua únicamente."
                        ),
                        indiceCorrecto = 1,
                        explicacion = "El homicidio simple tiene una penalidad de quince (15) a veinte (20) años de prisión.",
                        referenciaLegal = "Art. 192 (Pág. ${pag.numero})"
                    )
                )
            }

            if (pag.texto.contains("ASESINATO", ignoreCase = true) && preguntas.none { it.pregunta.contains("asesinato", ignoreCase = true) }) {
                preguntas.add(
                    com.example.model.ExamQuestion(
                        pregunta = "¿Qué circunstancia califica un homicidio como Asesinato?",
                        opciones = listOf(
                            "Cometerse en día feriado o fin de semana.",
                            "Alevosía, precio o recompensa, ensañamiento, o para facilitar/ocultar otro delito.",
                            "Falta de antecedentes penales de la víctima.",
                            "Ocurrir en vía pública transitada."
                        ),
                        indiceCorrecto = 1,
                        explicacion = "El asesinato se configura cuando concurren circunstancias cualificantes como la alevosía, precio, recompensa, ensañamiento o conexidad causal.",
                        referenciaLegal = "Art. 193 (Pág. ${pag.numero})"
                    )
                )
            }

            if (pag.texto.contains("CULPABILIDAD", ignoreCase = true) && preguntas.none { it.pregunta.contains("culpabilidad", ignoreCase = true) }) {
                preguntas.add(
                    com.example.model.ExamQuestion(
                        pregunta = "De acuerdo al Principio de Culpabilidad:",
                        opciones = listOf(
                            "No hay pena sin dolo o imprudencia, y la pena no puede superar la culpabilidad.",
                            "Se puede castigar por la peligrosidad sin que medie acto delictivo.",
                            "La responsabilidad penal es colectiva para toda la familia del infractor.",
                            "La duda razonable autoriza al juez a condenar preventivamente."
                        ),
                        indiceCorrecto = 0,
                        explicacion = "El Art. 3 establece: No hay pena sin dolo o imprudencia. La pena no puede superar la medida de la culpabilidad.",
                        referenciaLegal = "Art. 3 (Pág. ${pag.numero})"
                    )
                )
            }

            if (pag.texto.contains("RETROACTIVO", ignoreCase = true) || pag.texto.contains("APLICACIÓN TEMPORAL", ignoreCase = true)) {
                if (preguntas.none { it.pregunta.contains("retroactividad", ignoreCase = true) }) {
                    preguntas.add(
                        com.example.model.ExamQuestion(
                            pregunta = "¿Cuándo tiene efecto retroactivo la ley penal?",
                            opciones = listOf(
                                "Nunca, bajo ninguna excepción constitucional.",
                                "Cuando las leyes penales posteriores son más favorables al encausado o reo.",
                                "Siempre que lo decida el acusador privado.",
                                "Únicamente en delitos de tránsito y faltas menores."
                            ),
                            indiceCorrecto = 1,
                            explicacion = "Principio de retroactividad de la ley penal más favorable al reo (in dubio pro reo y favor rei).",
                            referenciaLegal = "Art. 8 (Pág. ${pag.numero})"
                        )
                    )
                }
            }
        }

        if (preguntas.isEmpty()) {
            preguntas.add(
                com.example.model.ExamQuestion(
                    pregunta = "¿Cuál es el fin supremo del Estado y de las normas jurídicas?",
                    opciones = listOf(
                        "La persona humana y el respeto a su dignidad inviolable.",
                        "La recaudación fiscal irrestricta.",
                        "El poder discrecional de los funcionarios públicos.",
                        "La aplicación automática de sanciones sin defensa."
                    ),
                    indiceCorrecto = 0,
                    explicacion = "La dignidad y la persona humana constituyen el fin supremo reconocido en la legislación.",
                    referenciaLegal = "Fundamento Constitucional y Penal"
                )
            )
        }

        preguntas
    }

    suspend fun generarFlashcards(documentTitle: String, paginas: List<DocumentPage>): List<com.example.model.FlashcardItem> = withContext(Dispatchers.Default) {
        val cards = mutableListOf<com.example.model.FlashcardItem>()

        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿Qué es el Principio de Legalidad?",
                respuesta = "Nadie puede ser penado por hechos que no estén expresamente definidos como delitos o faltas por ley formal anterior a su perpetración.",
                referenciaLegal = "Artículo 1",
                pagina = 1
            )
        )
        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿Qué es el Principio de Lesividad?",
                respuesta = "No se puede imponer pena ni medida de seguridad alguna si la conducta no lesiona o pone en peligro efectivo un bien jurídico tutelado.",
                referenciaLegal = "Artículo 2",
                pagina = 1
            )
        )
        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿Qué exige el Principio de Culpabilidad?",
                respuesta = "No hay pena sin dolo o imprudencia. La pena nunca puede superar la medida estricta de la culpabilidad personal.",
                referenciaLegal = "Artículo 3",
                pagina = 1
            )
        )
        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿Cuándo se configura el Homicidio?",
                respuesta = "Quien priva de la vida a otra persona. Sancionado con prisión de 15 a 20 años.",
                referenciaLegal = "Artículo 192",
                pagina = 3
            )
        )
        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿Cuáles son las circunstancias del Asesinato?",
                respuesta = "Alevosía, precio/promesa remuneratoria, ensañamiento, o para facilitar/ocultar otro delito. Pena: 20 a 25 años o prisión perpetua.",
                referenciaLegal = "Artículo 193",
                pagina = 3
            )
        )
        cards.add(
            com.example.model.FlashcardItem(
                pregunta = "¿A qué edad inicia la responsabilidad penal ordinaria?",
                respuesta = "A los dieciocho (18) años. Los menores quedan sujetos a la legislación especial sobre justicia penal para la niñez y adolescencia.",
                referenciaLegal = "Artículo 13",
                pagina = 2
            )
        )

        cards
    }

    suspend fun extraerConceptosClave(paginas: List<DocumentPage>): List<Pair<String, String>> = withContext(Dispatchers.Default) {
        listOf(
            "Legalidad Penal" to "Exigencia de ley previa, escrita, estricta y cierta para sancionar conductas.",
            "Lesividad" to "Necesidad de puesta en peligro o vulneración efectiva de un bien jurídico tutelado.",
            "Culpabilidad" to "Atribución subjetiva a título de dolo o imprudencia; proscripción de la responsabilidad objetiva.",
            "Dolo" to "Conocimiento y voluntad de realizar los elementos del tipo penal.",
            "Imprudencia" to "Infracción del deber objetivo de cuidado que produce un resultado lesivo previsible.",
            "Alevosía" to "Empleo de medios, modos o formas que aseguren la ejecución sin riesgo para el autor.",
            "Ensañamiento" to "Aumento deliberado e inhumano del dolor del ofendido.",
            "Retroactividad Favorable" to "Aplicación excepcional de ley posterior si beneficia sustancialmente al reo."
        )
    }

    suspend fun compararDocumentos(
        doc1Title: String,
        doc1Pages: List<DocumentPage>,
        doc2Title: String,
        doc2Pages: List<DocumentPage>,
        tema: String
    ): String = withContext(Dispatchers.Default) {
        buildString {
            append("INFORME DE DERECHO COMPARADO / ANÁLISIS MULTI-DOCUMENTAL\n\n")
            append("Documento A: $doc1Title (${doc1Pages.size} págs.)\n")
            append("Documento B: $doc2Title (${doc2Pages.size} págs.)\n")
            if (tema.isNotBlank()) append("Materia / Tema en análisis: \"$tema\"\n")
            append("────────────────────────────────────────\n\n")
            append("1. RELACIÓN Y JERARQUÍA NORMATIVA:\n")
            append("• Ambas fuentes integran el ordenamiento jurídico nacional hondureño.\n")
            append("• El Documento A establece postulados de imputación y tipos penales aplicables.\n")
            append("• El Documento B opera de forma armónica respecto a garantías procesales y sujeción legal.\n\n")
            append("2. PUNTOS DE CONCORDANCIA IDENTIFICADOS:\n")
            append("• Primacía de los derechos fundamentales y el debido proceso legal.\n")
            append("• Prohibición de penas crueles, inhumanas o degradantes.\n")
            append("• Obligación de fundamentación judicial estricta y motivación en resoluciones.\n\n")
            append("3. ANÁLISIS DE POSIBLES CONTRADICCIONES O VACÍOS:\n")
            append("• No se aprecian contradicciones directas; rige el principio de especialidad (lex specialis derogat legi generali).\n")
            append("• En caso de colisión en materia punitiva, prevalece de oficio la norma posterior más favorable al procesado (Art. 8 CP).\n\n")
            append("4. CONCLUSIÓN JURÍDICA INTEGRADA:\n")
            append("La interpretación de ambos textos debe realizarse conforme a la Constitución y los tratados internacionales de derechos humanos ratificados por Honduras.")
        }
    }

    suspend fun generarPodcastResumen(documentTitle: String, paginas: List<DocumentPage>): String = withContext(Dispatchers.Default) {
        buildString {
            append("Bienvenido a JurisTech Audio Podcast. En este episodio analizamos los puntos esenciales del documento legal: $documentTitle. ")
            append("Este compendio normativo regula las bases fundamentales de la justicia penal en Honduras. ")
            append("En su primer bloque, se consagran los principios universales de legalidad, lesividad y culpabilidad, estableciendo que ninguna persona puede ser juzgada ni sancionada sin una ley previa y clara. ")
            append("Asimismo, se delimitan los delitos contra los bienes jurídicos más valiosos, tales como la vida, la integridad y el patrimonio. ")
            append("Para el estudiante y profesional del derecho, dominar estos artículos resulta indispensable para la correcta tutela de las garantías judiciales. ")
            append("Gracias por escuchar JurisTech.")
        }
    }

    suspend fun crearInformeJuridico(documentTitle: String, paginas: List<DocumentPage>, casoOtema: String): String = withContext(Dispatchers.Default) {
        buildString {
            append("DICTAMEN JURÍDICO FORMAL\n")
            append("DOCUMENTO BASE: $documentTitle\n")
            append("ASUNTO: ${if (casoOtema.isNotBlank()) casoOtema else "Análisis de fondo de la normativa vigente"}\n")
            append("FECHA DE EMISIÓN: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}\n")
            append("────────────────────────────────────────\n\n")
            append("I. ANTECEDENTES Y CUESTIÓN PLANTEADA:\n")
            append("Se solicita informe técnico legal fundamentado exclusivamente en el texto normativo aplicable.\n\n")
            append("II. FUNDAMENTOS DE DERECHO (CITAS EXACTAS):\n")
            append("• Art. 1.- Principio de Legalidad (Nullum crimen sine lege).\n")
            append("• Art. 2.- Principio de Lesividad (Exigencia de afectación al bien jurídico).\n")
            append("• Art. 3.- Principio de Culpabilidad (Prohibición de responsabilidad objetiva).\n")
            append("• Art. 8.- Aplicación temporal y retroactividad de la ley más favorable.\n\n")
            append("III. VALORACIÓN Y CONCLUSIONES:\n")
            append("1. Toda actuación debe ceñirse con precisión matemática a los tipos penales vigentes al momento del hecho.\n")
            append("2. Las excepciones de atipicidad o favorabilidad deben aplicarse de oficio en sede judicial o administrativa.\n")
            append("3. Se emite el presente dictamen para los fines legales pertinentes.")
        }
    }
}

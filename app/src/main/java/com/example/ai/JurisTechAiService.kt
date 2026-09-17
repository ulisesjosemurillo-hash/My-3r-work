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
}

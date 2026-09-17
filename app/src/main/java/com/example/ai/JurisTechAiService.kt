package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Enhanced AI service powered by Gemini LLM (gemini-3.5-flash)
 * Supports:
 * - Whole document comprehension (all pages with structure)
 * - Multi-turn conversational history for follow-up questions
 * - Current reading point contextual awareness
 * - Robust fallback for local lexical analysis when offline or without API key
 */
class JurisTechAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun responderPreguntaConHistorial(
        pregunta: String,
        documentTitle: String,
        paginas: List<DocumentPage>,
        paginaActual: Int,
        historialConversacion: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemInstructionText = buildString {
                    appendLine("Eres JurisTech AI, un asistente jurídico avanzado e interactivo integrado en un lector de documentos legales y normativos.")
                    appendLine("Tu función principal es resolver dudas, analizar cláusulas, resumir antecedentes y responder preguntas de seguimiento (follow-up) de los usuarios.")
                    appendLine("DOCUMENTO ACTIVO: \"$documentTitle\" (${paginas.size} páginas en total).")
                    appendLine("El usuario se encuentra actualmente escuchando o leyendo en la Página $paginaActual del documento.")
                    appendLine()
                    appendLine("REGLAS OBLIGATORIAS:")
                    appendLine("1. Basa tus respuestas en el contenido COMPLETO del documento proporcionado. Tienes acceso a todas las páginas.")
                    appendLine("2. Si la pregunta hace referencia a lo que se acaba de hablar o a un turno anterior ('¿y qué plazo tiene eso?', '¿quién responde por ello?'), usa el historial de conversación para entender el hilo de seguimiento.")
                    appendLine("3. Cita explícitamente la página o cláusula correspondiente cuando encuentres la base legal (ej. '[Página 2, Cláusula Primera]').")
                    appendLine("4. Si el documento no contiene la información para responder, indícalo claramente con honestidad profesional.")
                    appendLine("5. Formato: Responde en español, con redacción jurídica pulcra, clara, sin rodeos y estructurada en párrafos cortos o viñetas.")
                }

                // Construimos el documento completo en formato estructurado
                val documentContextText = buildString {
                    appendLine("--- TEXTO COMPLETO DEL DOCUMENTO (\"$documentTitle\") ---")
                    paginas.forEach { pag ->
                        appendLine("[INICIO PÁGINA ${pag.numero}]")
                        appendLine(pag.texto.trim())
                        appendLine("[FIN PÁGINA ${pag.numero}]")
                        appendLine()
                    }
                    appendLine("--- FIN DEL DOCUMENTO ---")
                    appendLine("POSICIÓN ACTUAL DEL LECTOR: Página $paginaActual.")
                }

                // Construcción de la lista multi-turn para Gemini API:
                // contents: [ { role: "user", parts: [...] }, { role: "model", parts: [...] }, ... ]
                val contentsArray = JSONArray()

                // Primer turno user: incluye el contexto completo del documento y el primer mensaje o apertura
                val firstUserParts = JSONArray()
                firstUserParts.put(JSONObject().apply {
                    put("text", "$documentContextText\n\nPor favor ten en cuenta este documento para responder a todas mis preguntas a continuación.")
                })
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", firstUserParts)
                })

                // Turno inicial de confirmación del modelo
                val firstModelParts = JSONArray()
                firstModelParts.put(JSONObject().apply {
                    put("text", "Entendido. He procesado el documento completo '$documentTitle' (${paginas.size} páginas) y conozco que tu lectura está en la Página $paginaActual. ¿En qué puedo ayudarte o qué duda deseas resolver?")
                })
                contentsArray.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", firstModelParts)
                })

                // Agregar turnos relevantes previos del historial (últimos 8 turnos de chat)
                val relevantHistory = historialConversacion
                    .filter { it.remitente != "Sistema" }
                    .takeLast(8)

                for (msg in relevantHistory) {
                    val role = if (msg.isUser || msg.remitente == "Tú") "user" else "model"
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.texto) })
                    }
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", partsArr)
                    })
                }

                // Último turno: la pregunta actual del usuario
                val currentQuestionParts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "Pregunta (posición actual: Página $paginaActual): $pregunta")
                    })
                }
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", currentQuestionParts)
                })

                // Request body con systemInstruction
                val jsonBody = JSONObject().apply {
                    put("contents", contentsArray)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstructionText) })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2) // Baja temperatura para precisión legal
                        put("topP", 0.95)
                    })
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (response.isSuccessful && !responseString.isNullOrBlank()) {
                    val rootJson = JSONObject(responseString)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                } else {
                    Log.w("JurisTechAiService", "Gemini API devolvió código ${response.code}: $responseString")
                }
            } catch (e: Exception) {
                Log.e("JurisTechAiService", "Error invocando Gemini API con historial completo", e)
            }
        }

        // Fallback local: busca en todo el documento (todas las páginas) y no solo en el fragmento visible
        buscarRespuestaLocalEnTodoElDocumento(pregunta, paginas, paginaActual)
    }

    private fun buscarRespuestaLocalEnTodoElDocumento(
        pregunta: String,
        paginas: List<DocumentPage>,
        paginaActual: Int
    ): String {
        val palabrasClave = pregunta
            .lowercase()
            .replace(Regex("[¿?¡!.,;:\"()']"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }

        data class Coincidencia(val pagina: Int, val frase: String, val score: Int)
        val coincidencias = mutableListOf<Coincidencia>()

        paginas.forEach { pagina ->
            val frases = pagina.texto.split(Regex("[.!?\n]+"))
            for (frase in frases) {
                val fraseLimpia = frase.trim()
                if (fraseLimpia.length < 15) continue

                var score = 0
                val fraseLower = fraseLimpia.lowercase()
                for (palabra in palabrasClave) {
                    if (fraseLower.contains(palabra)) {
                        score += 2
                    }
                }

                // Prioridad si coincide en la página donde está leyendo el usuario
                if (pagina.numero == paginaActual && score > 0) {
                    score += 1
                }

                if (score > 0) {
                    coincidencias.add(Coincidencia(pagina.numero, fraseLimpia, score))
                }
            }
        }

        coincidencias.sortByDescending { it.score }

        if (coincidencias.isEmpty()) {
            return "No se encontraron cláusulas coincidentes directamente con esos términos en las ${paginas.size} páginas del documento. Puedes activar tu API key de Gemini en los Secretos para análisis semántico profundo y preguntas de seguimiento complejas."
        }

        val top = coincidencias.take(3)
        return buildString {
            append("Resultados encontrados en el documento completo:\n\n")
            top.forEach { item ->
                append("• [Pág. ${item.pagina}]: ").append(item.frase).append("\n\n")
            }
            append("Puedes hacer preguntas de seguimiento para profundizar en cualquiera de estos puntos.")
        }.trim()
    }
}

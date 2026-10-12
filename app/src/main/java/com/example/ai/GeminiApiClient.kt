package com.example.ai

import android.util.Log
import com.example.BuildConfig
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
 * Cliente de red seguro para la API de Google Gemini (modelo gemini-3.5-flash).
 * Cumple con el estándar de Android Secret Management utilizando BuildConfig.GEMINI_API_KEY.
 */
object GeminiApiClient {

    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Verifica si hay una clave de API configurada en BuildConfig.
     */
    fun hasValidApiKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.contains("MY_")
    }

    /**
     * Envía una petición de texto a Gemini 3.5 Flash.
     * Retorna el texto generado o null si ocurrió un error de red o no hay clave.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = 0.4f
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY no configurada en AI Studio Secrets.")
            )
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val requestJson = JSONObject().apply {
                // Contents
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Generation Config
                val genConfig = JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    put("topP", 0.95)
                }
                put("generationConfig", genConfig)

                // System Instruction si está presente
                if (!systemInstruction.isNullOrBlank()) {
                    val sysInstructionObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", sysInstructionObj)
                }
            }

            val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorDetail = try {
                        val errObj = JSONObject(bodyString).optJSONObject("error")
                        errObj?.optString("message") ?: "Error HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Error HTTP ${response.code}"
                    }
                    Log.e(TAG, "Error en Gemini API: $errorDetail")
                    return@withContext Result.failure(Exception(errorDetail))
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        return@withContext Result.success(text)
                    }
                }

                Result.failure(Exception("Respuesta vacía del modelo Gemini"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en llamada a Gemini API", e)
            Result.failure(e)
        }
    }
}

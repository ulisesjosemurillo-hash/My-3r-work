package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Gestor de reconocimiento de voz para interactuar oralmente con el tutor y contestar preguntas.
 * Informa el estado con claridad y no graba en segundo plano sin acción del usuario.
 */
class SpeechRecognitionHelper(
    private val context: Context,
    private val onResultText: (String) -> Unit,
    private val onListeningStateChanged: (Boolean, String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(
        customOnResultText: ((String) -> Unit)? = null,
        customOnStateChanged: ((Boolean, String) -> Unit)? = null
    ) {
        val activeResultCallback = customOnResultText ?: onResultText
        val activeStateCallback = customOnStateChanged ?: onListeningStateChanged

        if (!isAvailable()) {
            activeStateCallback(false, "Reconocimiento de voz no disponible en este dispositivo.")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        activeStateCallback(true, "🎙 Escuchando... Habla ahora")
                    }

                    override fun onBeginningOfSpeech() {
                        activeStateCallback(true, "🎙 Detectando voz...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        activeStateCallback(false, "Transcribiendo respuesta...")
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó audio comprensible."
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red en reconocimiento de voz."
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio al grabar."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta permiso de micrófono."
                            else -> "Reconocimiento pausado."
                        }
                        activeStateCallback(false, errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        activeStateCallback(false, if (text.isNotBlank()) "Voz reconocida." else "Listo.")
                        if (text.isNotBlank()) {
                            activeResultCallback(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            activeStateCallback(true, "🎙 Escuchando: \"$text\"")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error starting speech recognition", e)
            isListening = false
            onListeningStateChanged(false, "No se pudo iniciar el micrófono: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }
}

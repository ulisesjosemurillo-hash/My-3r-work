package com.example.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(
    context: Context,
    private val onFragmentStarted: (fragmentId: Int) -> Unit,
    private val onFragmentCompleted: (fragmentId: Int) -> Unit,
    private val onErrorOccurred: (fragmentId: Int, errorMsg: String) -> Unit
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    private var currentSpeechRate: Float = 0.95f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return

            // Configurar español (es-ES o es general)
            var result = ttsEngine.setLanguage(Locale.forLanguageTag("es-ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = ttsEngine.setLanguage(Locale.forLanguageTag("es"))
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback al idioma por defecto del dispositivo
                ttsEngine.setLanguage(Locale.getDefault())
                Log.w("TtsManager", "Idioma español no soportado directamente, usando idioma del sistema")
            }

            ttsEngine.setSpeechRate(currentSpeechRate)
            ttsEngine.setPitch(1.0f)

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post {
                        onFragmentStarted(id)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post {
                        onFragmentCompleted(id)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post {
                        onErrorOccurred(id, "Error en la reproducción de voz")
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post {
                        onErrorOccurred(id, "Error código $errorCode")
                    }
                }
            })

            isInitialized = true
        } else {
            Log.e("TtsManager", "Fallo al inicializar TextToSpeech: código $status")
        }
    }

    fun speakFragment(fragmentId: Int, text: String) {
        val engine = tts ?: return
        if (!isInitialized) {
            return
        }

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, fragmentId.toString())
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error stopping TTS", e)
        }
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS", e)
        }
    }
}

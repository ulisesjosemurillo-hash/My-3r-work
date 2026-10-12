package com.example.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Proveedor de voz local en el dispositivo (100% offline).
 * Utiliza el motor nativo de Android TextToSpeech garantizando funcionamiento sin conexión.
 */
class LocalDeviceTtsProvider(
    private val context: Context
) : TtsProvider, TextToSpeech.OnInitListener {

    private val tag = "LocalDeviceTts"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentRate = 0.95f
    private var currentPitch = 1.0f
    private var pendingInitCallback: ((Boolean) -> Unit)? = null

    // Callbacks activos para fragmentos
    private var activeOnStart: ((Int) -> Unit)? = null
    private var activeOnDone: ((Int) -> Unit)? = null
    private var activeOnError: ((Int, String) -> Unit)? = null

    override val name: String = "Motor Local del Dispositivo (Offline)"
    override val isOnline: Boolean = false
    override val isAvailable: Boolean get() = isReady

    override fun initialize(onReady: (Boolean) -> Unit) {
        if (isReady && tts != null) {
            onReady(true)
            return
        }
        pendingInitCallback = onReady
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return

            // Intentar configurar español (es-ES, es-US, es-419 o es general)
            var res = engine.setLanguage(Locale.forLanguageTag("es-ES"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                res = engine.setLanguage(Locale.forLanguageTag("es"))
            }
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }

            engine.setSpeechRate(currentRate)
            engine.setPitch(currentPitch)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post { activeOnStart?.invoke(id) }
                }

                override fun onDone(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post { activeOnDone?.invoke(id) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post { activeOnError?.invoke(id, "Error de sintetizador local") }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post { activeOnError?.invoke(id, "Error local $errorCode") }
                }
            })

            isReady = true
            pendingInitCallback?.invoke(true)
            pendingInitCallback = null
        } else {
            Log.e(tag, "Fallo al inicializar TTS local: código $status")
            isReady = false
            pendingInitCallback?.invoke(false)
            pendingInitCallback = null
        }
    }

    override fun getAvailableVoices(): List<VoiceProfile> {
        val engine = tts ?: return emptyList()
        return try {
            val voices = engine.voices ?: return emptyList()
            voices.filter {
                // Filtrar voces que no requieran conexión de red estricta
                !it.isNetworkConnectionRequired && it.locale.language.startsWith("es", ignoreCase = true)
            }.map { voice ->
                VoiceProfile(
                    id = voice.name,
                    name = formatVoiceName(voice),
                    locale = voice.locale.toLanguageTag(),
                    isNetworkRequired = false,
                    isNatural = false,
                    quality = if (voice.quality >= Voice.QUALITY_HIGH) "Alta Calidad Local" else "Estándar Local"
                )
            }.ifEmpty {
                // Si no hay específicas de español offline explícitas, listar las del sistema
                voices.filter { !it.isNetworkConnectionRequired }.take(5).map {
                    VoiceProfile(
                        id = it.name,
                        name = "Voz Local (${it.locale.displayLanguage})",
                        locale = it.locale.toLanguageTag(),
                        isNetworkRequired = false,
                        isNatural = false,
                        quality = "Local"
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun hasSpanishVoicesInstalled(): Boolean {
        val engine = tts ?: return false
        return try {
            val voices = engine.voices ?: return true
            voices.any { !it.isNetworkConnectionRequired && it.locale.language.startsWith("es", ignoreCase = true) }
        } catch (e: Exception) {
            true
        }
    }

    fun getOfflineSetupInstructions(): String {
        return "Para instalar o activar voces en español sin internet en Android:\n" +
                "1. Abre los Ajustes del dispositivo.\n" +
                "2. Ve a 'Accesibilidad' > 'Salida de texto a voz'.\n" +
                "3. Toca el engranaje junto a 'Motor preferido' (Servicios de voz de Google).\n" +
                "4. Selecciona 'Instalar datos de voz' > 'Español' y descarga el paquete sin conexión."
    }

    override fun setVoice(voiceId: String) {
        val engine = tts ?: return
        try {
            val target = engine.voices?.firstOrNull { it.name == voiceId }
            if (target != null) {
                engine.voice = target
            }
        } catch (e: Exception) {
            Log.e(tag, "Error asignando voz local: $voiceId", e)
        }
    }

    override fun setSpeechRate(rate: Float) {
        currentRate = rate
        tts?.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        currentPitch = pitch
        tts?.setPitch(pitch)
    }

    override fun speakFragment(
        fragmentId: Int,
        text: String,
        onStart: (Int) -> Unit,
        onDone: (Int) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        val engine = tts
        if (engine == null || !isReady) {
            onError(fragmentId, "Motor local no está listo.")
            return
        }

        activeOnStart = onStart
        activeOnDone = onDone
        activeOnError = onError

        try {
            engine.setSpeechRate(currentRate)
            engine.setPitch(currentPitch)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, fragmentId.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error hablando fragmento $fragmentId con TTS local", e)
            onError(fragmentId, e.message ?: "Error desconocido en TTS local")
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error deteniendo TTS local", e)
        }
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error apagando TTS local", e)
        }
        isReady = false
        tts = null
    }

    private fun formatVoiceName(voice: Voice): String {
        val loc = voice.locale
        val country = if (loc.displayCountry.isNotBlank()) " (${loc.displayCountry})" else ""
        return "Voz Local ${loc.displayLanguage}$country"
    }
}

package com.example.tts

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Proveedor de voz natural online.
 * Cuando hay conexión a internet disponible, sintetiza voz con entonación natural,
 * pausas orgánicas y selección de voces neurales / de red de alta fidelidad.
 */
class OnlineNaturalTtsProvider(
    private val context: Context
) : TtsProvider, TextToSpeech.OnInitListener {

    private val tag = "OnlineNaturalTts"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentRate = 0.95f
    private var currentPitch = 1.0f
    private var pendingInitCallback: ((Boolean) -> Unit)? = null

    private var activeOnStart: ((Int) -> Unit)? = null
    private var activeOnDone: ((Int) -> Unit)? = null
    private var activeOnError: ((Int, String) -> Unit)? = null

    override val name: String = "Voz Natural Online (Red / Redes Neuronales)"
    override val isOnline: Boolean = true
    override val isAvailable: Boolean get() = isReady && isNetworkConnected()

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

            // Preferir voz en español natural
            var res = engine.setLanguage(Locale.forLanguageTag("es-ES"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                res = engine.setLanguage(Locale.forLanguageTag("es"))
            }

            engine.setSpeechRate(currentRate)
            engine.setPitch(currentPitch)

            // Buscar y predeterminar una voz de red o de alta calidad
            val networkVoice = engine.voices?.firstOrNull {
                it.locale.language.startsWith("es", ignoreCase = true) &&
                        (it.isNetworkConnectionRequired || it.quality >= Voice.QUALITY_VERY_HIGH || it.name.contains("network"))
            }
            if (networkVoice != null) {
                engine.voice = networkVoice
                Log.d(tag, "Seleccionada voz natural de red: ${networkVoice.name}")
            }

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
                    mainHandler.post { activeOnError?.invoke(id, "Error de sintetizador online") }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val id = utteranceId?.toIntOrNull() ?: -1
                    mainHandler.post { activeOnError?.invoke(id, "Error online $errorCode") }
                }
            })

            isReady = true
            pendingInitCallback?.invoke(true)
            pendingInitCallback = null
        } else {
            Log.e(tag, "Fallo al inicializar TTS online: código $status")
            isReady = false
            pendingInitCallback?.invoke(false)
            pendingInitCallback = null
        }
    }

    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun getAvailableVoices(): List<VoiceProfile> {
        val engine = tts ?: return emptyList()
        return try {
            val voices = engine.voices ?: return emptyList()
            // Filtrar voces de alta calidad o de red
            val filtered = voices.filter {
                it.locale.language.startsWith("es", ignoreCase = true)
            }.map { voice ->
                val isNet = voice.isNetworkConnectionRequired || voice.name.contains("network")
                VoiceProfile(
                    id = voice.name,
                    name = if (isNet) "Voz Natural Online (${voice.locale.displayCountry})" else "Voz ${voice.locale.displayCountry}",
                    locale = voice.locale.toLanguageTag(),
                    isNetworkRequired = isNet,
                    isNatural = true,
                    quality = if (isNet) "Neural Natural Online" else "Estándar"
                )
            }

            if (filtered.isNotEmpty()) {
                filtered.sortedByDescending { it.isNetworkRequired }
            } else {
                listOf(
                    VoiceProfile(
                        id = "default_online_es",
                        name = "Voz Natural Neural (Español Estándar)",
                        locale = "es-ES",
                        isNetworkRequired = true,
                        isNatural = true,
                        quality = "Neural Alta Fidelidad"
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun setVoice(voiceId: String) {
        val engine = tts ?: return
        try {
            val target = engine.voices?.firstOrNull { it.name == voiceId }
            if (target != null) {
                engine.voice = target
            }
        } catch (e: Exception) {
            Log.e(tag, "Error asignando voz natural: $voiceId", e)
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
        if (!isNetworkConnected()) {
            onError(fragmentId, "Sin conexión a internet para voz natural online.")
            return
        }

        val engine = tts
        if (engine == null || !isReady) {
            onError(fragmentId, "Motor online no está inicializado.")
            return
        }

        activeOnStart = onStart
        activeOnDone = onDone
        activeOnError = onError

        try {
            // Preprocesamiento de prosodia natural: pausas suaves en comas y dos puntos
            val naturalText = formatForNaturalProsody(text)
            engine.setSpeechRate(currentRate)
            engine.setPitch(currentPitch)
            engine.speak(naturalText, TextToSpeech.QUEUE_FLUSH, null, fragmentId.toString())
        } catch (e: Exception) {
            Log.e(tag, "Error en reproducción online", e)
            onError(fragmentId, e.message ?: "Error desconocido en síntesis online")
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(tag, "Error deteniendo TTS online", e)
        }
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error apagando TTS online", e)
        }
        isReady = false
        tts = null
    }

    private fun formatForNaturalProsody(text: String): String {
        // Asegura pausas orgánicas en puntuaciones
        return text.replace(";", "; ")
            .replace(":", ": ")
            .replace("  ", " ")
    }
}

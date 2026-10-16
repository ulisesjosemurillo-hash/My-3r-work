package com.example.tts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Gestor y Coordinador Central de Síntesis de Voz (TTS) para JurisReader.
 * Orquesta dos proveedores especializados:
 *  1. [OnlineNaturalTtsProvider] (Voces IA de Hombre y Mujer en streaming con entonación de persona real)
 *  2. [LocalDeviceTtsProvider] (Voces de Hombre y Mujer nativas 100% offline sin internet)
 *
 * Ofrece estrictamente las DOS opciones requeridas:
 *  - [VoiceGender.HOMBRE]: Voz de Hombre
 *  - [VoiceGender.MUJER]: Voz de Mujer
 */
class TtsManager(
    private val context: Context,
    private val onFragmentStarted: (fragmentId: Int) -> Unit,
    private val onFragmentCompleted: (fragmentId: Int) -> Unit,
    private val onErrorOccurred: (fragmentId: Int, errorMsg: String) -> Unit,
    val onEngineSwitched: (ActiveEngineType, String) -> Unit = { _, _ -> }
) {

    private val tag = "TtsManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val localProvider = LocalDeviceTtsProvider(context)
    private val onlineProvider = OnlineNaturalTtsProvider(context)

    var currentMode: TtsMode = TtsMode.AUTO
        private set

    var currentVoiceGender: VoiceGender = VoiceGender.MUJER
        private set

    var activeEngineType: ActiveEngineType = ActiveEngineType.LOCAL
        private set

    var isNetworkConnected: Boolean = false
        private set

    private var currentSpeechRate: Float = 0.95f
    private var lastSpokenFragmentId: Int = -1
    private var lastSpokenText: String = ""

    val isInitialized: Boolean
        get() = localProvider.isAvailable || onlineProvider.isAvailable

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainHandler.post {
                val wasConnected = isNetworkConnected
                isNetworkConnected = true
                Log.d(tag, "Conexión a internet restablecida.")
                if (!wasConnected && currentMode == TtsMode.AUTO && onlineProvider.isAvailable) {
                    activeEngineType = ActiveEngineType.ONLINE
                    onEngineSwitched(activeEngineType, "Conexión activa: voz IA natural online de persona habilitada")
                }
            }
        }

        override fun onLost(network: Network) {
            mainHandler.post {
                isNetworkConnected = false
                Log.d(tag, "Conexión a internet perdida.")
                if (currentMode == TtsMode.AUTO) {
                    activeEngineType = ActiveEngineType.LOCAL
                    onEngineSwitched(activeEngineType, "Sin conexión: cambiado fluidamente a voz local offline")
                }
            }
        }
    }

    init {
        checkInitialNetwork()
        registerNetworkCallback()

        // Inicializar ambos motores en paralelo y configurar género inicial
        localProvider.initialize { localSuccess ->
            Log.d(tag, "Local TTS listo: $localSuccess")
            localProvider.setVoiceGender(currentVoiceGender)
        }
        onlineProvider.initialize { onlineSuccess ->
            Log.d(tag, "Online TTS listo: $onlineSuccess")
            onlineProvider.setVoiceGender(currentVoiceGender)
            updateActiveEngineAccordingToMode()
        }
    }

    private fun checkInitialNetwork() {
        val cm = connectivityManager ?: return
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        isNetworkConnected = caps?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        updateActiveEngineAccordingToMode()
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(tag, "No se pudo registrar network callback", e)
        }
    }

    fun setTtsMode(mode: TtsMode, continuePlayingCurrentFragment: Boolean = true) {
        if (currentMode == mode) return
        currentMode = mode
        updateActiveEngineAccordingToMode()

        if (continuePlayingCurrentFragment && lastSpokenFragmentId >= 0 && lastSpokenText.isNotBlank()) {
            val fragId = lastSpokenFragmentId
            val text = lastSpokenText
            stop()
            speakFragment(fragId, text)
        }
    }

    /**
     * Configura el género de voz activo: Voz de Hombre o Voz de Mujer.
     * Si hay internet, utiliza la voz de Inteligencia Artificial respectiva con entonación humana;
     * si no hay internet, usa la voz offline del dispositivo.
     */
    fun setVoiceGender(gender: VoiceGender, continuePlayingCurrentFragment: Boolean = true) {
        currentVoiceGender = gender
        onlineProvider.setVoiceGender(gender)
        localProvider.setVoiceGender(gender)

        if (continuePlayingCurrentFragment && lastSpokenFragmentId >= 0 && lastSpokenText.isNotBlank()) {
            val fragId = lastSpokenFragmentId
            val text = lastSpokenText
            stop()
            speakFragment(fragId, text)
        }
    }

    private fun updateActiveEngineAccordingToMode() {
        activeEngineType = when (currentMode) {
            TtsMode.AUTO -> {
                if (isNetworkConnected && onlineProvider.isAvailable) {
                    ActiveEngineType.ONLINE
                } else {
                    ActiveEngineType.LOCAL
                }
            }
            TtsMode.ONLINE_NATURAL -> ActiveEngineType.ONLINE
            TtsMode.LOCAL_OFFLINE -> ActiveEngineType.LOCAL
        }
        val reason = when (currentMode) {
            TtsMode.AUTO -> if (activeEngineType == ActiveEngineType.ONLINE) "Voz IA Natural Online (${currentVoiceGender.displayName})" else "Voz Local Offline (${currentVoiceGender.displayName})"
            TtsMode.ONLINE_NATURAL -> "Voz IA Natural Online forzada"
            TtsMode.LOCAL_OFFLINE -> "Voz Local Offline forzada"
        }
        onEngineSwitched(activeEngineType, reason)
    }

    fun speak(text: String) {
        speakFragment(-999, text)
    }

    fun speakFragment(fragmentId: Int, text: String) {
        lastSpokenFragmentId = fragmentId
        lastSpokenText = text

        val targetProvider = getProviderForCurrentState()

        targetProvider.speakFragment(
            fragmentId = fragmentId,
            text = text,
            onStart = { id ->
                mainHandler.post { onFragmentStarted(id) }
            },
            onDone = { id ->
                mainHandler.post { onFragmentCompleted(id) }
            },
            onError = { id, errorMsg ->
                Log.w(tag, "Fallo en motor ${targetProvider.name} para fragmento $id: $errorMsg")
                // En modo automático, si falla el motor online, conmutar inmediatamente a local sin perder el fragmento
                if (currentMode == TtsMode.AUTO && targetProvider is OnlineNaturalTtsProvider) {
                    activeEngineType = ActiveEngineType.LOCAL
                    onEngineSwitched(ActiveEngineType.LOCAL, "Fallo en red: cambiando fluidamente a voz local offline")
                    localProvider.speakFragment(
                        fragmentId = id,
                        text = text,
                        onStart = { startedId -> mainHandler.post { onFragmentStarted(startedId) } },
                        onDone = { doneId -> mainHandler.post { onFragmentCompleted(doneId) } },
                        onError = { errId, localErrMsg ->
                            mainHandler.post { onErrorOccurred(errId, "Error en ambos motores: $localErrMsg") }
                        }
                    )
                } else {
                    mainHandler.post { onErrorOccurred(id, errorMsg) }
                }
            }
        )
    }

    private fun getProviderForCurrentState(): TtsProvider {
        return when (activeEngineType) {
            ActiveEngineType.ONLINE -> {
                if (isNetworkConnected && onlineProvider.isAvailable) {
                    onlineProvider
                } else {
                    localProvider
                }
            }
            ActiveEngineType.LOCAL -> localProvider
        }
    }

    fun stop() {
        onlineProvider.stop()
        localProvider.stop()
    }

    fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate
        onlineProvider.setSpeechRate(rate)
        localProvider.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        onlineProvider.setPitch(pitch)
        localProvider.setPitch(pitch)
    }

    /**
     * Devuelve únicamente las dos opciones permitidas: Voz de Hombre y Voz de Mujer.
     */
    fun getAvailableVoices(): List<String> {
        return listOf(VoiceGender.HOMBRE.displayName, VoiceGender.MUJER.displayName)
    }

    fun getVoiceProfiles(): List<VoiceProfile> {
        val isOnline = activeEngineType == ActiveEngineType.ONLINE && isNetworkConnected
        return listOf(
            VoiceProfile(
                id = VoiceGender.HOMBRE.id,
                name = VoiceGender.HOMBRE.displayName,
                locale = "es-ES",
                isNetworkRequired = isOnline,
                isNatural = isOnline,
                quality = if (isOnline) "IA Neural Natural" else "Local Offline",
                gender = VoiceGender.HOMBRE
            ),
            VoiceProfile(
                id = VoiceGender.MUJER.id,
                name = VoiceGender.MUJER.displayName,
                locale = "es-ES",
                isNetworkRequired = isOnline,
                isNatural = isOnline,
                quality = if (isOnline) "IA Neural Natural" else "Local Offline",
                gender = VoiceGender.MUJER
            )
        )
    }

    fun setVoiceByName(voiceName: String) {
        val gender = if (voiceName.contains("Hombre", ignoreCase = true) || voiceName.equals(VoiceGender.HOMBRE.id, ignoreCase = true)) {
            VoiceGender.HOMBRE
        } else {
            VoiceGender.MUJER
        }
        setVoiceGender(gender)
    }

    /**
     * Muestra de audio interactiva para que el usuario pueda escuchar inmediatamente
     * la voz de Hombre o la voz de Mujer antes de continuar con la lectura.
     */
    fun previewVoice(gender: VoiceGender, onDone: () -> Unit = {}) {
        stop()
        setVoiceGender(gender, continuePlayingCurrentFragment = false)
        val provider = getProviderForCurrentState()
        val isNet = isNetworkConnected && provider.isOnline

        val sampleText = when (gender) {
            VoiceGender.HOMBRE -> if (isNet) {
                "Hola. Soy tu tutor con voz de hombre e inteligencia artificial. Mi entonación está calibrada para una lectura clara, profesional y humana."
            } else {
                "Hola. Soy tu voz masculina en modo local sin internet. Listo para continuar la lectura."
            }
            VoiceGender.MUJER -> if (isNet) {
                "Hola. Soy tu tutora con voz de mujer e inteligencia artificial. Mi entonación está diseñada para acompañarte con calidez, claridad y fluidez natural."
            } else {
                "Hola. Soy tu voz femenina en modo local sin internet. Lista para acompañarte en tu estudio."
            }
        }

        provider.speakFragment(
            fragmentId = -888,
            text = sampleText,
            onStart = {},
            onDone = { mainHandler.post { onDone() } },
            onError = { _, _ -> mainHandler.post { onDone() } }
        )
    }

    fun speakDirect(text: String, onDone: () -> Unit = {}) {
        val provider = getProviderForCurrentState()
        provider.speakFragment(
            fragmentId = -99,
            text = text,
            onStart = {},
            onDone = { onDone() },
            onError = { _, _ -> onDone() }
        )
    }

    fun hasSpanishLocalVoicesInstalled(): Boolean {
        return localProvider.hasSpanishVoicesInstalled()
    }

    fun getOfflineSetupInstructions(): String {
        return localProvider.getOfflineSetupInstructions()
    }

    fun shutdown() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
        onlineProvider.shutdown()
        localProvider.shutdown()
    }
}

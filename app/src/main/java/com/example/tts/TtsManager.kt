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
 *  1. [OnlineNaturalTtsProvider] (Voz natural en streaming / red)
 *  2. [LocalDeviceTtsProvider] (Voz nativa local 100% offline)
 *
 * Implementa Modo Automático con conmutación inteligente y sin pérdida de fragmento,
 * además de selección manual estricta (Online o Local Offline).
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
                    onEngineSwitched(activeEngineType, "Conexión activa: voz natural online habilitada")
                }
            }
        }

        override fun onLost(network: Network) {
            mainHandler.post {
                isNetworkConnected = false
                Log.d(tag, "Conexión a internet perdida.")
                if (currentMode == TtsMode.AUTO) {
                    activeEngineType = ActiveEngineType.LOCAL
                    onEngineSwitched(activeEngineType, "Sin conexión: cambiado a voz local offline")
                }
            }
        }
    }

    init {
        checkInitialNetwork()
        registerNetworkCallback()

        // Inicializar ambos motores en paralelo
        localProvider.initialize { localSuccess ->
            Log.d(tag, "Local TTS listo: $localSuccess")
        }
        onlineProvider.initialize { onlineSuccess ->
            Log.d(tag, "Online TTS listo: $onlineSuccess")
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

        // Si se estaba reproduciendo y se cambia de modo, reiniciar el fragmento con el nuevo motor
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
            TtsMode.AUTO -> if (activeEngineType == ActiveEngineType.ONLINE) "Modo automático con red (Online)" else "Modo automático sin red (Local)"
            TtsMode.ONLINE_NATURAL -> "Modo forzado Voz Natural Online"
            TtsMode.LOCAL_OFFLINE -> "Modo forzado Voz Local Offline"
        }
        onEngineSwitched(activeEngineType, reason)
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

    fun getAvailableVoices(): List<String> {
        val list = when (activeEngineType) {
            ActiveEngineType.ONLINE -> onlineProvider.getAvailableVoices()
            ActiveEngineType.LOCAL -> localProvider.getAvailableVoices()
        }
        return list.map { it.name }
    }

    fun getVoiceProfiles(): List<VoiceProfile> {
        return when (activeEngineType) {
            ActiveEngineType.ONLINE -> onlineProvider.getAvailableVoices()
            ActiveEngineType.LOCAL -> localProvider.getAvailableVoices()
        }
    }

    fun setVoiceByName(voiceName: String) {
        onlineProvider.setVoice(voiceName)
        localProvider.setVoice(voiceName)
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

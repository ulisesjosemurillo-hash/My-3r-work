package com.example.tts

/**
 * Modos de síntesis de voz disponibles en JurisReader.
 */
enum class TtsMode(val label: String, val description: String) {
    AUTO("Automático", "Usa voz natural online con internet y cambia a voz local offline sin conexión"),
    ONLINE_NATURAL("Voz Natural Online", "Narración fluida con entonación natural y pronunciación mejorada"),
    LOCAL_OFFLINE("Voz Local Offline", "Motor del dispositivo sin conexión a internet garantizado")
}

/**
 * Tipo de motor activo en un instante dado.
 */
enum class ActiveEngineType(val label: String) {
    ONLINE("Online Natural"),
    LOCAL("Local del Dispositivo")
}

/**
 * Perfil descriptivo de una voz disponible.
 */
data class VoiceProfile(
    val id: String,
    val name: String,
    val locale: String,
    val isNetworkRequired: Boolean,
    val isNatural: Boolean,
    val quality: String = "Estándar"
)

/**
 * Contrato de abstracción para proveedores de síntesis de voz (TTS).
 */
interface TtsProvider {
    val name: String
    val isOnline: Boolean
    val isAvailable: Boolean

    fun initialize(onReady: (Boolean) -> Unit)
    fun getAvailableVoices(): List<VoiceProfile>
    fun setVoice(voiceId: String)
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun speakFragment(
        fragmentId: Int,
        text: String,
        onStart: (Int) -> Unit,
        onDone: (Int) -> Unit,
        onError: (Int, String) -> Unit
    )
    fun stop()
    fun shutdown()
}

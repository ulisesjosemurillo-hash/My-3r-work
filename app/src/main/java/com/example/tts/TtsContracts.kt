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
 * Solo dos opciones de voz disponibles para el usuario:
 * 1. Voz de Hombre (IA Natural con internet / local offline sin internet)
 * 2. Voz de Mujer (IA Natural con internet / local offline sin internet)
 */
enum class VoiceGender(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    HOMBRE(
        id = "HOMBRE",
        displayName = "Voz de Hombre",
        shortLabel = "Hombre",
        description = "Voz masculina con entonación natural de persona mediante IA (online) o motor local (offline)"
    ),
    MUJER(
        id = "MUJER",
        displayName = "Voz de Mujer",
        shortLabel = "Mujer",
        description = "Voz femenina con entonación natural de persona mediante IA (online) o motor local (offline)"
    );

    companion object {
        fun fromId(id: String): VoiceGender {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MUJER
        }
    }
}

/**
 * Tipo de motor activo en un instante dado.
 */
enum class ActiveEngineType(val label: String) {
    ONLINE("Online Natural (IA)"),
    LOCAL("Local del Dispositivo (Offline)")
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
    val quality: String = "Estándar",
    val gender: VoiceGender = VoiceGender.MUJER
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
    fun setVoiceGender(gender: VoiceGender)
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


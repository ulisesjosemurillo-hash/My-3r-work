package com.example.tts

import org.junit.Assert.*
import org.junit.Test

class TtsModesAndCoordinatorTest {

    @Test
    fun ttsModes_containExpectedLabelsAndDescriptions() {
        assertEquals("Automático", TtsMode.AUTO.label)
        assertEquals("Voz Natural Online", TtsMode.ONLINE_NATURAL.label)
        assertEquals("Voz Local Offline", TtsMode.LOCAL_OFFLINE.label)

        assertTrue(TtsMode.AUTO.description.contains("online con internet"))
        assertTrue(TtsMode.LOCAL_OFFLINE.description.contains("sin conexión"))
    }

    @Test
    fun voiceProfile_distinguishesOfflineAndOnlineCapabilities() {
        val offlineVoice = VoiceProfile(
            id = "es_es_offline_1",
            name = "Español Local (Dispositivo)",
            locale = "es_ES",
            isNetworkRequired = false,
            isNatural = false,
            quality = "Estándar Offline"
        )

        val onlineVoice = VoiceProfile(
            id = "es_neural_natural_1",
            name = "Español Neural (Natural Online)",
            locale = "es_ES",
            isNetworkRequired = true,
            isNatural = true,
            quality = "Alta Fidelidad Neural"
        )

        assertFalse(offlineVoice.isNetworkRequired)
        assertFalse(offlineVoice.isNatural)

        assertTrue(onlineVoice.isNetworkRequired)
        assertTrue(onlineVoice.isNatural)
    }

    @Test
    fun activeEngineType_hasOnlineAndLocalRepresentations() {
        assertEquals("Online Natural", ActiveEngineType.ONLINE.label)
        assertEquals("Local del Dispositivo", ActiveEngineType.LOCAL.label)
    }
}

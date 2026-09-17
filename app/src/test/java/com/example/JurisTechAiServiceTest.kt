package com.example

import com.example.ai.JurisTechAiService
import com.example.model.ChatMessage
import com.example.model.DocumentPage
import com.example.model.SampleDocumentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JurisTechAiServiceTest {

    @Test
    fun testWholeDocumentContextAnalysis() = runBlocking {
        val service = JurisTechAiService()
        val sample = SampleDocumentRepository.samples.first()

        // Test asking about specific clauses across the whole document
        val response = service.responderPreguntaConHistorial(
            pregunta = "¿Cuáles son los honorarios y la penalización por retraso?",
            documentTitle = sample.title,
            paginas = sample.pages,
            paginaActual = 1,
            historialConversacion = emptyList()
        )

        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun testFollowUpQuestionFlow() = runBlocking {
        val service = JurisTechAiService()
        val sample = SampleDocumentRepository.samples.first()

        val history = listOf(
            ChatMessage(
                remitente = "Tú",
                texto = "¿Cuál es el plazo de vigencia del contrato?",
                isUser = true
            ),
            ChatMessage(
                remitente = "JurisTech AI",
                texto = "El contrato tiene una duración de doce (12) meses prorrogables.",
                isUser = false
            )
        )

        // Follow-up question relying on context
        val followUpResponse = service.responderPreguntaConHistorial(
            pregunta = "¿Y con cuánto preaviso debe notificarse si no se desea prorrogar?",
            documentTitle = sample.title,
            paginas = sample.pages,
            paginaActual = 2,
            historialConversacion = history
        )

        assertNotNull(followUpResponse)
        assertTrue(followUpResponse.isNotBlank())
    }
}

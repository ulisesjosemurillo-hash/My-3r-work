package com.example.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JurisTechLearningEngineTest {

    private lateinit var learningEngine: JurisTechLearningEngine

    @Before
    fun setUp() {
        learningEngine = JurisTechLearningEngine()
    }

    @Test
    fun testGenerarPreguntaEvocacionLegalidad() = runBlocking {
        val snippet = "Artículo 1. Principio de Legalidad. Nadie puede ser sancionado por hechos que no estén expresamente definidos como delitos o faltas por ley formal anterior."
        val question = learningEngine.generarPreguntaEvocacion(
            documentTitle = "Código Penal",
            fragmentoTexto = snippet,
            pagina = 1,
            fragmentoId = 0
        )

        assertNotNull(question)
        assertEquals(1, question.pageNumber)
        assertTrue("Concepto debe contener legalidad", question.concept.contains("Legalidad", ignoreCase = true))
        assertTrue("Pregunta debe tener contenido no vacío", question.questionText.isNotBlank())
        assertTrue("Pista socrática no vacía", question.socraticHint.isNotBlank())
    }

    @Test
    fun testEvaluacionSemanticaAceptaComprensionConPalabrasPropias() = runBlocking {
        val snippet = "No hay pena sin dolo o imprudencia. La pena no puede superar la medida de la culpabilidad."
        val question = learningEngine.generarPreguntaEvocacion(
            documentTitle = "Código Penal",
            fragmentoTexto = snippet,
            pagina = 2,
            fragmentoId = 1
        )

        // Respuesta explicada con palabras propias del usuario
        val respuestaUsuario = "Significa que para castigar a alguien tiene que haber actuado con intención deliberada de hacer el daño o con descuido grave, y el juez no puede imponer una pena desproporcionada."

        val eval = learningEngine.evaluarRespuestaSemantica(
            pregunta = question,
            respuestaUsuario = respuestaUsuario,
            fragmentoTexto = snippet,
            documentTitle = "Código Penal"
        )

        assertNotNull(eval)
        assertTrue("Debe considerar comprendido el concepto", eval.isUnderstood)
        assertTrue("Puntuación debe ser alta por equivalencia semántica", eval.scorePercent >= 60)
        assertTrue("Debe citar la página fuente", eval.verifiedCitation.contains("Página 2"))
    }

    @Test
    fun testAlgoritmoRepeticionEspaciadaIntervalos() {
        // Intervalos de la ciencia del aprendizaje: 1 -> 3 -> 7 -> 21 días
        // Prueba inicial: calificación fácil (2)
        val (int1, ease1) = learningEngine.calcularProximoRepaso(
            intervaloActualDias = 1,
            easeFactorActual = 2.5f,
            rating = 2
        )
        assertEquals("De 1 día debe saltar a 3 días", 3, int1)
        assertTrue("Ease factor debe incrementarse", ease1 >= 2.5f)

        // Segundo repaso fácil (2)
        val (int2, ease2) = learningEngine.calcularProximoRepaso(
            intervaloActualDias = 3,
            easeFactorActual = ease1,
            rating = 2
        )
        assertEquals("De 3 días debe saltar a 7 días", 7, int2)

        // Tercer repaso fácil (2)
        val (int3, _) = learningEngine.calcularProximoRepaso(
            intervaloActualDias = 7,
            easeFactorActual = ease2,
            rating = 2
        )
        assertEquals("De 7 días debe saltar a 21 días", 21, int3)

        // Si el usuario falla (rating 0), debe resetear a 1 día
        val (intFail, _) = learningEngine.calcularProximoRepaso(
            intervaloActualDias = 21,
            easeFactorActual = 2.5f,
            rating = 0
        )
        assertEquals("Al fallar, el intervalo debe reiniciar a 1 día", 1, intFail)
    }

    @Test
    fun testGenerarNotasCornellEstructura() = runBlocking {
        val pageText = "LIBRO PRIMERO. PARTE GENERAL. TÍTULO I. DE LA LEY PENAL Y SU APLICACIÓN.\nEl principio de territorialidad se aplica a todos los delitos cometidos en el territorio nacional."
        val note = learningEngine.generarNotasCornell(
            documentTitle = "Código Penal",
            paginaTexto = pageText,
            pagina = 5
        )

        assertNotNull(note)
        assertEquals(5, note.pageNumber)
        assertTrue("Debe tener ideas clave en cues", note.cues.isNotEmpty())
        assertTrue("Debe contener apuntes", note.notes.isNotBlank())
        assertTrue("Debe contener resumen de síntesis", note.summary.isNotBlank())
    }

    @Test
    fun testGenerarCodificacionDualEstructura() = runBlocking {
        val text = "El principio de legalidad exige ley previa. Como excepción, rige la retroactividad de la ley más favorable al reo."
        val dualCoding = learningEngine.generarCodificacionDual(
            documentTitle = "Código Penal",
            texto = text,
            pagina = 1
        )

        assertNotNull(dualCoding)
        assertTrue("Debe contener nodos conceptuales", dualCoding.nodes.isNotEmpty())
        assertTrue("Debe contener relaciones entre nodos", dualCoding.connections.isNotEmpty())
        assertTrue("Debe contener descripción accesible", dualCoding.accessibleTextDescription.isNotBlank())
    }
}

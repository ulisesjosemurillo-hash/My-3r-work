package com.example.model

import java.util.UUID

data class DocumentPage(
    val numero: Int,
    val texto: String
)

data class FragmentoLectura(
    val id: Int,
    val pagina: Int,
    val texto: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val remitente: String,
    val texto: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUser: Boolean = false,
    val referencedPages: List<Int> = emptyList(),
    val isAiThinking: Boolean = false
)

data class SampleDocument(
    val title: String,
    val subtitle: String,
    val pages: List<DocumentPage>
)

object SampleDocumentRepository {

    val samples: List<SampleDocument> = listOf(
        SampleDocument(
            title = "Contrato de Servicios JurisTech AI",
            subtitle = "Acuerdo de Licenciamiento y Procesamiento de Datos",
            pages = listOf(
                DocumentPage(
                    numero = 1,
                    texto = """CONTRATO DE PRESTACIÓN DE SERVICIOS TECNOLÓGICOS E INTELIGENCIA ARTIFICIAL

En Madrid, a 15 de enero de 2025.

REUNIDOS
De una parte, JURISTECH SOLUTIONS S.L., con NIF B-88492019, domiciliada en Paseo de la Castellana 120, Madrid, representada por D. Carlos Mendoza en calidad de Director General.
De otra parte, CORPORACIÓN LEGAL ASOCIADOS S.A., con NIF A-28394012, domiciliada en Calle Serrano 45, Madrid, representada por Dña. Elena Vázquez en calidad de Consejera Delegada.

EXPONEN
I. Que JurisTech es una entidad especializada en el desarrollo de software de inteligencia artificial aplicado al análisis de expedientes judiciales y documentos normativos.
II. Que el Cliente tiene interés en contratar la plataforma JurisTech Reader AI para optimizar los procesos de lectura asistida por voz y consulta contextual de su fondo documental."""
                ),
                DocumentPage(
                    numero = 2,
                    texto = """CLÁUSULAS

PRIMERA. OBJETO DEL CONTRATO
El presente contrato tiene por objeto regular las condiciones conforme a las cuales JurisTech facilitará al Cliente el acceso a la plataforma JurisTech Reader AI en modalidad Software as a Service (SaaS). El servicio incluye la lectura sintética de textos jurídicos, segmentación en fragmentos de lectura y un motor de asistencia cognitiva capaz de resolver dudas jurídicas inmediatas sobre los expedientes cargados.

SEGUNDA. CONDICIONES DE ACCESO Y LICENCIA
JurisTech concede al Cliente una licencia de uso no exclusiva, intransferible y de ámbito territorial europeo. Queda expresamente prohibida la descompilación, ingeniería inversa o comercialización a terceros sin autorización previa por escrito."""
                ),
                DocumentPage(
                    numero = 3,
                    texto = """TERCERA. CONFIDENCIALIDAD Y TRATAMIENTO DE DATOS (RGPD)
Ambas partes se comprometen a guardar estricto secreto profesional respecto a toda la información legal, datos procesales o secretos comerciales a los que tengan acceso. Los documentos procesados por el motor de IA no serán empleados para el reentrenamiento de modelos públicos sin consentimiento expreso.

CUARTA. RESPONSABILIDAD Y GARANTÍAS DE DISPONIBILIDAD
JurisTech garantiza una disponibilidad del servicio del 99.5% mensual, salvo paradas por mantenimiento programado comunicadas con al menos 48 horas de antelación. Las respuestas provistas por el asistente de IA tienen carácter orientativo y no reemplazan el dictamen formal de un letrado colegiado."""
                ),
                DocumentPage(
                    numero = 4,
                    texto = """QUINTA. DURACIÓN Y EXTINCIÓN
El contrato tendrá una vigencia inicial de un año natural a contar desde la fecha de firma, prorrogable tácitamente por periodos anuales salvo preaviso de 30 días naturales.
Cualquiera de las partes podrá resolver el contrato en caso de incumplimiento grave de las obligaciones pactadas.

SEXTA. JURISDICCIÓN Y LEGISLACIÓN APLICABLE
El presente contrato se rige por la legislación española. Para cualquier controversia derivada del mismo, las partes se someten expresamente a la jurisdicción de los Juzgados y Tribunales de la ciudad de Madrid."""
                )
            )
        ),
        SampleDocument(
            title = "Dictamen Jurídico sobre IA y Protección de Datos",
            subtitle = "Evaluación de Impacto según el Reglamento Europeo (RGPD)",
            pages = listOf(
                DocumentPage(
                    numero = 1,
                    texto = """DICTAMEN JURÍDICO 04/2025
SOBRE EL TRATAMIENTO DE DATOS EN SISTEMAS DE LECTURA ASISTIDA POR INTELIGENCIA ARTIFICIAL

ANTECEDENTES
Se solicita análisis jurídico sobre la conformidad legal de la implantación de asistentes de lectura y análisis documental automatizado en despachos profesionales.
El sistema procesa documentos judiciales, contratos y resoluciones administrativas mediante extracción de texto y síntesis por voz."""
                ),
                DocumentPage(
                    numero = 2,
                    texto = """FUNDAMENTOS DE DERECHO

1. PRINCIPIO DE TRANSPARENCIA Y MINIMIZACIÓN (ART. 5 RGPD)
Todo procesamiento automatizado debe limitar el tratamiento a los datos estrictamente necesarios para la formulación de consultas. Se recomienda anonimizar identificadores personales antes de consultar APIs externas.

2. DECISIONES AUTOMATIZADAS Y SUPERVISIÓN HUMANA (ART. 22 RGPD)
Los informes generados por la IA deben requerir siempre validación humana cualificada antes de producir efectos vinculantes frente a clientes o tribunales."""
                ),
                DocumentPage(
                    numero = 3,
                    texto = """CONCLUSIONES Y RECOMENDACIONES
Primera: El uso de lectores inteligentes es plenamente conforme a derecho siempre que se implemente cifrado en tránsito y reposo.
Segunda: Se aconseja registrar el tratamiento en el Registro de Actividades (RAT) de la entidad responsable.
Tercera: Mantener trazabilidad de las consultas realizadas al asistente para garantizar la auditoría forense interna."""
                )
            )
        )
    )

    fun crearFragmentos(paginas: List<DocumentPage>): List<FragmentoLectura> {
        val fragmentos = mutableListOf<FragmentoLectura>()
        var globalIndex = 0

        paginas.forEach { pagina ->
            val palabras = pagina.texto.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val bloquePalabras = mutableListOf<String>()

            for (palabra in palabras) {
                bloquePalabras.add(palabra)
                // Aprox 45 palabras por bloque, igual que en el prototipo
                if (bloquePalabras.size >= 45) {
                    val textoBloque = bloquePalabras.joinToString(" ")
                    fragmentos.add(
                        FragmentoLectura(
                            id = globalIndex++,
                            pagina = pagina.numero,
                            texto = textoBloque
                        )
                    )
                    bloquePalabras.clear()
                }
            }

            if (bloquePalabras.isNotEmpty()) {
                val textoBloque = bloquePalabras.joinToString(" ")
                fragmentos.add(
                    FragmentoLectura(
                        id = globalIndex++,
                        pagina = pagina.numero,
                        texto = textoBloque
                    )
                )
            }
        }

        return fragmentos
    }
}

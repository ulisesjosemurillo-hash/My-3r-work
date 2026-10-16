package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object CodigoProcesalPenalHondurasData {

    val document: SampleDocument = SampleDocument(
        title = "Código Procesal Penal de Honduras",
        subtitle = "Decreto No. 9-99-E - Sistema Acusatorio Oral, Garantías y Medidas Cautelares",
        coverColor = 0xFF2D1E4A, // Piel judicial morado berenjena solemne
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """REPÚBLICA DE HONDURAS
CÓDIGO PROCESAL PENAL
TÍTULO PRELIMINAR: PRINCIPIOS Y GARANTÍAS PROCESALES FUNDAMENTALES

ARTÍCULO 1.- Juicio Previo y Debido Proceso. Nadie podrá ser condenado a una pena ni sometido a una medida de seguridad, sino mediante una sentencia definitiva firme dictada por un tribunal competente e imparcial, conforme a las normas de este Código y con riguroso respeto de las garantías consagradas en la Constitución de la República.

ARTÍCULO 2.- Estado de Inocencia. Todo imputado se presume inocente mientras no se declare su culpabilidad por sentencia judicial ejecutoriada. En caso de duda razonable, se resolverá siempre a favor del imputado (In dubio pro reo).

ARTÍCULO 3.- Derecho a la Defensa Técnica. La defensa del imputado es un derecho inviolable e irrenunciable. Desde el momento de su aprehensión o imputación, tiene derecho a nombrar un defensor de su elección y, a falta de éste, a que se le asigne uno de la Defensa Pública.

ARTÍCULO 6.- Oralidad y Publicidad. El juicio oral y público es la fase central del proceso penal hondureño. Las audiencias son públicas salvo excepciones expresas fundadas en el pudor, orden público o seguridad nacional."""
            ),
            DocumentPage(
                numero = 2,
                texto = """LIBRO SEGUNDO: EL PROCEDIMIENTO ORDINARIO
TÍTULO I: LA ETAPA PREPARATORIA Y DE INVESTIGACIÓN

ARTÍCULO 262.- Dirección de la Investigación. Corresponde al Ministerio Público la dirección técnico-jurídica y funcional de la investigación de los delitos de acción pública, la cual será ejecutada operativamente por la Dirección Policial de Investigaciones (DPI).

ARTÍCULO 280.- La Audiencia Inicial. Dentro del término de ley tras la detención judicial, se celebrará la Audiencia Inicial ante el Juez de Letras, en la cual las partes propondrán y evacuarán medios de prueba idóneos para resolver sobre:
1. El Auto de Formal Procesamiento con Medida Cautelar; o,
2. El Sobreseimiento Definitivo o Provisional a favor del encausado.

ARTÍCULO 173.- Medidas Cautelares Sustitutivas. La prisión preventiva es de carácter excepcional. El órgano jurisdiccional podrá imponer arresto domiciliario, prohibición de salir del país, presentación periódica al tribunal o caución económica cuando aseguren eficazmente la presencia del imputado."""
            ),
            DocumentPage(
                numero = 3,
                texto = """LIBRO TERCERO: DEL JUICIO ORAL Y PÚBLICO
TÍTULO I: DEBATE, ALEGATOS Y SENTENCIA

ARTÍCULO 314.- Apertura del Debate. El Presidente del Tribunal de Sentencia declarará abierto el debate público, advirtiendo al imputado sobre la importancia del acto y concediendo la palabra sucesivamente al Ministerio Público, a la acusación privada y a la defensa técnica para sus alegatos de apertura.

ARTÍCULO 328.- Evacuación y Valoración de la Prueba. Las pruebas periciales, testificales y documentales se reproducirán en presencia inmediata de los jueces (principio de inmediación). Los jueces valorarán las pruebas conforme a las reglas de la Sana Crítica, exponiendo razonadamente el valor otorgado a cada elemento.

ARTÍCULO 338.- Sentencia Absolutoria o Condenatoria. La sentencia contendrá la relación de los hechos probados, la calificación jurídica del delito y la motivación analítica sobre la culpabilidad o absolución del justiciable."""
            )
        )
    )
}

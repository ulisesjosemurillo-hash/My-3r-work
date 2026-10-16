package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object CodigoCivilHondurasData {

    val document: SampleDocument = SampleDocument(
        title = "Código Civil de Honduras",
        subtitle = "Decreto No. 76 - De las Personas, Bienes, Obligaciones y Contratos",
        coverColor = 0xFF1C3A27, // Piel judicial verde bosque clásico
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """REPÚBLICA DE HONDURAS
CÓDIGO CIVIL
TÍTULO PRELIMINAR: DE LA LEY Y SU EFICACIA

ARTÍCULO 1.- La ley es una declaración de la voluntad soberana que, manifestada en la forma prescrita por la Constitución, manda, prohíbe o permite.

ARTÍCULO 2.- La costumbre no constituye derecho sino en los casos en que la ley se remite a ella.

ARTÍCULO 6.- La ignorancia de la ley no sirve de excusa, salvo disposición expresa en contrario.

ARTÍCULO 10.- La ley es obligatoria para todos los habitantes de la República, incluso los extranjeros, y su observancia no puede suspenderse sino por los medios y formas que ella misma determina.

LIBRO PRIMERO: DE LAS PERSONAS Y LA FAMILIA
TÍTULO I: DEL PRINCIPIO Y FIN DE LA EXISTENCIA DE LAS PERSONAS

ARTÍCULO 51.- La persona humana es el fin supremo de la sociedad y del Estado. El principio de la existencia natural de las personas principia con el nacimiento y culmina con la muerte biológica comprobada. La ley protege la vida del que está por nacer."""
            ),
            DocumentPage(
                numero = 2,
                texto = """LIBRO SEGUNDO: DE LOS BIENES Y DE SU DOMINIO, POSESIÓN, USO Y GOCE
TÍTULO I: DE LAS VARIAS CLASES DE BIENES

ARTÍCULO 599.- Los bienes consisten en cosas corporales o incorporales. Corporales son las que tienen un ser real y pueden ser percibidas por los sentidos, como una casa, un libro. Incorporales las que consisten en meros derechos, como los créditos y las servidumbres activas.

ARTÍCULO 600.- Las cosas corporales se dividen en muebles e inmuebles:
1. Muebles son las que pueden transportarse de un lugar a otro, sea moviéndose ellas a sí mismas, sea que sólo se muevan por una fuerza externa.
2. Inmuebles o fincas o bienes raíces son las cosas que no pueden transportarse de un lugar a otro, como las tierras y las minas, y las que se adhieren permanentemente a ellas como los edificios y árboles.

ARTÍCULO 613.- El dominio, que se llama también propiedad, es el derecho de poseer exclusivamente una cosa y gozar y disponer de ella, sin más limitaciones que las establecidas por la ley o por la voluntad del propietario."""
            ),
            DocumentPage(
                numero = 3,
                texto = """LIBRO CUARTO: DE LAS OBLIGACIONES EN GENERAL Y DE LOS CONTRATOS
TÍTULO I: DE LAS OBLIGACIONES Y SUS FUENTES

ARTÍCULO 1346.- Las obligaciones nacen de la ley, de los contratos y cuasicontratos, y de los actos y omisiones ilícitos o en que intervenga cualquier género de culpa o negligencia.

ARTÍCULO 1347.- Las obligaciones que nacen de los contratos tienen fuerza de ley entre las partes contratantes, y deben cumplirse al tenor de los mismos bajo el principio rector de la buena fe contractual (Pacta Sunt Servanda).

ARTÍCULO 1539.- Los contratos se perfeccionan por el mero consentimiento, y desde entonces obligan no sólo al cumplimiento de lo expresamente pactado, sino también a todas las consecuencias que, según su naturaleza, sean conformes a la buena fe, al uso y a la ley.

ARTÍCULO 1552.- No hay contrato sino cuando concurren los requisitos siguientes:
1. Consentimiento de los contratantes;
2. Objeto cierto que sea materia del contrato; y,
3. Causa de la obligación que se establezca."""
            )
        )
    )
}

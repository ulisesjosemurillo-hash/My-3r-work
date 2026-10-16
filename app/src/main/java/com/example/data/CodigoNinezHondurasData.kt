package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object CodigoNinezHondurasData {

    val document: SampleDocument = SampleDocument(
        title = "Código de la Niñez y la Adolescencia",
        subtitle = "Decreto No. 73-96 - Protección Integral, Familia e Interés Superior del Niño",
        coverColor = 0xFF1B4332, // Piel judicial esmeralda noble
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """REPÚBLICA DE HONDURAS
CÓDIGO DE LA NIÑEZ Y LA ADOLESCENCIA
TÍTULO I: DISPOSICIONES GENERALES
CAPÍTULO ÚNICO: PRINCIPIOS FUNDAMENTALES

ARTÍCULO 1.- La presente Ley tiene por objeto la protección integral de los niños en los términos que consagra la Constitución de la República y la Convención sobre los Derechos del Niño.

ARTÍCULO 2.- Para los efectos de este Código se entiende por niño o niña a toda persona menor de dieciocho (18) años de edad. La infancia comprende desde el nacimiento hasta los doce (12) años cumplidos; y la adolescencia, desde los doce (12) hasta haber cumplido los dieciocho (18) años de edad.

ARTÍCULO 4.- Principio del Interés Superior del Niño. En la interpretación y aplicación de este Código, la sociedad y el Estado deberán atender de manera primordial el Interés Superior del Niño, el cual prevalecerá sobre cualquier otro interés legítimo que pudiera concurrir con él."""
            ),
            DocumentPage(
                numero = 2,
                texto = """TÍTULO II: DERECHOS Y LIBERTADES FUNDAMENTALES
CAPÍTULO I: DERECHO A LA VIDA, SALUD Y SEGURIDAD SOCIAL

ARTÍCULO 11.- Los niños tienen derecho a la vida desde el momento de su concepción. El Estado garantizará su supervivencia y su desarrollo armónico e integral dentro del núcleo familiar.

ARTÍCULO 18.- Los niños tienen derecho a un nombre propio, a una nacionalidad y a saber quiénes son sus progenitores desde el momento de su nacimiento, siendo obligatoria e inmediata su inscripción en el Registro Nacional de las Personas (RNP) de forma gratuita.

ARTÍCULO 29.- Los niños tienen derecho a recibir educación gratuita y obligatoria por parte del Estado, al descanso, al esparcimiento, al juego y a participar en la vida cultural y artística de su comunidad."""
            ),
            DocumentPage(
                numero = 3,
                texto = """TÍTULO IV: DE LA JUSTICIA PENAL ESPECIAL PARA LA NIÑEZ
CAPÍTULO I: DEL SISTEMA DE RESPONSABILIDAD PENAL DE ADOLESCENTES

ARTÍCULO 180.- Los menores de doce (12) años están exentos de responsabilidad penal. Cuando se les atribuya la comisión de una infracción serán remitidos a las autoridades de bienestar social de la niñez para su orientación y protección.

ARTÍCULO 181.- Los adolescentes entre doce (12) y dieciocho (18) años que incurran en infracciones tipificadas en la ley penal estarán sujetos a medidas socioeducativas especiales orientadas a su rehabilitación, reintegración familiar y desarrollo integral, primando las medidas no privativas de libertad."""
            )
        )
    )
}

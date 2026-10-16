package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object TratadoDerechosHumanosData {

    val document: SampleDocument = SampleDocument(
        title = "Tratado de Derechos Humanos y Garantías",
        subtitle = "Convención Americana sobre Derechos Humanos (Pacto de San José)",
        coverColor = 0xFF543D2B, // Piel judicial cuero tabaco envejecido
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """ORGANIZACIÓN DE LOS ESTADOS AMERICANOS
CONVENCIÓN AMERICANA SOBRE DERECHOS HUMANOS
(PACTO DE SAN JOSÉ DE COSTA RICA)

PARTE I: DEBERES DE LOS ESTADOS Y DERECHOS PROTEGIDOS
CAPÍTULO I: OBLIGACIÓN DE RESPETAR LOS DERECHOS

ARTÍCULO 1.- Obligación de Respetar los Derechos. Los Estados Partes en esta Convención se comprometen a respetar los derechos y libertades reconocidos en ella y a garantizar su libre y pleno ejercicio a toda persona que esté sujeta a su jurisdicción, sin discriminación alguna por motivos de raza, color, sexo, idioma, religión, opiniones políticas o de cualquier otra índole, origen nacional o social, posición económica, nacimiento o cualquier otra condición social.

ARTÍCULO 2.- Deber de Adoptar Disposiciones de Derecho Interno. Si el ejercicio de los derechos y libertades mencionados en el artículo 1 no estuviere ya garantizado por disposiciones legislativas o de otro carácter, los Estados Partes se comprometen a adoptar las medidas legislativas o de otro carácter que fueren necesarias para hacer efectivos tales derechos y libertades."""
            ),
            DocumentPage(
                numero = 2,
                texto = """CAPÍTULO II: DERECHOS CIVILES Y POLÍTICOS

ARTÍCULO 4.- Derecho a la Vida. Toda persona tiene derecho a que se respete su vida. Este derecho estará protegido por la ley y, en general, a partir del momento de la concepción. Nadie puede ser privado de la vida arbitrariamente.

ARTÍCULO 7.- Derecho a la Libertad Personal. Toda persona tiene derecho a la libertad y a la seguridad personales. Nadie puede ser privado de su libertad física, salvo por las causas y en las condiciones fijadas de antemano por las Constituciones Políticas de los Estados Partes o por las leyes dictadas conforme a ellas. Nadie puede ser sometido a detención o encarcelamiento arbitrarios.

ARTÍCULO 8.- Garantías Judiciales. Toda persona tiene derecho a ser oída, con las debidas garantías y dentro de un plazo razonable, por un juez o tribunal competente, independiente e imparcial, establecido con anterioridad por la ley, en la sustanciación de cualquier acusación penal formulada contra ella."""
            ),
            DocumentPage(
                numero = 3,
                texto = """PARTE II: MEDIOS DE LA PROTECCIÓN
CAPÍTULO VII: LA COMISIÓN INTERAMERICANA DE DERECHOS HUMANOS
CAPÍTULO VIII: LA CORTE INTERAMERICANA DE DERECHOS HUMANOS

ARTÍCULO 62.- Competencia Contenciosa de la Corte. Todo Estado Parte puede, en el momento del depósito de su instrumento de ratificación de esta Convención o en cualquier momento posterior, declarar que reconoce como obligatoria de pleno derecho y sin convención especial, la competencia de la Corte sobre todos los casos relativos a la interpretación o aplicación de esta Convención.

ARTÍCULO 63.- Reparación Integral y Medidas Provisionales. Cuando decida que hubo violación de un derecho o libertad protegidos en esta Convención, la Corte dispondrá que se garantice al lesionado en el goce de su derecho o libertad conculcados. Dispondrá asimismo, si ello fuera procedente, que se reparen las consecuencias de la medida o situación que ha configurado la vulneración de esos derechos y el pago de una justa indemnización a la parte lesionada."""
            )
        )
    )
}

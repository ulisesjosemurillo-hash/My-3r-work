package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object CodigoComercioHondurasData {

    val document: SampleDocument = SampleDocument(
        title = "Código de Comercio de Honduras",
        subtitle = "Decreto No. 73-1950 - Actos de Comercio, Sociedades y Títulos Valores",
        coverColor = 0xFF4A1E28, // Piel judicial burdeos solemne
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """REPÚBLICA DE HONDURAS
CÓDIGO DE COMERCIO
TÍTULO PRELIMINAR: DISPOSICIONES GENERALES

ARTÍCULO 1.- Los comerciantes, los actos de comercio y las cosas mercantiles se regirán por las disposiciones de este Código y de las demás leyes mercantiles; en su defecto, por las costumbres mercantiles generales o locales, y a falta de éstas, por las normas del Código Civil.

ARTÍCULO 2.- Son comerciantes:
I. Las personas naturales titulares de una empresa mercantil;
II. Las sociedades que se constituyan bajo forma mercantil, con calidad de comerciantes colectivos; y,
III. Las sociedades extranjeras y sus sucursales que ejerzan actos de comercio en territorio hondureño.

ARTÍCULO 3.- Son actos de comercio todos los que tengan por objeto crear, transferir, modificar o extinguir con ánimo de lucro relaciones jurídicas comerciales."""
            ),
            DocumentPage(
                numero = 2,
                texto = """LIBRO PRIMERO: DE LOS COMERCIANTES Y SUS AUXILIARES
TÍTULO II: DE LAS SOCIEDADES MERCANTILES

ARTÍCULO 13.- Son sociedades mercantiles:
I. La Sociedad en Nombre Colectivo;
II. La Sociedad en Comandita Simple;
III. La Sociedad de Responsabilidad Limitada (S. de R.L.);
IV. La Sociedad Anónima (S.A.);
V. La Sociedad en Comandita por Acciones; y,
VI. La Sociedad por Acciones Simplificada (S.A.S.).

ARTÍCULO 14.- La escritura constitutiva de toda sociedad mercantil deberá otorgarse ante Notario Público e inscribirse en el Registro Mercantil correspondiente del domicilio social para surtir efectos frente a terceros.

ARTÍCULO 90.- En la Sociedad Anónima el capital social se divide en acciones y la responsabilidad de los socios o accionistas se limita estrictamente al pago de sus aportaciones."""
            ),
            DocumentPage(
                numero = 3,
                texto = """LIBRO TERCERO: DE LAS COSAS MERCANTILES
TÍTULO PRIMERO: DE LOS TÍTULOS VALORES

ARTÍCULO 449.- Son títulos valores los documentos necesarios para ejercitar el derecho literal y autónomo que en ellos se consigna. Pueden ser de crédito, de participación o representativos de mercaderías.

ARTÍCULO 451.- Son títulos valores reconocidos:
1. La Letra de Cambio;
2. El Pagaré;
3. El Cheque;
4. Las Acciones y Bonos; y,
5. El Certificado de Depósito y Bono de Prenda.

ARTÍCULO 452.- La transmisión del título valor comprende no sólo el derecho principal en él incorporado, sino también los derechos de garantía e intereses correspondientes."""
            )
        )
    )
}

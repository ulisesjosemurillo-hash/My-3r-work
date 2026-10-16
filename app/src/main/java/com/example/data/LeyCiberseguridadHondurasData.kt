package com.example.data

import com.example.model.DocumentPage
import com.example.model.SampleDocument

object LeyCiberseguridadHondurasData {

    val document: SampleDocument = SampleDocument(
        title = "Ley de Ciberseguridad y Delitos Informáticos",
        subtitle = "Marco Legal de Protección de Datos, Evidencia Digital y Seguridad en Redes",
        coverColor = 0xFF0D324D, // Piel judicial azul cibertecnológico
        pages = listOf(
            DocumentPage(
                numero = 1,
                texto = """REPÚBLICA DE HONDURAS
LEY DE CIBERSEGURIDAD, PROTECCIÓN DE DATOS Y DELITOS INFORMÁTICOS
TÍTULO I: DISPOSICIONES GENERALES Y DEFINICIONES

ARTÍCULO 1.- Objeto. La presente Ley tiene por objeto establecer el marco normativo integral para la prevención, detección, investigación y sanción de las conductas ilícitas perpetradas a través de tecnologías de la información, sistemas informáticos y redes de telecomunicaciones, así como la tutela efectiva del derecho a la privacidad digital y autodeterminación informativa.

ARTÍCULO 2.- Definiciones Operativas Fundamentales:
a) Sistema Informático: Todo dispositivo aislado o conjunto de dispositivos interconectados que realizan el tratamiento automatizado de datos.
b) Datos Informáticos: Cualquier representación de hechos, información o conceptos en una forma apta para el procesamiento informático.
c) Evidencia Digital: Información de valor probatorio almacenada o transmitida en forma binaria, sujeta a cadena de custodia forense.
d) Phishing o Suplantación: Empleo fraudulento de identidades electrónicas para la obtención indebida de credenciales y datos financieros."""
            ),
            DocumentPage(
                numero = 2,
                texto = """TÍTULO II: DE LOS DELITOS INFORMÁTICOS
CAPÍTULO I: DELITOS CONTRA LA CONFIDENCIALIDAD, INTEGRIDAD Y DISPONIBILIDAD

ARTÍCULO 12.- Acceso Ilícito a Sistemas Informáticos (Hacking). Quien, sin la debida autorización o excediendo la concedida, vulnere medidas de seguridad e ingrese indebidamente a un sistema informático, será sancionado con reclusión de tres a seis años.

ARTÍCULO 15.- Interceptación Ilícita de Telecomunicaciones y Datos. Se sancionará a quien por medios técnicos intercepte sin orden judicial previa transmisiones no públicas de datos informáticos dirigidas a un sistema o emanadas del mismo.

ARTÍCULO 18.- Daño Informático y Ransomware. Quien dolosamente altere, borre, deteriore o secuestre mediante cifrado datos informáticos ajenos exigiendo recompensa o rescate económico, incurrirá en pena de reclusión de seis a diez años y multa proporcional al daño causado."""
            ),
            DocumentPage(
                numero = 3,
                texto = """TÍTULO III: PROTECCIÓN DE DATOS PERSONALES Y CADENA DE CUSTODIA DIGITAL

ARTÍCULO 30.- Principio del Consentimiento Informado. El tratamiento automatizado de datos de carácter personal requerirá el consentimiento inequívoco, libre y explícito de su titular, salvo las excepciones legítimas de interés público previstas en la ley.

ARTÍCULO 35.- Preservación y Cadena de Custodia de Evidencia Digital. Los prestadores de servicios de telecomunicaciones e internet deberán conservar de forma confidencial y segura los datos de tráfico relativos a comunicaciones electrónicas por un período de dos años, a disposición exclusiva del Ministerio Público y órganos jurisdiccionales mediante orden motivada.

ARTÍCULO 42.- Derechos ARCO en el Entorno Digital. Toda persona física o jurídica ostenta el derecho de Acceso, Rectificación, Cancelación y Oposición respecto a sus datos personales almacenados en bases de datos públicas o corporativas."""
            )
        )
    )
}

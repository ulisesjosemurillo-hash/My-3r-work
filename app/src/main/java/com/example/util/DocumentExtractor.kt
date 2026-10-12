package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

/**
 * Extractor unificado para ingesta inteligente de documentos:
 * Soporta PDF (con detección de escaneado), DOCX (Word) y TXT plano.
 */
object DocumentExtractor {

    suspend fun extractDocument(
        context: Context,
        uri: Uri,
        fileName: String,
        onProgress: (current: Int, total: Int, text: String) -> Unit
    ): Result<List<DocumentPage>> = withContext(Dispatchers.IO) {
        val lowerName = fileName.lowercase()
        when {
            lowerName.endsWith(".pdf") -> {
                PdfExtractor.extractTextFromPdf(context, uri, onProgress)
            }
            lowerName.endsWith(".docx") -> {
                extractFromDocx(context, uri, onProgress)
            }
            lowerName.endsWith(".txt") -> {
                extractFromTxt(context, uri, onProgress)
            }
            else -> {
                // Intento primario como PDF y secundario como texto plano
                val pdfResult = PdfExtractor.extractTextFromPdf(context, uri, onProgress)
                if (pdfResult.isSuccess) {
                    pdfResult
                } else {
                    extractFromTxt(context, uri, onProgress)
                }
            }
        }
    }

    /**
     * Extrae texto estructurado de un archivo Microsoft Word (.docx).
     * Un archivo .docx es un archivo ZIP que contiene 'word/document.xml'.
     * Utiliza XmlPullParser estándar de Android para extraer párrafos y títulos preservando orden.
     */
    private suspend fun extractFromDocx(
        context: Context,
        uri: Uri,
        onProgress: (current: Int, total: Int, text: String) -> Unit
    ): Result<List<DocumentPage>> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var zipInput: ZipInputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo DOCX."))

            zipInput = ZipInputStream(inputStream)
            var entry = zipInput.nextEntry
            var foundDocumentXml = false

            val paragraphs = mutableListOf<String>()

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    foundDocumentXml = true
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zipInput, "UTF-8")

                    var eventType = parser.eventType
                    var currentParagraph = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (name == "p") {
                                    currentParagraph = StringBuilder()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                val t = parser.text
                                if (t != null) {
                                    currentParagraph.append(t)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (name == "p") {
                                    val text = currentParagraph.toString().trim()
                                    if (text.isNotBlank()) {
                                        paragraphs.add(text)
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zipInput.nextEntry
            }

            if (!foundDocumentXml || paragraphs.isEmpty()) {
                return@withContext Result.failure(Exception("El archivo DOCX no contiene texto legible o está dañado."))
            }

            // Agrupar párrafos en páginas virtuales (~250 palabras por página)
            val pages = mutableListOf<DocumentPage>()
            var pageIndex = 1
            var pageBuffer = StringBuilder()

            paragraphs.forEachIndexed { idx, p ->
                pageBuffer.append(p).append("\n\n")
                val wordCount = pageBuffer.split(Regex("\\s+")).size
                if (wordCount >= 220 || idx == paragraphs.size - 1) {
                    pages.add(DocumentPage(numero = pageIndex++, texto = pageBuffer.toString().trim()))
                    onProgress(pages.size, paragraphs.size / 4 + 1, "Procesando sección ${pages.size}")
                    pageBuffer = StringBuilder()
                }
            }

            if (pages.isEmpty() && pageBuffer.isNotBlank()) {
                pages.add(DocumentPage(numero = 1, texto = pageBuffer.toString().trim()))
            }

            Result.success(pages)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { zipInput?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extrae texto de un archivo plano .txt con codificación UTF-8.
     */
    private suspend fun extractFromTxt(
        context: Context,
        uri: Uri,
        onProgress: (current: Int, total: Int, text: String) -> Unit
    ): Result<List<DocumentPage>> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("No se pudo leer el archivo de texto."))

            if (content.isBlank()) {
                return@withContext Result.failure(Exception("El archivo de texto está vacío."))
            }

            val blocks = content.split("\n\n").filter { it.isNotBlank() }
            val pages = mutableListOf<DocumentPage>()
            var pageIndex = 1
            var pageBuffer = StringBuilder()

            blocks.forEachIndexed { idx, b ->
                pageBuffer.append(b.trim()).append("\n\n")
                val words = pageBuffer.split(Regex("\\s+")).size
                if (words >= 200 || idx == blocks.size - 1) {
                    pages.add(DocumentPage(numero = pageIndex++, texto = pageBuffer.toString().trim()))
                    onProgress(pages.size, blocks.size / 3 + 1, "Procesando página ${pages.size}")
                    pageBuffer = StringBuilder()
                }
            }

            if (pages.isEmpty()) {
                pages.add(DocumentPage(numero = 1, texto = content.trim()))
            }

            Result.success(pages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

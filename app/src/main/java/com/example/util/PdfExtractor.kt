package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.DocumentPage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object PdfExtractor {

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun extractTextFromPdf(
        context: Context,
        uri: Uri,
        onProgress: (current: Int, total: Int, text: String) -> Unit
    ): Result<List<DocumentPage>> = withContext(Dispatchers.IO) {
        initialize(context)

        var inputStream: InputStream? = null
        var pdDocument: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado."))

            pdDocument = PDDocument.load(inputStream)
            val totalPages = pdDocument.numberOfPages

            if (totalPages <= 0) {
                return@withContext Result.failure(Exception("El archivo PDF no contiene páginas."))
            }

            val pages = mutableListOf<DocumentPage>()
            val stripper = PDFTextStripper()

            for (pageIndex in 1..totalPages) {
                stripper.startPage = pageIndex
                stripper.endPage = pageIndex

                val rawText = try {
                    stripper.getText(pdDocument).trim()
                } catch (e: Exception) {
                    "[No se pudo extraer el texto de esta página.]"
                }

                val pageText = if (rawText.isBlank()) {
                    "[Página sin texto detectable o escaneada como imagen]"
                } else {
                    rawText.replace(Regex("\\s+"), " ")
                }

                pages.add(DocumentPage(numero = pageIndex, texto = pageText))
                onProgress(pageIndex, totalPages, "Procesando página $pageIndex de $totalPages")
            }

            // Validar si al menos una página tuvo texto real
            val hasRealText = pages.any {
                it.texto.isNotBlank() && !it.texto.startsWith("[Página sin texto")
            }

            if (!hasRealText) {
                return@withContext Result.failure(
                    Exception("El documento no contiene texto seleccionable. Puede ser un PDF escaneado (imágenes).")
                )
            }

            Result.success(pages)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                pdDocument?.close()
            } catch (_: Exception) {}
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }
}

package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.DocumentPage
import com.example.model.PdfOutlineNode
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

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
            // Guardar copia local en caché para renderizado de páginas originales
            val cacheFile = File(context.cacheDir, "current_visual_document.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            inputStream = if (cacheFile.exists() && cacheFile.length() > 0) {
                cacheFile.inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado."))

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

    /**
     * Extrae el esquema de marcadores / índice (TOC) embebido en el PDF si existe.
     */
    suspend fun extractPdfBookmarks(context: Context, file: File): List<PdfOutlineNode> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        initialize(context)
        var pdDocument: PDDocument? = null
        try {
            pdDocument = PDDocument.load(file)
            val outline = pdDocument.documentCatalog.documentOutline ?: return@withContext emptyList()
            val nodes = mutableListOf<PdfOutlineNode>()

            var item: PDOutlineItem? = outline.firstChild
            var pageIndex = 1
            while (item != null) {
                val title = item.title?.trim() ?: "Sección"
                // Intentar recuperar número de página del destino
                val destPage = try {
                    val dest = item.destination
                    if (dest != null) {
                        // En PDFBox Android el destino puede asociar página
                        pageIndex++
                    } else pageIndex++
                } catch (_: Exception) {
                    pageIndex++
                }

                nodes.add(
                    PdfOutlineNode(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        pageNumber = destPage.coerceAtLeast(1),
                        fragmentId = 0,
                        level = 0,
                        isAiInferred = false
                    )
                )
                item = item.nextSibling
            }
            nodes
        } catch (e: Exception) {
            emptyList()
        } finally {
            try {
                pdDocument?.close()
            } catch (_: Exception) {}
        }
    }
}

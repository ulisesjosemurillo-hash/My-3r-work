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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object PdfExtractor {

    private var isInitialized = false

    private val CHAPTER_REGEX = Regex(
        "^(LIBRO|T[ÍI]TULO|CAP[ÍI]TULO|SECCI[ÓO]N)\\b.*",
        RegexOption.IGNORE_CASE
    )

    private val ARTICLE_REGEX = Regex(
        "^(ART[ÍI]CULO|ART\\.?)\\s*(\\d+).*",
        RegexOption.IGNORE_CASE
    )

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
        documentId: String = "",
        onProgress: (current: Int, total: Int, text: String) -> Unit
    ): Result<List<DocumentPage>> = withContext(Dispatchers.IO) {
        initialize(context)

        var inputStream: InputStream? = null
        var pdDocument: PDDocument? = null

        val effectiveDocId = if (documentId.isNotBlank()) documentId else "doc_${System.currentTimeMillis()}"
        val pdfTargetFile = DocumentPageStorage.getDocumentPdfFile(context, effectiveDocId)

        try {
            // 1. Guardar copia local del PDF en disco dedicada para este documento
            val tempFile = File(context.cacheDir, "temp_ingest_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                tempFile.delete()
                return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado o está vacío."))
            }

            tempFile.copyTo(pdfTargetFile, overwrite = true)
            tempFile.delete()

            // 2. Cargar con PDDocument.load(File) para acceso paginado sobre disco
            pdDocument = PDDocument.load(pdfTargetFile)
            val totalPages = pdDocument.numberOfPages

            if (totalPages <= 0) {
                return@withContext Result.failure(Exception("El archivo PDF no contiene páginas legibles."))
            }

            // Preparar archivos y directorios de streaming en almacenamiento interno
            val pagesDir = DocumentPageStorage.getPagesDirectory(context)
            val streamingPagesFile = File(pagesDir, "current_extracted_pages.json")
            val targetDocJsonFile = DocumentPageStorage.getDocumentJsonFile(context, effectiveDocId)

            // Lista ligera en memoria: las primeras 50 páginas tienen texto completo (ventana inicial),
            // y las páginas posteriores tienen un snippet para evitar desbordar memoria en archivos de 1,720+ páginas.
            val initialPages = ArrayList<DocumentPage>(minOf(totalPages, 2500))
            val outlineList = mutableListOf<PdfOutlineNode>()
            var hasRealText = false
            var stripper = PDFTextStripper()

            streamingPagesFile.bufferedWriter(Charsets.UTF_8).use { writerGlobal ->
                writerGlobal.write("[")

                // Abrir también el archivo permanente dedicado al documento
                val docWriter = targetDocJsonFile.bufferedWriter(Charsets.UTF_8)
                docWriter.write("[")

                for (pageIndex in 1..totalPages) {
                    // Reiniciar stripper y reconectar PDDocument periódicamente para limpiar buffers y COS streams
                    // acumulados en PDFBox en documentos masivos (ej. 1,720 páginas)
                    if (pageIndex > 1 && pageIndex % 50 == 0) {
                        stripper = PDFTextStripper()
                    }
                    if (pageIndex > 1 && pageIndex % 100 == 0) {
                        try {
                            pdDocument?.close()
                            pdDocument = PDDocument.load(pdfTargetFile)
                            stripper = PDFTextStripper()
                            System.gc()
                        } catch (_: Exception) {}
                    }

                    stripper.startPage = pageIndex
                    stripper.endPage = pageIndex

                    val rawText = try {
                        stripper.getText(pdDocument).trim()
                    } catch (_: Exception) {
                        "[No se pudo extraer el texto de esta página.]"
                    }

                    val pageText = if (rawText.isBlank()) {
                        "[Página sin texto detectable o escaneada como imagen]"
                    } else {
                        hasRealText = true
                        rawText.replace(Regex("\\s+"), " ")
                    }

                    // 1. Guardar página individual en disco en O(1)
                    DocumentPageStorage.saveSinglePage(context, effectiveDocId, pageIndex, pageText)

                    // 2. Escribir en archivo JSON de streaming global y permanente
                    if (pageIndex > 1) {
                        writerGlobal.write(",")
                        docWriter.write(",")
                    }
                    val jsonEntry = "{\"numero\":$pageIndex,\"texto\":${JSONObject.quote(pageText)}}"
                    writerGlobal.write(jsonEntry)
                    docWriter.write(jsonEntry)

                    // 3. Detectar títulos de capítulos y artículos al vuelo durante la extracción
                    if (outlineList.size < 250) {
                        val lines = rawText.split("\n")
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (CHAPTER_REGEX.matches(trimmed)) {
                                outlineList.add(
                                    PdfOutlineNode(
                                        id = UUID.randomUUID().toString(),
                                        title = trimmed.take(65).trim(),
                                        pageNumber = pageIndex,
                                        fragmentId = 0,
                                        level = 1,
                                        isAiInferred = false
                                    )
                                )
                                break
                            } else if (ARTICLE_REGEX.matches(trimmed)) {
                                outlineList.add(
                                    PdfOutlineNode(
                                        id = UUID.randomUUID().toString(),
                                        title = trimmed.take(65).trim(),
                                        pageNumber = pageIndex,
                                        fragmentId = 0,
                                        level = 2,
                                        isAiInferred = false
                                    )
                                )
                                break
                            }
                        }
                    }

                    // 4. Mantener en memoria el objeto DocumentPage:
                    // Ventana inicial (páginas 1..50) tiene texto completo para lectura inmediata.
                    // Páginas posteriores mantienen vista previa ligera (~60 chars) para el selector de páginas sin OOM.
                    val memoryText = if (pageIndex <= 50) pageText else pageText.take(60)
                    initialPages.add(DocumentPage(numero = pageIndex, texto = memoryText))

                    // Notificar progreso periódicamente para mantener fluida la interfaz
                    if (pageIndex == 1 || pageIndex == totalPages || pageIndex % 10 == 0) {
                        onProgress(pageIndex, totalPages, "Procesando página $pageIndex de $totalPages...")
                    }
                }

                writerGlobal.write("]")
                docWriter.write("]")
                docWriter.flush()
                docWriter.close()
            }

            // Si el PDF es predominantemente visual o escaneado sin texto OCR incrustado,
            // no rechazar el documento: permitir lectura visual completa y anotaciones
            if (!hasRealText) {
                initialPages.clear()
                for (p in 1..totalPages) {
                    val fallbackText = "Página $p. [Documento visual escaneado. Disponible para lectura gráfica, zoom y anotaciones]."
                    DocumentPageStorage.saveSinglePage(context, effectiveDocId, p, fallbackText)
                    initialPages.add(DocumentPage(numero = p, texto = fallbackText))
                }
            }

            // Guardar esquema del documento en disco
            val embeddedBookmarks = extractPdfBookmarks(context, pdfTargetFile)
            val finalOutline = if (embeddedBookmarks.isNotEmpty()) embeddedBookmarks else outlineList
            DocumentPageStorage.saveOutline(context, effectiveDocId, finalOutline)

            Result.success(initialPages)
        } catch (e: Throwable) {
            // Rescate con PdfRenderer nativo de Android si el parser de PDFBox encontró alguna anomalía
            try {
                if (pdfTargetFile.exists() && pdfTargetFile.length() > 0L) {
                    val pfd = android.os.ParcelFileDescriptor.open(pdfTargetFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val nativeRenderer = android.graphics.pdf.PdfRenderer(pfd)
                    val pagesCount = nativeRenderer.pageCount
                    nativeRenderer.close()
                    pfd.close()

                    if (pagesCount > 0) {
                        val fallbackPages = mutableListOf<DocumentPage>()
                        for (p in 1..pagesCount) {
                            val txt = "Página $p. [Documento cargado en modo visual. Disponible para lectura y estudio]."
                            DocumentPageStorage.saveSinglePage(context, effectiveDocId, p, txt)
                            fallbackPages.add(DocumentPage(numero = p, texto = txt))
                        }
                        return@withContext Result.success(fallbackPages)
                    }
                }
            } catch (_: Exception) {}
            Result.failure(Exception(e.message ?: "Error al procesar el archivo PDF"))
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
                val destPage = try {
                    val dest = item.destination
                    if (dest != null) {
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

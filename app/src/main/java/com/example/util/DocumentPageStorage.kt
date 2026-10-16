package com.example.util

import android.content.Context
import android.util.JsonReader
import android.util.JsonWriter
import com.example.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Gestor de almacenamiento en disco de alto rendimiento para páginas de documentos extensos (ej. 1,720+ páginas).
 * Evita problemas de OutOfMemory (OOM) al no cargar todas las páginas simultáneamente en memoria RAM.
 * Proporciona:
 * 1. Acceso aleatorio O(1) a páginas individuales mediante archivos dedicados en almacenamiento interno.
 * 2. Carga en ventanas deslizantes (sliding window) para lectura continua y TTS sin interrupciones.
 * 3. Búsqueda y streaming con JsonReader sin desbordamiento de búfer.
 */
object DocumentPageStorage {

    fun getPagesDirectory(context: Context): File {
        val dir = File(context.filesDir, "pages")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDocumentPagesDir(context: Context, documentId: String): File {
        val cleanId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = File(getPagesDirectory(context), "doc_$cleanId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDocumentJsonFile(context: Context, documentId: String): File {
        val cleanId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getPagesDirectory(context), "pages_${cleanId}.json")
    }

    fun getOutlineJsonFile(context: Context, documentId: String): File {
        val cleanId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getPagesDirectory(context), "outline_${cleanId}.json")
    }

    /**
     * Retorna el archivo PDF persistente dedicado para un documento específico.
     * Permite tener múltiples libros en la biblioteca sin colisiones ni sobreescrituras.
     */
    fun getDocumentPdfFile(context: Context, documentId: String): File {
        val cleanId = documentId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfsDir = File(context.filesDir, "pdfs")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()
        return File(pdfsDir, "doc_${cleanId}.pdf")
    }

    /**
     * Elimina todos los archivos en disco asociados a un documento (páginas, JSON, outline y PDF).
     */
    fun deleteDocumentStorage(context: Context, documentId: String) {
        try {
            val pdfFile = getDocumentPdfFile(context, documentId)
            if (pdfFile.exists()) pdfFile.delete()
            val jsonFile = getDocumentJsonFile(context, documentId)
            if (jsonFile.exists()) jsonFile.delete()
            val outlineFile = getOutlineJsonFile(context, documentId)
            if (outlineFile.exists()) outlineFile.delete()
            val pagesDir = getDocumentPagesDir(context, documentId)
            if (pagesDir.exists()) pagesDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    /**
     * Guarda una página individual directamente en disco.
     */
    fun saveSinglePage(context: Context, documentId: String, pageNumber: Int, text: String) {
        try {
            val dir = getDocumentPagesDir(context, documentId)
            val pageFile = File(dir, "p_${pageNumber}.txt")
            pageFile.writeText(text, Charsets.UTF_8)
        } catch (_: Throwable) {}
    }

    /**
     * Lee el texto completo de una página específica desde disco en O(1).
     */
    fun readSinglePage(context: Context, documentId: String, pageNumber: Int): String? {
        val dir = getDocumentPagesDir(context, documentId)
        val pageFile = File(dir, "p_${pageNumber}.txt")
        if (pageFile.exists() && pageFile.length() > 0L) {
            return try {
                pageFile.readText(Charsets.UTF_8)
            } catch (_: Throwable) {
                null
            }
        }

        // Respaldo secundario: extraer del archivo JSON en streaming
        val jsonFile = getDocumentJsonFile(context, documentId)
        if (jsonFile.exists() && jsonFile.length() > 0L) {
            return readSinglePageFromJsonStream(jsonFile, pageNumber)
        }

        return null
    }

    /**
     * Lee únicamente una ventana de páginas (startPage..endPage) en memoria.
     */
    suspend fun readPageWindow(
        context: Context,
        documentId: String,
        startPage: Int,
        endPage: Int
    ): List<DocumentPage> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DocumentPage>()
        val dir = getDocumentPagesDir(context, documentId)

        // Intentar leer de los archivos individuales rápidos
        var allReadFromFiles = true
        for (pageNum in startPage..endPage) {
            val pageFile = File(dir, "p_${pageNum}.txt")
            if (pageFile.exists()) {
                val text = try { pageFile.readText(Charsets.UTF_8) } catch (_: Throwable) { "" }
                result.add(DocumentPage(numero = pageNum, texto = text))
            } else {
                allReadFromFiles = false
                break
            }
        }

        if (allReadFromFiles && result.isNotEmpty()) {
            return@withContext result
        }

        // Si faltaron archivos individuales, leer del JSON en streaming sin cargar todo en RAM
        val jsonFile = getDocumentJsonFile(context, documentId)
        if (jsonFile.exists() && jsonFile.length() > 0L) {
            return@withContext readPageWindowFromJsonStream(jsonFile, startPage, endPage)
        }

        result
    }

    /**
     * Lee una ventana de páginas de un JSON grande usando JsonReader streaming (cero OOM).
     */
    private fun readPageWindowFromJsonStream(file: File, startPage: Int, endPage: Int): List<DocumentPage> {
        val pages = mutableListOf<DocumentPage>()
        try {
            JsonReader(FileReader(file)).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    var pageNum = 0
                    var pageText = ""
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "numero" -> pageNum = reader.nextInt()
                            "texto" -> {
                                if (pageNum in startPage..endPage) {
                                    pageText = reader.nextString()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    if (pageNum in startPage..endPage && pageText.isNotEmpty()) {
                        pages.add(DocumentPage(numero = pageNum, texto = pageText))
                    }
                    if (pageNum > endPage && pages.size >= (endPage - startPage + 1)) {
                        break
                    }
                }
            }
        } catch (_: Throwable) {}
        return pages
    }

    private fun readSinglePageFromJsonStream(file: File, targetPage: Int): String? {
        try {
            JsonReader(FileReader(file)).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    var pageNum = 0
                    var pageText: String? = null
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "numero" -> pageNum = reader.nextInt()
                            "texto" -> {
                                if (pageNum == targetPage) {
                                    pageText = reader.nextString()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (pageNum == targetPage && pageText != null) {
                        return pageText
                    }
                    if (pageNum > targetPage) break
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    /**
     * Guarda el outline (TOC) en disco para no tener que regenerarlo en RAM.
     */
    fun saveOutline(context: Context, documentId: String, outlineNodes: List<com.example.model.PdfOutlineNode>) {
        try {
            val file = getOutlineJsonFile(context, documentId)
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("[")
                outlineNodes.forEachIndexed { idx, node ->
                    if (idx > 0) writer.write(",")
                    writer.write(
                        "{\"id\":${JSONObject.quote(node.id)}," +
                        "\"title\":${JSONObject.quote(node.title)}," +
                        "\"pageNumber\":${node.pageNumber}," +
                        "\"level\":${node.level}}"
                    )
                }
                writer.write("]")
            }
        } catch (_: Throwable) {}
    }

    /**
     * Carga el outline (TOC) previamente extraído desde disco.
     */
    fun readOutline(context: Context, documentId: String): List<com.example.model.PdfOutlineNode> {
        val file = getOutlineJsonFile(context, documentId)
        if (!file.exists()) return emptyList()
        val nodes = mutableListOf<com.example.model.PdfOutlineNode>()
        try {
            JsonReader(FileReader(file)).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    var id = java.util.UUID.randomUUID().toString()
                    var title = ""
                    var pageNumber = 1
                    var level = 1
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextString()
                            "title" -> title = reader.nextString()
                            "pageNumber" -> pageNumber = reader.nextInt()
                            "level" -> level = reader.nextInt()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (title.isNotBlank()) {
                        nodes.add(
                            com.example.model.PdfOutlineNode(
                                id = id,
                                title = title,
                                pageNumber = pageNumber,
                                fragmentId = 0,
                                level = level,
                                isAiInferred = false
                            )
                        )
                    }
                }
                reader.endArray()
            }
        } catch (_: Throwable) {}
        return nodes
    }

    /**
     * Itera en streaming sobre todas las páginas del documento sin acumularlas en memoria RAM.
     * Invoca [onPage] para cada página procesada.
     */
    fun streamAllPages(context: Context, documentId: String, onPage: (pageNumber: Int, text: String) -> Unit) {
        val jsonFile = getDocumentJsonFile(context, documentId)
        if (jsonFile.exists() && jsonFile.length() > 0L) {
            try {
                JsonReader(FileReader(jsonFile)).use { reader ->
                    reader.beginArray()
                    while (reader.hasNext()) {
                        var pageNum = 0
                        var pageText = ""
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "numero" -> pageNum = reader.nextInt()
                                "texto" -> pageText = reader.nextString()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        if (pageNum > 0 && pageText.isNotBlank()) {
                            onPage(pageNum, pageText)
                        }
                    }
                    reader.endArray()
                }
                return
            } catch (_: Throwable) {}
        }

        // Respaldo secundario: iterar sobre archivos individuales p_X.txt
        val dir = getDocumentPagesDir(context, documentId)
        val files = dir.listFiles { f -> f.name.startsWith("p_") && f.name.endsWith(".txt") }
        if (files != null && files.isNotEmpty()) {
            files.sortedBy { f -> f.name.removePrefix("p_").removeSuffix(".txt").toIntOrNull() ?: 0 }
                .forEach { file ->
                    val pageNum = file.name.removePrefix("p_").removeSuffix(".txt").toIntOrNull() ?: 0
                    if (pageNum > 0) {
                        val text = try { file.readText(Charsets.UTF_8) } catch (_: Throwable) { "" }
                        if (text.isNotBlank()) {
                            onPage(pageNum, text)
                        }
                    }
                }
        }
    }
}

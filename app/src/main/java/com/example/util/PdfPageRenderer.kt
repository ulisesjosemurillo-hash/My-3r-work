package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renderizador de páginas de PDF de alto rendimiento usando la API nativa de Android [PdfRenderer].
 * Genera representaciones Bitmap fieles al diseño original (gráficos, márgenes, tipografías e imágenes),
 * con caché LRU en memoria. Si no hay archivo físico disponible (ej. documentos muestra), genera una
 * página visual estructurada tipo libro editorial.
 */
class PdfPageRenderer(private val context: Context) {

    private val tag = "PdfPageRenderer"
    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var activePdfFile: File? = null

    // Caché LRU en memoria para un desplazamiento fluido entre páginas (10 páginas en RAM)
    private val bitmapCache = object : LruCache<Int, Bitmap>(10) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            // No reciclamos inmediatamente si Compose puede seguir mostrándolo
        }
    }

    /**
     * Carga un PDF desde un archivo local en disco.
     */
    fun openFile(file: File): Boolean {
        close()
        return try {
            activePdfFile = file
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pfd!!)
            bitmapCache.evictAll()
            Log.d(tag, "PDFRenderer inicializado con éxito: ${pdfRenderer?.pageCount} páginas.")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error abriendo archivo PDF para renderizado", e)
            close()
            false
        }
    }

    /**
     * Copia un Uri de ContentResolver a la caché interna y lo inicializa en PdfRenderer.
     */
    suspend fun openFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "current_visual_document.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            openFile(cacheFile)
        } catch (e: Exception) {
            Log.e(tag, "Error copiando PDF desde Uri a caché", e)
            false
        }
    }

    /**
     * Total de páginas del PDF visualmente cargado.
     */
    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    /**
     * Renderiza la página indicada (1-based index) a un Bitmap de alta resolución.
     */
    suspend fun renderPage(pageNumber: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        val cached = bitmapCache.get(pageNumber)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        val renderer = pdfRenderer
        if (renderer != null && pageCount > 0) {
            val zeroIndex = (pageNumber - 1).coerceIn(0, pageCount - 1)
            try {
                val page = renderer.openPage(zeroIndex)
                val width = targetWidth
                val height = ((targetWidth.toFloat() / page.width) * page.height).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                bitmapCache.put(pageNumber, bitmap)
                return@withContext bitmap
            } catch (e: Exception) {
                Log.e(tag, "Error renderizando página $pageNumber con PdfRenderer nativo", e)
            }
        }

        null
    }

    /**
     * Genera una página visual editorial (estilo libro o compendio jurídico impreso)
     * a partir del texto y metadatos, garantizando que todo documento tenga vista visual original.
     */
    fun generateBookPageBitmap(
        documentTitle: String,
        pageNumber: Int,
        pageText: String,
        width: Int = 1080,
        height: Int = 1520
    ): Bitmap {
        val cached = bitmapCache.get(pageNumber)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo papel crema suave tipo libro editorial (#FDFBF7)
        canvas.drawColor(Color.parseColor("#FDFBF7"))

        // Borde fino de página
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0DCD3")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), borderPaint)

        // Encabezado de página
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#795548")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(documentTitle.take(45), 70f, 90f, headerPaint)

        // Línea divisoria de encabezado
        val linePaint = Paint().apply {
            color = Color.parseColor("#D7CCC8")
            strokeWidth = 2f
        }
        canvas.drawLine(70f, 110f, (width - 70).toFloat(), 110f, linePaint)

        // Texto del cuerpo estilo editorial justificado
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 32f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        var currentY = 170f
        val marginX = 75f
        val maxTextWidth = (width - 150).toFloat()

        val lines = pageText.split("\n")
        for (rawLine in lines) {
            if (currentY > height - 120) break

            val line = rawLine.trim()
            if (line.isBlank()) {
                currentY += 24f
                continue
            }

            // Detectar si es título o artículo para darle estilo
            val isHeading = line.startsWith("ART") || line.startsWith("CAP") || line.startsWith("TÍT") || line.startsWith("LIBRO")
            if (isHeading) {
                textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textPaint.color = Color.parseColor("#1B5E20")
                currentY += 16f
            } else {
                textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                textPaint.color = Color.parseColor("#212121")
            }

            // Word wrap
            val words = line.split(" ")
            val currentLineWords = StringBuilder()

            for (word in words) {
                val testLine = if (currentLineWords.isEmpty()) word else "$currentLineWords $word"
                val measuredWidth = textPaint.measureText(testLine)

                if (measuredWidth > maxTextWidth) {
                    canvas.drawText(currentLineWords.toString(), marginX, currentY, textPaint)
                    currentY += 46f
                    currentLineWords.clear()
                    currentLineWords.append(word)
                    if (currentY > height - 120) break
                } else {
                    currentLineWords.clear()
                    currentLineWords.append(testLine)
                }
            }

            if (currentLineWords.isNotEmpty() && currentY <= height - 120) {
                canvas.drawText(currentLineWords.toString(), marginX, currentY, textPaint)
                currentY += 46f
            }
        }

        // Pie de página con número
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val footerText = "— Página $pageNumber —"
        val footerWidth = footerPaint.measureText(footerText)
        canvas.drawText(footerText, (width - footerWidth) / 2f, (height - 65).toFloat(), footerPaint)

        bitmapCache.put(pageNumber, bitmap)
        return bitmap
    }

    /**
     * Cierra y libera los recursos del descriptor y renderizador de Android.
     */
    fun close() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            pfd?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        pfd = null
        bitmapCache.evictAll()
    }
}

package com.example.util

import com.example.model.*
import java.util.UUID

/**
 * Analizador semántico y estructural de contenido de libros y documentos legales en PDF.
 * Convierte texto de páginas en bloques organizados por jerarquía:
 * Capítulos, Secciones, Artículos, Listas, Tablas estructuradas, Citas y Notas al pie.
 */
object PdfStructureParser {

    private val CHAPTER_REGEX = Regex(
        "^(LIBRO|T[ÍI]TULO|CAP[ÍI]TULO|SECCI[ÓO]N)\\b.*",
        RegexOption.IGNORE_CASE
    )

    private val ARTICLE_REGEX = Regex(
        "^(ART[ÍI]CULO|ART\\.?)\\s*(\\d+).*",
        RegexOption.IGNORE_CASE
    )

    private val LIST_BULLET_REGEX = Regex(
        "^([•\\-\\*]|\\d+[\\.\\)]|[a-zA-Z][\\.\\)])\\s+(.*)"
    )

    private val FOOTNOTE_REGEX = Regex(
        "^(\\[\\d+\\]|\\*+|\\d+\\.|Nota:)\\s*(.*)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parsea una lista de [DocumentPage] en una lista de [PdfPageStructure] y [StructuredBlock].
     */
    fun parseDocument(
        documentId: String,
        pages: List<DocumentPage>
    ): Pair<List<PdfPageStructure>, List<StructuredBlock>> {
        val pageStructures = mutableListOf<PdfPageStructure>()
        val allBlocks = mutableListOf<StructuredBlock>()
        var globalFragmentId = 0
        var currentSectionTitle = "Introducción"

        pages.forEach { page ->
            val pageNum = page.numero
            val pageText = page.texto.trim()

            // Detección de página escaneada o sin texto
            val isScanned = pageText.isBlank() ||
                    pageText.startsWith("[Página sin texto detectable") ||
                    pageText.length < 25 && !pageText.any { it.isLetter() }

            if (isScanned) {
                val scannedBlock = StructuredBlock(
                    id = UUID.randomUUID().toString(),
                    documentId = documentId,
                    pageNumber = pageNum,
                    fragmentId = globalFragmentId++,
                    type = BlockType.SCANNED_PAGE_ALERT,
                    level = 0,
                    text = "[Página $pageNum escaneada como imagen. Texto no seleccionable detectado.]",
                    spokenText = "Página $pageNum escaneada. Requiere procesamiento óptico de caracteres para lectura completa.",
                    isAiInferred = false,
                    confidence = 0.2f
                )
                val pageStruct = PdfPageStructure(
                    pageNumber = pageNum,
                    isScannedImage = true,
                    blocks = listOf(scannedBlock),
                    confidenceScore = 0.2f
                )
                pageStructures.add(pageStruct)
                allBlocks.add(scannedBlock)
                return@forEach
            }

            // Separar el texto en párrafos o líneas significativas
            val rawParagraphs = splitIntoSemanticParagraphs(pageText)
            val pageBlocks = mutableListOf<StructuredBlock>()
            val detectedHeadersOnPage = mutableListOf<String>()

            var i = 0
            while (i < rawParagraphs.size) {
                val p = rawParagraphs[i].trim()
                if (p.isBlank()) {
                    i++
                    continue
                }

                // 1. Detección de Tablas en texto (líneas con | o \t o encabezados delimitados)
                val tableCheck = tryParseTable(rawParagraphs, i)
                if (tableCheck != null) {
                    val (table, consumedCount) = tableCheck
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.TABLE,
                        level = 0,
                        text = table.headers.joinToString(" | ") + "\n" +
                                table.rows.joinToString("\n") { it.joinToString(" | ") },
                        spokenText = table.toAccessibleSpokenText(),
                        tableData = table,
                        isAiInferred = false,
                        confidence = 0.95f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i += consumedCount
                    continue
                }

                // 2. Detección de Capítulos / Títulos Principales
                if (CHAPTER_REGEX.matches(p) || isProbableChapterHeading(p)) {
                    currentSectionTitle = p
                    detectedHeadersOnPage.add(p)
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.CHAPTER_HEADER,
                        level = 1,
                        text = p,
                        spokenText = "$p.",
                        isAiInferred = !CHAPTER_REGEX.matches(p),
                        confidence = if (CHAPTER_REGEX.matches(p)) 1.0f else 0.85f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 3. Detección de Artículos Legales
                if (ARTICLE_REGEX.matches(p)) {
                    currentSectionTitle = p.take(60)
                    detectedHeadersOnPage.add(p.take(40))
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.ARTICLE_HEADER,
                        level = 2,
                        text = p,
                        spokenText = "$p.",
                        isAiInferred = false,
                        confidence = 0.98f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 4. Detección de Sección / Subencabezados
                if (isSubheading(p)) {
                    detectedHeadersOnPage.add(p)
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.SECTION_HEADER,
                        level = 2,
                        text = p,
                        spokenText = "Sección: $p.",
                        isAiInferred = true,
                        confidence = 0.85f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 5. Detección de Notas al Pie (antes de listas para evitar que notas con asteriscos o [1] se confundan con viñetas)
                val isFootnoteCandidate = FOOTNOTE_REGEX.matches(p) || p.startsWith("Nota:") || p.startsWith("* Nota") || p.startsWith("[Nota")
                if (isFootnoteCandidate) {
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.FOOTNOTE,
                        level = 0,
                        text = p,
                        spokenText = "Nota al pie: $p",
                        isAiInferred = false,
                        confidence = 0.90f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 6. Detección de Citas Textuales
                if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("«") && p.endsWith("»"))) {
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.QUOTE,
                        level = 0,
                        text = p,
                        spokenText = "Cita textual: $p",
                        isAiInferred = false,
                        confidence = 0.95f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 7. Detección de Elementos de Lista
                val listMatch = LIST_BULLET_REGEX.find(p)
                if (listMatch != null) {
                    val bullet = listMatch.groupValues[1]
                    val content = listMatch.groupValues[2]
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.LIST_ITEM,
                        level = 0,
                        text = p,
                        spokenText = "Punto: $content",
                        isAiInferred = false,
                        confidence = 0.95f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                    i++
                    continue
                }

                // 8. Párrafo Regular de Lectura
                // Si el párrafo es muy extenso (> 500 caracteres), lo dividimos en oraciones / unidades cómodas para lectura y resaltado
                val subParagraphs = splitLongParagraph(p)
                for (subP in subParagraphs) {
                    val block = StructuredBlock(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageNumber = pageNum,
                        fragmentId = globalFragmentId++,
                        type = BlockType.PARAGRAPH,
                        level = 0,
                        text = subP,
                        spokenText = subP,
                        isAiInferred = false,
                        confidence = 1.0f,
                        orderIndex = pageBlocks.size
                    )
                    pageBlocks.add(block)
                }
                i++
            }

            val pageStruct = PdfPageStructure(
                pageNumber = pageNum,
                isScannedImage = false,
                blocks = pageBlocks,
                detectedHeaders = detectedHeadersOnPage,
                confidenceScore = 1.0f
            )
            pageStructures.add(pageStruct)
            allBlocks.addAll(pageBlocks)
        }

        return Pair(pageStructures, allBlocks)
    }

    /**
     * Construye un árbol de marcadores / índice (TOC) a partir de los bloques detectados.
     */
    fun buildOutlineTree(blocks: List<StructuredBlock>): List<PdfOutlineNode> {
        val headerBlocks = blocks.filter {
            it.type == BlockType.DOCUMENT_TITLE ||
                    it.type == BlockType.CHAPTER_HEADER ||
                    it.type == BlockType.SECTION_HEADER ||
                    it.type == BlockType.ARTICLE_HEADER
        }

        if (headerBlocks.isEmpty()) {
            // Si no hay encabezados explícitos, agrupar por páginas cada 5 páginas
            val byPages = blocks.groupBy { it.pageNumber }
            return byPages.map { (pageNum, bList) ->
                val firstSnippet = bList.firstOrNull()?.text?.take(40) ?: "Página $pageNum"
                PdfOutlineNode(
                    title = "Página $pageNum: $firstSnippet",
                    pageNumber = pageNum,
                    fragmentId = bList.firstOrNull()?.fragmentId ?: 0,
                    level = 0,
                    isAiInferred = true
                )
            }
        }

        val result = mutableListOf<PdfOutlineNode>()
        headerBlocks.forEach { hb ->
            result.add(
                PdfOutlineNode(
                    id = hb.id,
                    title = hb.text.take(65).trim(),
                    pageNumber = hb.pageNumber,
                    fragmentId = hb.fragmentId,
                    level = hb.level,
                    isAiInferred = hb.isAiInferred
                )
            )
        }
        return result
    }

    private fun splitIntoSemanticParagraphs(text: String): List<String> {
        val lines = text.split("\n")
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                if (current.isNotBlank()) {
                    paragraphs.add(current.toString().trim())
                    current.clear()
                }
            } else {
                val isHeaderLike = CHAPTER_REGEX.matches(trimmed) || ARTICLE_REGEX.matches(trimmed)
                val isTable = trimmed.contains("|") || trimmed.contains("\t")
                val isList = LIST_BULLET_REGEX.matches(trimmed)
                val isFootnote = FOOTNOTE_REGEX.matches(trimmed)
                val isQuote = (trimmed.startsWith("«") && trimmed.endsWith("»")) || (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                val isSpecial = isHeaderLike || isTable || isList || isFootnote || isQuote

                if (isSpecial) {
                    if (current.isNotBlank()) {
                        paragraphs.add(current.toString().trim())
                        current.clear()
                    }
                    paragraphs.add(trimmed)
                } else {
                    if (current.isNotEmpty()) current.append(" ")
                    current.append(trimmed)
                }
            }
        }

        if (current.isNotBlank()) {
            paragraphs.add(current.toString().trim())
        }

        return paragraphs
    }

    private fun splitLongParagraph(paragraph: String): List<String> {
        if (paragraph.length <= 400) return listOf(paragraph)

        val words = paragraph.split(Regex("\\s+"))
        val chunks = mutableListOf<String>()
        val current = mutableListOf<String>()

        for (word in words) {
            current.add(word)
            if (current.size >= 45 && (word.endsWith(".") || word.endsWith(";") || word.endsWith(":"))) {
                chunks.add(current.joinToString(" "))
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            if (chunks.isNotEmpty() && current.size < 15) {
                // Unir al último si es muy corto
                chunks[chunks.size - 1] = chunks.last() + " " + current.joinToString(" ")
            } else {
                chunks.add(current.joinToString(" "))
            }
        }

        return if (chunks.isEmpty()) listOf(paragraph) else chunks
    }

    private fun isProbableChapterHeading(text: String): Boolean {
        if (text.length in 5..80 && text == text.uppercase() && !text.endsWith(".")) {
            return true
        }
        return false
    }

    private fun isSubheading(text: String): Boolean {
        if (text.length in 5..70 && text.firstOrNull()?.isUpperCase() == true &&
            (text.endsWith(":") || (!text.endsWith(".") && !text.contains(",")))
        ) {
            return true
        }
        return false
    }

    private fun tryParseTable(lines: List<String>, startIndex: Int): Pair<StructuredTable, Int>? {
        val firstLine = lines.getOrNull(startIndex) ?: return null
        val isTableLine = firstLine.contains("|") || firstLine.contains("\t")

        if (!isTableLine) return null

        val tableLines = mutableListOf<String>()
        var idx = startIndex
        while (idx < lines.size) {
            val line = lines[idx].trim()
            if (line.contains("|") || line.contains("\t")) {
                tableLines.add(line)
                idx++
            } else {
                break
            }
        }

        if (tableLines.size < 2) return null

        val delimiter = if (tableLines[0].contains("|")) "|" else "\t"
        val rawHeader = tableLines[0].split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
        if (rawHeader.size < 2) return null

        // Ignorar filas de separador markdown como |---|---|
        val dataRows = mutableListOf<List<String>>()
        for (r in 1 until tableLines.size) {
            val rowLine = tableLines[r]
            if (rowLine.replace(Regex("[-|:\\s]"), "").isEmpty()) continue
            val cells = rowLine.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
            if (cells.isNotEmpty()) {
                dataRows.add(cells)
            }
        }

        if (dataRows.isEmpty()) return null

        val structuredTable = StructuredTable(
            headers = rawHeader,
            rows = dataRows,
            caption = ""
        )
        return Pair(structuredTable, tableLines.size)
    }
}

package com.example.data.local

import com.example.model.BookBookmark
import com.example.model.DocumentPage
import com.example.model.FragmentOpinion
import com.example.model.SampleDocumentRepository
import com.example.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RecentDocumentRepository(
    private val dao: RecentDocumentDao,
    val highlightDao: HighlightDao? = null
) {

    val recentDocuments: Flow<List<RecentDocumentEntity>> = dao.getAllRecentDocuments()

    suspend fun saveOrUpdateDocument(
        id: String,
        title: String,
        subtitle: String = "",
        pages: List<DocumentPage>,
        totalFragments: Int,
        lastPage: Int = 1,
        lastFragmentIndex: Int = 0,
        sourceType: String = "SAMPLE",
        customCoverUri: String? = null,
        coverColor: Long? = null,
        highlightsJson: String = "[]",
        opinionsJson: String = "[]",
        bookmarksJson: String = "[]",
        audioCommentsJson: String = "[]",
        questionsHistoryJson: String = "[]",
        positionHistoryJson: String = "[]"
    ) = withContext(Dispatchers.IO) {
        val pagesJson = serializePages(pages)
        val entity = RecentDocumentEntity(
            id = id,
            title = title,
            subtitle = subtitle,
            totalPages = pages.size,
            totalFragments = totalFragments,
            lastPage = lastPage,
            lastFragmentIndex = lastFragmentIndex,
            lastAccessedTimestamp = System.currentTimeMillis(),
            pagesJson = pagesJson,
            sourceType = sourceType,
            customCoverUri = customCoverUri,
            coverColor = coverColor,
            highlightsJson = highlightsJson,
            opinionsJson = opinionsJson,
            bookmarksJson = bookmarksJson,
            audioCommentsJson = audioCommentsJson,
            questionsHistoryJson = questionsHistoryJson,
            positionHistoryJson = positionHistoryJson
        )
        dao.insertOrUpdate(entity)
    }

    suspend fun updateReadingProgress(
        id: String,
        lastPage: Int,
        lastFragmentIndex: Int
    ) = withContext(Dispatchers.IO) {
        dao.updateReadingProgress(id, lastPage, lastFragmentIndex, System.currentTimeMillis())
    }

    suspend fun renameDocument(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        dao.updateTitle(id, newTitle.trim())
    }

    suspend fun updateMetadata(
        id: String,
        title: String,
        author: String,
        materia: String,
        year: String,
        description: String,
        collection: String,
        tags: List<String>
    ) = withContext(Dispatchers.IO) {
        val tagsArray = JSONArray()
        tags.forEach { tagsArray.put(it) }
        dao.updateMetadata(id, title.trim(), author.trim(), materia.trim(), year.trim(), description.trim(), collection.trim(), tagsArray.toString())
    }

    suspend fun updateGeneralNotes(id: String, generalNotes: String) = withContext(Dispatchers.IO) {
        dao.updateGeneralNotes(id, generalNotes)
    }

    suspend fun updateCover(id: String, customCoverUri: String?, coverColor: Long?) = withContext(Dispatchers.IO) {
        dao.updateCover(id, customCoverUri, coverColor)
    }

    suspend fun updateHighlights(id: String, highlights: Set<Int>) = withContext(Dispatchers.IO) {
        val json = serializeHighlights(highlights)
        dao.updateHighlights(id, json)
    }

    suspend fun updateHighlightItems(id: String, items: List<com.example.model.HighlightItem>) = withContext(Dispatchers.IO) {
        val json = serializeHighlightItems(items)
        dao.updateHighlights(id, json)
    }

    fun getHighlightsFlow(documentId: String): Flow<List<Highlight>>? {
        return highlightDao?.getHighlightsByDocument(documentId)
    }

    suspend fun saveHighlight(highlight: Highlight) = withContext(Dispatchers.IO) {
        highlightDao?.insertHighlight(highlight)
    }

    suspend fun saveHighlights(highlights: List<Highlight>) = withContext(Dispatchers.IO) {
        highlightDao?.insertHighlights(highlights)
    }

    suspend fun removeHighlightById(id: String) = withContext(Dispatchers.IO) {
        highlightDao?.deleteHighlightById(id)
    }

    suspend fun removeHighlightsByFragment(documentId: String, fragmentId: Int) = withContext(Dispatchers.IO) {
        highlightDao?.deleteHighlightsByFragment(documentId, fragmentId)
    }

    suspend fun getHighlightsByDocument(documentId: String): List<Highlight> = withContext(Dispatchers.IO) {
        highlightDao?.getHighlightsByDocumentSync(documentId) ?: emptyList()
    }

    suspend fun updateOpinions(id: String, opinions: List<FragmentOpinion>) = withContext(Dispatchers.IO) {
        val json = serializeOpinions(opinions)
        dao.updateOpinions(id, json)
    }

    suspend fun updateBookmarks(id: String, bookmarks: List<BookBookmark>) = withContext(Dispatchers.IO) {
        val json = serializeBookmarks(bookmarks)
        dao.updateBookmarks(id, json)
    }

    suspend fun updateAudioComments(id: String, audioComments: List<com.example.model.AudioComment>) = withContext(Dispatchers.IO) {
        val json = serializeAudioComments(audioComments)
        dao.updateAudioComments(id, json)
    }

    suspend fun updateQuestionsHistory(id: String, questions: List<com.example.model.ChatMessage>) = withContext(Dispatchers.IO) {
        val json = serializeQuestions(questions)
        dao.updateQuestionsHistory(id, json)
    }

    suspend fun updatePositionHistory(id: String, history: List<com.example.model.PositionHistoryItem>) = withContext(Dispatchers.IO) {
        val json = serializePositionHistory(history)
        dao.updatePositionHistory(id, json)
    }

    suspend fun getDocumentById(id: String): RecentDocumentEntity? = withContext(Dispatchers.IO) {
        dao.getDocumentById(id)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun searchInDocuments(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()
        val docs = dao.getAllDocumentsList()

        docs.forEach { doc ->
            // Title match
            if (doc.title.lowercase().contains(cleanQuery)) {
                results.add(
                    SearchResultItem(
                        documentId = doc.id,
                        documentTitle = doc.title,
                        pageNumber = doc.lastPage,
                        fragmentIndex = doc.lastFragmentIndex,
                        matchedSnippet = doc.title,
                        isTitleMatch = true
                    )
                )
            }

            // In-text matches inside pages
            val pages = parsePages(doc.pagesJson)
            val fragments = SampleDocumentRepository.crearFragmentos(pages)

            fragments.forEachIndexed { idx, frag ->
                if (frag.texto.lowercase().contains(cleanQuery)) {
                    // Extract snippet around match
                    val lowerText = frag.texto.lowercase()
                    val matchIndex = lowerText.indexOf(cleanQuery)
                    val start = maxOf(0, matchIndex - 30)
                    val end = minOf(frag.texto.length, matchIndex + cleanQuery.length + 50)
                    val snippet = "..." + frag.texto.substring(start, end).trim() + "..."

                    results.add(
                        SearchResultItem(
                            documentId = doc.id,
                            documentTitle = doc.title,
                            pageNumber = frag.pagina,
                            fragmentIndex = idx,
                            matchedSnippet = snippet,
                            isTitleMatch = false
                        )
                    )
                }
            }
        }
        results
    }

    fun serializePages(pages: List<DocumentPage>): String {
        val jsonArray = JSONArray()
        pages.forEach { page ->
            jsonArray.put(JSONObject().apply {
                put("numero", page.numero)
                put("texto", page.texto)
            })
        }
        return jsonArray.toString()
    }

    fun parsePages(pagesJson: String): List<DocumentPage> {
        val result = mutableListOf<DocumentPage>()
        try {
            val arr = JSONArray(pagesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    DocumentPage(
                        numero = obj.optInt("numero", i + 1),
                        texto = obj.optString("texto", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return result
    }

    fun serializeHighlights(highlights: Set<Int>): String {
        val arr = JSONArray()
        highlights.forEach { arr.put(it) }
        return arr.toString()
    }

    fun parseHighlights(json: String?): Set<Int> {
        if (json.isNullOrBlank()) return emptySet()
        val set = mutableSetOf<Int>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val item = arr.get(i)
                if (item is Int) {
                    set.add(item)
                } else if (item is JSONObject) {
                    set.add(item.optInt("fragmentId", 0))
                }
            }
        } catch (_: Exception) {}
        return set
    }

    fun serializeHighlightItems(items: List<com.example.model.HighlightItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("documentId", item.documentId)
                put("pageNumber", item.pageNumber)
                put("fragmentId", item.fragmentId)
                put("text", item.text)
                put("startOffset", item.startOffset)
                put("endOffset", item.endOffset)
                put("colorHex", item.colorHex)
                put("isUnderline", item.isUnderline)
                put("timestamp", item.timestamp)
            })
        }
        return arr.toString()
    }

    fun parseHighlightItems(json: String?): List<com.example.model.HighlightItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<com.example.model.HighlightItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val item = arr.get(i)
                if (item is JSONObject) {
                    list.add(
                        com.example.model.HighlightItem(
                            id = item.optString("id", java.util.UUID.randomUUID().toString()),
                            documentId = item.optString("documentId", ""),
                            pageNumber = item.optInt("pageNumber", 1),
                            fragmentId = item.optInt("fragmentId", 0),
                            text = item.optString("text", ""),
                            startOffset = item.optInt("startOffset", 0),
                            endOffset = item.optInt("endOffset", 0),
                            colorHex = item.optString("colorHex", "#FFF59D"),
                            isUnderline = item.optBoolean("isUnderline", false),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                } else if (item is Int) {
                    list.add(
                        com.example.model.HighlightItem(
                            id = java.util.UUID.randomUUID().toString(),
                            fragmentId = item,
                            colorHex = "#FFF59D",
                            isUnderline = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeOpinions(opinions: List<FragmentOpinion>): String {
        val arr = JSONArray()
        opinions.forEach { op ->
            arr.put(JSONObject().apply {
                put("id", op.id)
                put("documentId", op.documentId)
                put("fragmentId", op.fragmentId)
                put("pagina", op.pagina)
                put("textoFragmento", op.textoFragmento)
                put("opinion", op.opinion)
                put("tag", op.tag)
                put("timestamp", op.timestamp)
                put("selectedText", op.selectedText)
                put("startOffset", op.startOffset)
                put("endOffset", op.endOffset)
            })
        }
        return arr.toString()
    }

    fun parseOpinions(json: String?): List<FragmentOpinion> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<FragmentOpinion>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val textoFrag = obj.optString("textoFragmento", "")
                list.add(
                    FragmentOpinion(
                        id = obj.optString("id", i.toString()),
                        documentId = obj.optString("documentId", ""),
                        fragmentId = obj.optInt("fragmentId", 0),
                        pagina = obj.optInt("pagina", 1),
                        textoFragmento = textoFrag,
                        opinion = obj.optString("opinion", ""),
                        tag = obj.optString("tag", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        selectedText = obj.optString("selectedText", textoFrag),
                        startOffset = obj.optInt("startOffset", 0),
                        endOffset = obj.optInt("endOffset", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeBookmarks(bookmarks: List<BookBookmark>): String {
        val arr = JSONArray()
        bookmarks.forEach { bm ->
            arr.put(JSONObject().apply {
                put("id", bm.id)
                put("documentId", bm.documentId)
                put("fragmentId", bm.fragmentId)
                put("pagina", bm.pagina)
                put("snippet", bm.snippet)
                put("timestamp", bm.timestamp)
            })
        }
        return arr.toString()
    }

    fun parseBookmarks(json: String?): List<BookBookmark> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<BookBookmark>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookBookmark(
                        id = obj.optString("id", i.toString()),
                        documentId = obj.optString("documentId", ""),
                        fragmentId = obj.optInt("fragmentId", 0),
                        pagina = obj.optInt("pagina", 1),
                        snippet = obj.optString("snippet", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeAudioComments(audioComments: List<com.example.model.AudioComment>): String {
        val arr = JSONArray()
        audioComments.forEach { ac ->
            arr.put(JSONObject().apply {
                put("id", ac.id)
                put("documentId", ac.documentId)
                put("fragmentId", ac.fragmentId)
                put("pagina", ac.pagina)
                put("snippet", ac.snippet)
                put("audioPath", ac.audioPath)
                put("durationSeconds", ac.durationSeconds)
                put("timestamp", ac.timestamp)
                put("selectedText", ac.selectedText)
                put("startOffset", ac.startOffset)
                put("endOffset", ac.endOffset)
            })
        }
        return arr.toString()
    }

    fun parseAudioComments(json: String?): List<com.example.model.AudioComment> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<com.example.model.AudioComment>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val snip = obj.optString("snippet", "")
                list.add(
                    com.example.model.AudioComment(
                        id = obj.optString("id", i.toString()),
                        documentId = obj.optString("documentId", ""),
                        fragmentId = obj.optInt("fragmentId", 0),
                        pagina = obj.optInt("pagina", 1),
                        snippet = snip,
                        audioPath = obj.optString("audioPath", ""),
                        durationSeconds = obj.optInt("durationSeconds", 0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        selectedText = obj.optString("selectedText", snip),
                        startOffset = obj.optInt("startOffset", 0),
                        endOffset = obj.optInt("endOffset", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializePositionHistory(history: List<com.example.model.PositionHistoryItem>): String {
        val arr = JSONArray()
        history.takeLast(50).forEach { item ->
            arr.put(JSONObject().apply {
                put("fragmentIndex", item.fragmentIndex)
                put("pageNumber", item.pageNumber)
                put("snippet", item.snippet)
                put("timestamp", item.timestamp)
            })
        }
        return arr.toString()
    }

    fun parsePositionHistory(json: String?): List<com.example.model.PositionHistoryItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<com.example.model.PositionHistoryItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    com.example.model.PositionHistoryItem(
                        fragmentIndex = obj.optInt("fragmentIndex", 0),
                        pageNumber = obj.optInt("pageNumber", 1),
                        snippet = obj.optString("snippet", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeQuestions(questions: List<com.example.model.ChatMessage>): String {
        val arr = JSONArray()
        questions.takeLast(30).forEach { q ->
            arr.put(JSONObject().apply {
                put("id", q.id)
                put("remitente", q.remitente)
                put("texto", q.texto)
                put("timestamp", q.timestamp)
                put("isUser", q.isUser)
                put("referencedFragmentIndex", q.referencedFragmentIndex)
                val pgs = JSONArray()
                q.referencedPages.forEach { pgs.put(it) }
                put("referencedPages", pgs)
            })
        }
        return arr.toString()
    }

    fun parseQuestions(json: String?): List<com.example.model.ChatMessage> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<com.example.model.ChatMessage>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pgs = mutableListOf<Int>()
                val pgsArr = obj.optJSONArray("referencedPages")
                if (pgsArr != null) {
                    for (p in 0 until pgsArr.length()) {
                        pgs.add(pgsArr.getInt(p))
                    }
                }
                list.add(
                    com.example.model.ChatMessage(
                        id = obj.optString("id", i.toString()),
                        remitente = obj.optString("remitente", "Usuario"),
                        texto = obj.optString("texto", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isUser = obj.optBoolean("isUser", false),
                        referencedPages = pgs,
                        referencedFragmentIndex = obj.optInt("referencedFragmentIndex", -1)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}


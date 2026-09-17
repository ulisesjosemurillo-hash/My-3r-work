package com.example.data.local

import com.example.model.DocumentPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RecentDocumentRepository(private val dao: RecentDocumentDao) {

    val recentDocuments: Flow<List<RecentDocumentEntity>> = dao.getAllRecentDocuments()

    suspend fun saveOrUpdateDocument(
        id: String,
        title: String,
        subtitle: String = "",
        pages: List<DocumentPage>,
        totalFragments: Int,
        lastPage: Int = 1,
        lastFragmentIndex: Int = 0,
        sourceType: String = "SAMPLE"
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
            sourceType = sourceType
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

    suspend fun getDocumentById(id: String): RecentDocumentEntity? = withContext(Dispatchers.IO) {
        dao.getDocumentById(id)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
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
}

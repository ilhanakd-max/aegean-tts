package com.example.dogalseslikitap.data

import android.content.Context
import com.example.dogalseslikitap.data.db.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository that persists books locally without any network or annotation processors.
 */
class BookRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("books_repo", Context.MODE_PRIVATE)
    private val booksFlow = MutableStateFlow(loadBooks())

    fun getBooks(): Flow<List<BookEntity>> = booksFlow.asStateFlow()

    suspend fun addBook(entity: BookEntity): Long {
        val nextId = (booksFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        val newBook = entity.copy(id = nextId)
        saveBooks(booksFlow.value + newBook)
        return nextId
    }

    suspend fun updateBook(entity: BookEntity) {
        val updated = booksFlow.value.map { if (it.id == entity.id) entity else it }
        saveBooks(updated)
    }

    suspend fun getBook(id: Long): BookEntity? = booksFlow.value.firstOrNull { it.id == id }

    private fun saveBooks(list: List<BookEntity>) {
        booksFlow.value = list
        val json = JSONArray()
        list.forEach { book ->
            val obj = JSONObject()
            obj.put("id", book.id)
            obj.put("title", book.title)
            obj.put("path", book.path)
            obj.put("type", book.type)
            obj.put("lastPosition", book.lastPosition)
            json.put(obj)
        }
        prefs.edit().putString("books", json.toString()).apply()
    }

    private fun loadBooks(): List<BookEntity> {
        val raw = prefs.getString("books", "[]") ?: "[]"
        val array = JSONArray(raw)
        val list = mutableListOf<BookEntity>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optLong("id", 0L)
            val title = obj.optString("title")
            val path = obj.optString("path")
            val type = obj.optString("type")
            val lastPosition = obj.optInt("lastPosition", 0)
            list.add(BookEntity(id = id, title = title, path = path, type = type, lastPosition = lastPosition))
        }
        return list
    }
}

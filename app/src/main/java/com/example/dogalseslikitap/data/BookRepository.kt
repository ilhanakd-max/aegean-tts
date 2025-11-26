package com.example.dogalseslikitap.data

import android.content.Context
import com.example.dogalseslikitap.data.db.AppDatabase
import com.example.dogalseslikitap.data.db.BookEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository that persists books using Room.
 */
class BookRepository(context: Context) {
    private val bookDao = AppDatabase.getInstance(context).bookDao()

    fun getBooks(): Flow<List<BookEntity>> = bookDao.getBooks()

    suspend fun addBook(entity: BookEntity): Long = bookDao.insert(entity)

    suspend fun updateBook(entity: BookEntity) = bookDao.update(entity)

    suspend fun getBook(id: Long): BookEntity? = bookDao.getBookById(id)
}

package com.example.dogalseslikitap.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.data.BookRepository
import com.example.dogalseslikitap.data.db.BookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BookRepository(application)

    val books = repository.getBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBook(entity: BookEntity) {
        viewModelScope.launch {
            repository.addBook(entity)
        }
    }
}

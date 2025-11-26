package com.example.dogalseslikitap.data.db

data class BookEntity(
    val id: Long = 0,
    val title: String,
    val path: String,
    val type: String,
    val lastPosition: Int = 0
)

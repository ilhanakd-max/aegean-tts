package com.example.dogalseslikitap.model

enum class BookType(val extension: String) {
    EPUB("epub"),
    PDF("pdf"),
    TXT("txt");

    companion object {
        fun fromPath(path: String): BookType = when {
            path.lowercase().endsWith(".epub") -> EPUB
            path.lowercase().endsWith(".pdf") -> PDF
            else -> TXT
        }
    }
}

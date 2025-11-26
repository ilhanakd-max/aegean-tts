package com.example.dogalseslikitap.util

import android.content.Context
import android.net.Uri
import com.mertakdut.Reader
import com.mertakdut.exception.OutOfPagesException
import com.mertakdut.BookSection
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Helper that converts selected book files to plain text for rendering and TTS.
 */
object BookContentLoader {
    fun loadText(context: Context, uri: Uri, mimeType: String?): String {
        return when {
            mimeType?.contains("epub") == true || uri.toString().endsWith(".epub") -> loadEpub(context, uri)
            mimeType?.contains("pdf") == true || uri.toString().endsWith(".pdf") -> loadPdf(context, uri)
            else -> loadTxt(context, uri)
        }
    }

    private fun loadTxt(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: ""
    }

    private fun loadPdf(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        inputStream.use {
            PDDocument.load(it).use { doc ->
                val stripper = PDFTextStripper()
                return stripper.getText(doc)
            }
        }
    }

    private fun loadEpub(context: Context, uri: Uri): String {
        val tempFile = copyToCache(context, uri, "temp_epub.epub")
        val reader = Reader().apply {
            setMaxContentPerSection(1500)
            setIsIncludingTextContent(true)
            setFullContent(tempFile.path)
        }
        val builder = StringBuilder()
        var index = 0
        var keepReading = true
        while (keepReading) {
            try {
                val section: BookSection = reader.readSection(index)
                builder.append(section.sectionTextContent).append("\n\n")
                index++
            } catch (e: OutOfPagesException) {
                keepReading = false
            }
        }
        return builder.toString()
    }

    private fun copyToCache(context: Context, uri: Uri, name: String): File {
        val file = File(context.cacheDir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                copyStream(input, output)
            }
        }
        return file
    }

    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(8 * 1024)
        var length: Int
        while (true) {
            length = input.read(buffer)
            if (length <= 0) break
            output.write(buffer, 0, length)
        }
    }
}

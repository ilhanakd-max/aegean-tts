package com.example.dogalseslikitap.util

import android.content.Context
import android.net.Uri
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubReader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import java.io.InputStreamReader

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
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        inputStream.use { stream ->
            val book = EpubReader().readEpub(stream)
            val builder = StringBuilder()
            // Iterate in spine order to keep reading flow coherent
            for (spineRef in book.spine.spineReferences) {
                val resource = spineRef.resource
                val textContent = readResourceText(resource)
                if (textContent.isNotBlank()) {
                    builder.append(textContent.trim()).append("\n\n")
                }
            }
            return builder.toString()
        }
    }

    private fun readResourceText(resource: Resource): String {
        return try {
            val encoding = resource.inputEncoding ?: "UTF-8"
            resource.inputStream.use { resStream ->
                InputStreamReader(resStream, encoding).use { reader ->
                    val html = reader.readText()
                    // Strip HTML tags to get plain text for display/TTS
                    Jsoup.parse(html).text()
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
}

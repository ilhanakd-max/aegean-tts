package com.example.dogalseslikitap.ui.reader

import android.app.Application
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.data.BookRepository
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.data.db.BookEntity
import com.example.dogalseslikitap.tts.TtsProvider
import com.example.dogalseslikitap.tts.TtsSettings
import com.example.dogalseslikitap.util.BookContentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val bookRepository = BookRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _content = MutableStateFlow(SpannableString(""))
    val content: StateFlow<Spannable> = _content

    private val _bookTitle = MutableStateFlow("")
    val bookTitle: StateFlow<String> = _bookTitle

    private val _progressText = MutableStateFlow("")
    val progressText: StateFlow<String> = _progressText

    private val _currentSettings = MutableStateFlow(TtsSettings())
    val currentSettings: StateFlow<TtsSettings> = _currentSettings

    private var book: BookEntity? = null
    private var sentences: List<String> = emptyList()
    private var currentIndex = 0

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            book = bookRepository.getBook(bookId)
            book?.let { entity ->
                _bookTitle.value = entity.title
                currentIndex = entity.lastPosition
                loadSettings()
                val text = withContext(Dispatchers.IO) {
                    val uri = Uri.parse(entity.path)
                    val mime = getApplication<Application>().contentResolver.getType(uri)
                    BookContentLoader.loadText(getApplication(), uri, mime)
                }
                sentences = text.split(Regex("(?<=[.!?])"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                updateContent()
            }
        }
    }

    private suspend fun loadSettings() {
        val prefs = settingsRepository.settingsFlow.first()
        _currentSettings.value = TtsSettings(
            provider = TtsProvider.fromValue(prefs[SettingsRepository.KEY_PROVIDER]),
            speed = prefs[SettingsRepository.KEY_SPEED] ?: 1.0f,
            pitch = prefs[SettingsRepository.KEY_PITCH] ?: 1.0f,
            voice = prefs[SettingsRepository.KEY_VOICE] ?: "",
            openAiKey = prefs[SettingsRepository.KEY_OPENAI_KEY] ?: "",
            openAiBase = prefs[SettingsRepository.KEY_OPENAI_BASE] ?: "https://api.openai.com/",
            azureKey = prefs[SettingsRepository.KEY_AZURE_KEY] ?: "",
            azureRegion = prefs[SettingsRepository.KEY_AZURE_REGION] ?: "https://<region>.tts.speech.microsoft.com/"
        )
    }

    private fun updateContent() {
        if (sentences.isEmpty()) {
            _content.value = SpannableString(getApplication<Application>().getString(R.string.empty_content))
            _progressText.value = ""
            return
        }
        if (currentIndex >= sentences.size) currentIndex = sentences.lastIndex
        val joined = sentences.joinToString(". ")
        val spannable = SpannableString(joined)
        val start = sentences.take(currentIndex).joinToString(". ").length + if (currentIndex > 0) 2 else 0
        val end = start + sentences[currentIndex].length
        val highlightColor = getApplication<Application>().getColor(R.color.md_theme_primary)
        if (start in 0..spannable.length && end <= spannable.length) {
            spannable.setSpan(BackgroundColorSpan(highlightColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        _content.value = spannable
        _progressText.value = getApplication<Application>().getString(
            R.string.progress_format,
            currentIndex + 1,
            sentences.size
        )
    }

    fun nextSentence() {
        if (currentIndex < sentences.lastIndex) {
            currentIndex++
            updateContent()
            saveProgress()
        }
    }

    fun previousSentence() {
        if (currentIndex > 0) {
            currentIndex--
            updateContent()
            saveProgress()
        }
    }

    fun currentSentence(): String = sentences.getOrElse(currentIndex) { "" }

    fun saveProgress() {
        viewModelScope.launch {
            book?.let {
                bookRepository.updateBook(it.copy(lastPosition = currentIndex))
            }
        }
    }
}

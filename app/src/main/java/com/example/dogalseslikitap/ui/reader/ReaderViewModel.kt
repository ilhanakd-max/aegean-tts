package com.example.dogalseslikitap.ui.reader

import android.app.Application
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.data.BookRepository
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.data.db.BookEntity
import com.example.dogalseslikitap.tts.TtsManager
import com.example.dogalseslikitap.tts.TtsSettings
import com.example.dogalseslikitap.util.BookContentLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val bookRepository = BookRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val ttsManager = TtsManager()

    private val _content = MutableStateFlow(SpannableString(""))
    val content: StateFlow<Spannable> = _content

    private val _bookTitle = MutableStateFlow("")
    val bookTitle: StateFlow<String> = _bookTitle

    private val _progressText = MutableStateFlow("")
    val progressText: StateFlow<String> = _progressText

    private var book: BookEntity? = null
    private var sentences: List<String> = emptyList()
    private var ranges: List<IntRange> = emptyList()
    private var normalizedText: String = ""
    private var currentIndex = 0

    val currentSettings: StateFlow<TtsSettings> = settingsRepository.settingsFlow
        .map { prefs ->
            TtsSettings(
                selectedVoice = prefs[SettingsRepository.KEY_VOICE] ?: "",
                rate = prefs[SettingsRepository.KEY_SPEED] ?: 1.0f,
                pitch = prefs[SettingsRepository.KEY_PITCH] ?: 1.0f,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TtsSettings())

    init {
        viewModelScope.launch {
            ttsManager.initialize(getApplication())
            applySettings(currentSettings.value)
        }
        viewModelScope.launch {
            currentSettings.collect { applySettings(it) }
        }
    }

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            book = bookRepository.getBook(bookId)
            book?.let { entity ->
                _bookTitle.value = entity.title
                currentIndex = entity.lastPosition
                val text = withContext(Dispatchers.IO) {
                    val uri = Uri.parse(entity.path)
                    val mime = getApplication<Application>().contentResolver.getType(uri)
                    BookContentLoader.loadText(getApplication(), uri, mime)
                }
                parseText(text)
            }
        }
    }

    private fun parseText(raw: String) {
        normalizedText = raw.replace("\\s+".toRegex(), " ").trim()
        if (normalizedText.isEmpty()) {
            _content.value = SpannableString(getApplication<Application>().getString(R.string.empty_content))
            _progressText.value = ""
            sentences = emptyList()
            ranges = emptyList()
            return
        }
        val regex = Regex("[^.!?]+[.!?]?")
        val matches = regex.findAll(normalizedText)
            .mapNotNull {
                val sentence = it.value.trim()
                if (sentence.isEmpty()) null else sentence to it.range
            }
            .toList()
        sentences = matches.map { it.first }
        ranges = matches.map { it.second }
        if (currentIndex >= sentences.size) currentIndex = sentences.lastIndex
        updateContent()
    }

    private fun updateContent() {
        if (sentences.isEmpty() || ranges.isEmpty()) {
            _content.value = SpannableString(getApplication<Application>().getString(R.string.empty_content))
            _progressText.value = ""
            return
        }
        val spannable = SpannableString(normalizedText)
        val range = ranges.getOrElse(currentIndex) { ranges.last() }
        val color = ContextCompat.getColor(getApplication(), R.color.md_theme_primary)
        val end = (range.last + 1).coerceAtMost(spannable.length)
        if (range.first in 0..spannable.length && end <= spannable.length) {
            spannable.setSpan(BackgroundColorSpan(color), range.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        _content.value = spannable
        _progressText.value = getApplication<Application>().getString(
            R.string.progress_format,
            currentIndex + 1,
            sentences.size,
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

    fun speakCurrentSentence(autoAdvance: Boolean, onError: (Throwable) -> Unit = {}) {
        val text = currentSentence()
        if (text.isBlank()) return
        val settings = currentSettings.value
        applySettings(settings)
        ttsManager.speak(
            text,
            onDone = {
                saveProgress()
                if (autoAdvance) {
                    nextSentence()
                }
            },
            onError = onError,
        )
    }

    fun stopSpeech() = ttsManager.stop()

    fun pauseSpeech() = ttsManager.pause()

    fun resumeSpeech() = ttsManager.resume()

    fun saveProgress() {
        viewModelScope.launch {
            book?.let {
                bookRepository.updateBook(it.copy(lastPosition = currentIndex))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    private fun applySettings(settings: TtsSettings) {
        ttsManager.setVoice(settings.selectedVoice)
        ttsManager.setRate(settings.rate)
        ttsManager.setPitch(settings.pitch)
    }
}

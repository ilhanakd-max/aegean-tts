package com.example.dogalseslikitap.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.tts.TtsManager
import com.example.dogalseslikitap.tts.TtsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val ttsManager = TtsManager()

    val settings: StateFlow<TtsSettings> = repository.settingsFlow
        .map { prefs ->
            TtsSettings(
                selectedVoice = prefs[SettingsRepository.KEY_VOICE] ?: "",
                rate = prefs[SettingsRepository.KEY_SPEED] ?: 1.0f,
                pitch = prefs[SettingsRepository.KEY_PITCH] ?: 1.0f,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TtsSettings())

    private val _voices = MutableStateFlow<List<android.speech.tts.Voice>>(emptyList())
    val voices: StateFlow<List<android.speech.tts.Voice>> = _voices

    private val _loadingVoices = MutableStateFlow(false)
    val loadingVoices: StateFlow<Boolean> = _loadingVoices

    init {
        viewModelScope.launch {
            ttsManager.initialize(getApplication())
            refreshVoices()
            applyCurrentSettings()
        }
    }

    fun refreshVoices() {
        viewModelScope.launch {
            _loadingVoices.value = true
            _voices.value = ttsManager.getVoices()
            _loadingVoices.value = false
        }
    }

    fun save(settings: TtsSettings) {
        viewModelScope.launch {
            repository.setString(SettingsRepository.KEY_VOICE, settings.selectedVoice)
            repository.setFloat(SettingsRepository.KEY_SPEED, settings.rate)
            repository.setFloat(SettingsRepository.KEY_PITCH, settings.pitch)
            ttsManager.setVoice(settings.selectedVoice)
            ttsManager.setRate(settings.rate)
            ttsManager.setPitch(settings.pitch)
        }
    }

    suspend fun testSpeech(text: String, settings: TtsSettings, onError: (Throwable) -> Unit) {
        ttsManager.setVoice(settings.selectedVoice)
        ttsManager.setRate(settings.rate)
        ttsManager.setPitch(settings.pitch)
        try {
            ttsManager.speak(text)
        } catch (t: Throwable) {
            onError(t)
        }
    }

    private suspend fun applyCurrentSettings() {
        val current = settings.value
        ttsManager.setVoice(current.selectedVoice)
        ttsManager.setRate(current.rate)
        ttsManager.setPitch(current.pitch)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

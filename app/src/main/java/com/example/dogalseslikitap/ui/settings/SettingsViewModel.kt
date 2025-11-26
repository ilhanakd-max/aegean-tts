package com.example.dogalseslikitap.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.tts.TtsManager
import com.example.dogalseslikitap.tts.TtsSettings
import com.example.dogalseslikitap.tts.VoiceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val ttsManager = TtsManager(application)

    val settings: StateFlow<TtsSettings> = repository.settingsFlow
        .map { prefs ->
            TtsSettings(
                voiceName = prefs[SettingsRepository.KEY_VOICE] ?: "",
                speed = prefs[SettingsRepository.KEY_SPEED] ?: 1.0f,
                pitch = prefs[SettingsRepository.KEY_PITCH] ?: 1.0f
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TtsSettings())

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val voices: StateFlow<List<VoiceOption>> = _voices

    private val _loadingVoices = MutableStateFlow(false)
    val loadingVoices: StateFlow<Boolean> = _loadingVoices

    init {
        viewModelScope.launch { refreshVoices() }
    }

    fun save(settings: TtsSettings) {
        viewModelScope.launch {
            repository.setString(SettingsRepository.KEY_VOICE, settings.voiceName)
            repository.setFloat(SettingsRepository.KEY_SPEED, settings.speed)
            repository.setFloat(SettingsRepository.KEY_PITCH, settings.pitch)
        }
    }

    fun refreshVoices() {
        viewModelScope.launch {
            _loadingVoices.value = true
            _voices.value = ttsManager.listVoices()
            _loadingVoices.value = false
        }
    }

    suspend fun testSpeech(text: String, settings: TtsSettings, onError: (Throwable) -> Unit) {
        ttsManager.speak(
            text = text,
            settings = settings,
            onDone = {},
            onError = onError
        )
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

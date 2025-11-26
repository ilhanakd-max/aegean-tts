package com.example.dogalseslikitap.ui.settings

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.tts.EngineOption
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
                enginePackage = prefs[SettingsRepository.KEY_ENGINE] ?: TextToSpeech.Engine.DEFAULT_ENGINE,
                voiceName = prefs[SettingsRepository.KEY_VOICE] ?: "",
                speed = prefs[SettingsRepository.KEY_SPEED] ?: 1.0f,
                pitch = prefs[SettingsRepository.KEY_PITCH] ?: 1.0f
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TtsSettings())

    private val _engines = MutableStateFlow<List<EngineOption>>(emptyList())
    val engines: StateFlow<List<EngineOption>> = _engines

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val voices: StateFlow<List<VoiceOption>> = _voices

    private val _loadingVoices = MutableStateFlow(false)
    val loadingVoices: StateFlow<Boolean> = _loadingVoices

    init {
        viewModelScope.launch {
            _engines.value = ttsManager.installedEngines()
            refreshVoices(settings.value.enginePackage)
        }
    }

    fun save(settings: TtsSettings) {
        viewModelScope.launch {
            repository.setString(SettingsRepository.KEY_ENGINE, settings.enginePackage)
            repository.setString(SettingsRepository.KEY_VOICE, settings.voiceName)
            repository.setFloat(SettingsRepository.KEY_SPEED, settings.speed)
            repository.setFloat(SettingsRepository.KEY_PITCH, settings.pitch)
        }
    }

    fun refreshVoices(enginePackage: String) {
        viewModelScope.launch {
            _loadingVoices.value = true
            val engine = enginePackage.ifBlank { TextToSpeech.Engine.DEFAULT_ENGINE }
            _voices.value = ttsManager.loadVoiceOptions(engine)
            _loadingVoices.value = false
        }
    }

    fun rhvoicePackage(): String? = _engines.value.firstOrNull { it.packageName.contains("rhvoice", true) }?.packageName

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

package com.example.dogalseslikitap.ui.settings

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogalseslikitap.data.SettingsRepository
import com.example.dogalseslikitap.tts.TtsProvider
import com.example.dogalseslikitap.tts.TtsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _settings = MutableStateFlow(TtsSettings())
    val settings: StateFlow<TtsSettings> = _settings

    init {
        viewModelScope.launch {
            val prefs = repository.settingsFlow.first()
            _settings.value = mapPrefsToSettings(prefs)
        }
    }

    fun save(settings: TtsSettings) {
        viewModelScope.launch {
            repository.setString(SettingsRepository.KEY_PROVIDER, settings.provider.value)
            repository.setFloat(SettingsRepository.KEY_SPEED, settings.speed)
            repository.setFloat(SettingsRepository.KEY_PITCH, settings.pitch)
            repository.setString(SettingsRepository.KEY_VOICE, settings.voice)
            repository.setString(SettingsRepository.KEY_OPENAI_KEY, settings.openAiKey)
            repository.setString(SettingsRepository.KEY_OPENAI_BASE, settings.openAiBase)
            repository.setString(SettingsRepository.KEY_AZURE_KEY, settings.azureKey)
            repository.setString(SettingsRepository.KEY_AZURE_REGION, settings.azureRegion)
            _settings.value = settings
        }
    }

    private fun mapPrefsToSettings(prefs: Preferences): TtsSettings {
        return TtsSettings(
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
}

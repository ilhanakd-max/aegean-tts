package com.example.dogalseslikitap.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reader_settings")

/**
 * Stores TTS settings and API keys using DataStore.
 */
class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_PROVIDER = stringPreferencesKey("tts_provider")
        val KEY_SPEED = floatPreferencesKey("tts_speed")
        val KEY_PITCH = floatPreferencesKey("tts_pitch")
        val KEY_VOICE = stringPreferencesKey("tts_voice")
        val KEY_OPENAI_KEY = stringPreferencesKey("openai_key")
        val KEY_OPENAI_BASE = stringPreferencesKey("openai_base")
        val KEY_AZURE_KEY = stringPreferencesKey("azure_key")
        val KEY_AZURE_REGION = stringPreferencesKey("azure_region")
    }

    val settingsFlow: Flow<Preferences> = context.dataStore.data

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    suspend fun setFloat(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        settingsFlow.map { it[key] ?: default }

    fun getFloat(key: Preferences.Key<Float>, default: Float = 1.0f): Flow<Float> =
        settingsFlow.map { it[key] ?: default }
}

package com.example.dogalseslikitap.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "reader_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val KEY_ENGINE = stringPreferencesKey("tts_engine")
        val KEY_VOICE = stringPreferencesKey("tts_voice")
        val KEY_SPEED = floatPreferencesKey("tts_speed")
        val KEY_PITCH = floatPreferencesKey("tts_pitch")
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
}

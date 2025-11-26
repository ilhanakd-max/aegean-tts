package com.example.dogalseslikitap.tts

import android.content.Context

/**
 * Selects the correct TTS engine based on user preference and exposes a unified speak/stop API.
 */
class TtsManager(private val context: Context) {
    private var currentEngine: TtsEngine = SystemTtsEngine(context)

    fun updateSettings(settings: TtsSettings) {
        currentEngine = when (settings.provider) {
            TtsProvider.SYSTEM -> SystemTtsEngine(context)
            TtsProvider.OPENAI -> OpenAiTtsEngine(context, settings.openAiBase.ifEmpty { "https://api.openai.com/" })
            TtsProvider.AZURE -> AzureTtsEngine(context, settings.azureRegion)
        }
    }

    fun speak(text: String, settings: TtsSettings, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        updateSettings(settings)
        currentEngine.speak(text, settings, onDone, onError)
    }

    fun stop() = currentEngine.stop()
}

package com.example.dogalseslikitap.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Wraps Android's native TextToSpeech.
 */
class SystemTtsEngine(context: Context) : TtsEngine {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("tr", "TR")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Logging only. UI layer shows warning if needed.
                }
            }
        }
    }

    override fun speak(text: String, settings: TtsSettings, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        try {
            tts?.setSpeechRate(settings.rate)
            tts?.setPitch(settings.pitch)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SPEECH_${System.currentTimeMillis()}")
            onDone()
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun stop() {
        tts?.stop()
    }
}

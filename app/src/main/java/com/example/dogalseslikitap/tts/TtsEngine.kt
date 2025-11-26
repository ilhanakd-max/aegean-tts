package com.example.dogalseslikitap.tts

/**
 * Unified TTS engine contract. Engines stream audio and notify the caller when playback ends.
 */
interface TtsEngine {
    fun speak(text: String, settings: TtsSettings, onDone: () -> Unit, onError: (Throwable) -> Unit)
    fun stop()
}

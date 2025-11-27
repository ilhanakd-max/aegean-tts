package com.example.dogalseslikitap.tts

data class TtsSettings(
    val selectedVoice: String = "",
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
)

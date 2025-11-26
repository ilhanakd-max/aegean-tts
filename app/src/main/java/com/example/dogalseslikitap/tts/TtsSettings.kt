package com.example.dogalseslikitap.tts

import android.speech.tts.TextToSpeech

data class TtsSettings(
    val enginePackage: String = TextToSpeech.Engine.DEFAULT_ENGINE,
    val voiceName: String = "",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f
)

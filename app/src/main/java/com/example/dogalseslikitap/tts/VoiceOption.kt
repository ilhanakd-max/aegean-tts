package com.example.dogalseslikitap.tts

import java.util.Locale

data class VoiceOption(
    val name: String,
    val label: String,
    val locale: Locale,
    val enginePackage: String
)

data class EngineOption(
    val label: String,
    val packageName: String
)

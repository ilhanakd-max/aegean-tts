package com.example.dogalseslikitap.tts

data class TtsSettings(
    val provider: TtsProvider = TtsProvider.SYSTEM,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val voice: String = "",
    val openAiKey: String = "",
    val openAiBase: String = "https://api.openai.com/",
    val azureKey: String = "",
    val azureRegion: String = ""
)

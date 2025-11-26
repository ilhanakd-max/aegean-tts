package com.example.dogalseslikitap.tts

enum class TtsProvider(val value: String) {
    SYSTEM("system"),
    OPENAI("openai"),
    AZURE("azure");

    companion object {
        fun fromValue(value: String?): TtsProvider = when (value) {
            OPENAI.value -> OPENAI
            AZURE.value -> AZURE
            else -> SYSTEM
        }
    }
}

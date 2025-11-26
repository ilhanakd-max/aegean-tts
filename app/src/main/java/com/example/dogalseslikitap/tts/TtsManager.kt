package com.example.dogalseslikitap.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TtsManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var currentEngine: String? = null

    suspend fun speak(
        text: String,
        settings: TtsSettings,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val ready = ensureEngine(settings.enginePackage)
        if (!ready) {
            onError(IllegalStateException("TTS hazır değil"))
            return
        }
        val engine = tts ?: return
        withContext(Dispatchers.Main) {
            engine.setSpeechRate(settings.speed)
            engine.setPitch(settings.pitch)
            val voice = engine.voices?.firstOrNull { it.name == settings.voiceName }
                ?: engine.voices?.firstOrNull { it.locale?.language == "tr" }
            voice?.let { engine.voice = it }
            val utteranceId = "local_${System.currentTimeMillis()}"
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post { onError(RuntimeException("TTS hatası")) }
                }

                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post { onDone() }
                }
            })
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                onError(RuntimeException("TTS başlatılamadı"))
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    suspend fun loadVoiceOptions(enginePackage: String?): List<VoiceOption> {
        val ready = ensureEngine(enginePackage)
        if (!ready) return emptyList()
        val engineVoices = tts?.voices ?: return emptyList()
        return engineVoices
            .filter { it.locale?.language == Locale("tr", "TR").language }
            .map {
                VoiceOption(
                    name = it.name,
                    label = "${it.locale.displayName} - ${it.name}",
                    locale = it.locale,
                    enginePackage = currentEngine ?: TextToSpeech.Engine.DEFAULT_ENGINE
                )
            }
            .sortedBy { it.label }
    }

    fun installedEngines(): List<EngineOption> {
        val temp = TextToSpeech(context.applicationContext) {}
        val engines = temp.engines?.map { EngineOption(label = it.label, packageName = it.name) } ?: emptyList()
        temp.shutdown()
        return engines
    }

    private suspend fun ensureEngine(enginePackage: String?): Boolean {
        if (tts != null && enginePackage == currentEngine) return true
        return withContext(Dispatchers.Main) {
            tts?.shutdown()
            currentEngine = enginePackage?.takeIf { it.isNotBlank() }
            suspendCoroutine { cont ->
                val listener = TextToSpeech.OnInitListener { status ->
                    cont.resume(status == TextToSpeech.SUCCESS)
                }
                val instance = if (currentEngine.isNullOrEmpty()) {
                    TextToSpeech(context.applicationContext, listener)
                } else {
                    TextToSpeech(context.applicationContext, listener, currentEngine)
                }
                tts = instance
            }
        }
    }
}

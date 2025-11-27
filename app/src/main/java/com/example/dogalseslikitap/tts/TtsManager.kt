package com.example.dogalseslikitap.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TtsManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var initialized = false

    private var rate: Float = 1.0f
    private var pitch: Float = 1.0f
    private var selectedVoice: String? = null

    suspend fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        withContext(Dispatchers.Main) {
            if (initialized) return@withContext
            suspendCancellableCoroutine { cont ->
                val engine = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts = engine
                        initialized = true
                        engine.language = Locale("tr", "TR")
                        engine.setSpeechRate(rate)
                        engine.setPitch(pitch)
                        selectVoice(engine, selectedVoice)
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            IllegalStateException("TextToSpeech init failed: $status"),
                        )
                    }
                }
                cont.invokeOnCancellation { engine.shutdown() }
            }
        }
    }

    fun speak(
        text: String,
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        val engine = tts ?: run {
            onError(IllegalStateException("TextToSpeech not initialized"))
            return
        }
        mainHandler.post {
            try {
                engine.setSpeechRate(rate)
                engine.setPitch(pitch)
                selectVoice(engine, selectedVoice)
                val utteranceId = "offline_${System.currentTimeMillis()}"
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { onDone() }
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { onError(RuntimeException("TTS hatası")) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { onError(RuntimeException("TTS hatası: $errorCode")) }
                    }
                })
                val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    onError(RuntimeException("Metin okunamadı"))
                }
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun pause() {
        stop()
    }

    fun resume() {
        // Android TextToSpeech cannot resume mid-utterance; kept for API completeness.
    }

    fun setRate(rate: Float) {
        this.rate = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        this.pitch = pitch
        tts?.setPitch(pitch)
    }

    fun setVoice(voiceName: String?) {
        selectedVoice = voiceName
        tts?.let { selectVoice(it, voiceName) }
    }

    fun getVoices(): List<Voice> {
        val voices = tts?.voices?.toList().orEmpty()
        val turkish = voices.filter { voice ->
            voice.locale?.language.equals(Locale("tr").language, ignoreCase = true)
        }
        return if (turkish.isNotEmpty()) turkish else voices
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        initialized = false
    }

    private fun selectVoice(engine: TextToSpeech, voiceName: String?) {
        val target = if (!voiceName.isNullOrBlank()) {
            engine.voices?.firstOrNull { it.name == voiceName }
        } else {
            engine.voices?.firstOrNull { it.locale?.language.equals("tr", ignoreCase = true) }
        }
        target?.let { engine.voice = it }
    }
}

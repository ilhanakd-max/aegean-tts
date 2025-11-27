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
    private var selectedVoiceName: String? = null

    suspend fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        withContext(Dispatchers.Main) {
            if (initialized) return@withContext
            suspendCancellableCoroutine { cont ->
                val ttsInstance = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts = ttsInstance
                        initialized = true
                        ttsInstance.setSpeechRate(rate)
                        ttsInstance.setPitch(pitch)
                        selectVoice(ttsInstance, selectedVoiceName)
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            IllegalStateException("TextToSpeech init failed: $status"),
                        )
                    }
                }
                cont.invokeOnCancellation { ttsInstance.shutdown() }
            }
        }
    }

    suspend fun speak(
        text: String,
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        val engine = tts ?: throw IllegalStateException("TextToSpeech not initialized")
        withContext(Dispatchers.Main) {
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)
            selectVoice(engine, selectedVoiceName)
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
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun pause() {
        stop()
    }

    fun resume() {
        // Android TextToSpeech does not support true resume; this is a no-op placeholder.
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
        selectedVoiceName = voiceName
        tts?.let { engine -> selectVoice(engine, voiceName) }
    }

    fun getVoices(): List<Voice> {
        val voices = tts?.voices?.toList().orEmpty()
        val turkish = voices.filter { it.locale?.language.equals(Locale("tr").language, ignoreCase = true) }
        return if (turkish.isNotEmpty()) turkish else voices
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        initialized = false
    }

    private fun selectVoice(engine: TextToSpeech, voiceName: String?) {
        val targetVoice = when {
            !voiceName.isNullOrBlank() -> engine.voices?.firstOrNull { it.name == voiceName }
            else -> engine.voices?.firstOrNull { it.locale?.language.equals("tr", ignoreCase = true) }
        }
        targetVoice?.let { engine.voice = it }
    }

    private fun selectVoice(engine: TextToSpeech, voiceName: String) {
        val targetVoice = engine.voices?.firstOrNull { it.name == voiceName }
            ?: engine.voices?.firstOrNull { it.locale?.language.equals("tr", ignoreCase = true) }
        targetVoice?.let { engine.voice = it }
    }
}

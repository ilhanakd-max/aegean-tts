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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var pendingRate = 1.0f
    private var pendingPitch = 1.0f

    suspend fun speak(
        text: String,
        settings: TtsSettings,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        pendingRate = settings.speed
        pendingPitch = settings.pitch
        val engine = ensureTts()
        if (engine == null) {
            onError(IllegalStateException("TTS başlatılamadı"))
            return
        }
        withContext(Dispatchers.Main) {
            engine.setSpeechRate(settings.speed)
            engine.setPitch(settings.pitch)
            selectVoice(engine, settings.voiceName)
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
        pendingRate = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        pendingPitch = pitch
        tts?.setPitch(pitch)
    }

    suspend fun listVoices(): List<VoiceOption> {
        val engine = ensureTts() ?: return emptyList()
        return withContext(Dispatchers.Main) {
            engine.voices
                ?.filter { it.locale?.language.equals(Locale("tr", "TR").language, ignoreCase = true) }
                ?.map {
                    VoiceOption(
                        name = it.name,
                        label = "${it.locale.displayName} - ${it.name}",
                        locale = it.locale,
                        enginePackage = engine.defaultEngine ?: ""
                    )
                }
                ?.sortedBy { it.label }
                ?: emptyList()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private suspend fun ensureTts(): TextToSpeech? = withContext(Dispatchers.Main) {
        tts?.let { return@withContext it }
        suspendCoroutine { cont ->
            val instance = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    instance.setSpeechRate(pendingRate)
                    instance.setPitch(pendingPitch)
                    cont.resume(instance)
                } else {
                    cont.resume(null)
                }
            }
            tts = instance
        }
    }

    private fun selectVoice(engine: TextToSpeech, voiceName: String) {
        val targetVoice = engine.voices?.firstOrNull { it.name == voiceName }
            ?: engine.voices?.firstOrNull { it.locale?.language.equals("tr", ignoreCase = true) }
        targetVoice?.let { engine.voice = it }
    }
}

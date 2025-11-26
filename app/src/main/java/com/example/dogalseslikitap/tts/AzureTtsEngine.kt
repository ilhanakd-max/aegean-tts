package com.example.dogalseslikitap.tts

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.io.File

private interface AzureService {
    @Headers("Content-Type: application/ssml+xml")
    @POST("cognitiveservices/v1")
    suspend fun tts(
        @Header("Ocp-Apim-Subscription-Key") apiKey: String,
        @Header("X-Microsoft-OutputFormat") format: String = "audio-16khz-32kbitrate-mono-mp3",
        @Body ssml: okhttp3.RequestBody
    ): ResponseBody
}

/**
 * REST client for Azure Cognitive Services TTS.
 */
class AzureTtsEngine(private val context: Context, endpoint: String) : TtsEngine {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val service: AzureService = Retrofit.Builder()
        .baseUrl(endpoint)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()
        )
        .build()
        .create(AzureService::class.java)

    override fun speak(text: String, settings: TtsSettings, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        scope.launch {
            try {
                val ssml = """
                    <speak version='1.0' xml:lang='tr-TR'>
                        <voice xml:lang='tr-TR' name='${settings.voice.ifEmpty { "tr-TR-AhmetNeural" }}'>
                            $text
                        </voice>
                    </speak>
                """.trimIndent().toRequestBody("application/ssml+xml".toMediaType())
                val body = service.tts(settings.azureKey, ssml = ssml)
                val file = saveToFile(body)
                playFile(file, onDone)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun saveToFile(body: ResponseBody): File {
        val file = File(context.cacheDir, "azure_tts.mp3")
        file.outputStream().use { output ->
            body.byteStream().use { input ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun playFile(file: File, onDone: () -> Unit) {
        val item = MediaItem.fromUri(file.toURI().toString())
        player.setMediaItem(item)
        player.prepare()
        player.play()
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    onDone()
                }
            }
        })
    }

    override fun stop() {
        player.stop()
    }
}

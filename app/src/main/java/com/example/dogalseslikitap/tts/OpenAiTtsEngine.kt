package com.example.dogalseslikitap.tts

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File

private interface OpenAiService {
    @POST("v1/audio/speech")
    suspend fun tts(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiTtsRequest
    ): ResponseBody
}

data class OpenAiTtsRequest(
    @SerializedName("model") val model: String = "gpt-4o-mini-tts",
    @SerializedName("input") val input: String,
    @SerializedName("voice") val voice: String = "alloy"
)

/**
 * Streams audio from OpenAI's TTS HTTP endpoint.
 */
class OpenAiTtsEngine(private val context: Context, baseUrl: String) : TtsEngine {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val service: OpenAiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAiService::class.java)

    override fun speak(text: String, settings: TtsSettings, onDone: () -> Unit, onError: (Throwable) -> Unit) {
        // Network work must be executed outside main thread; caller handles coroutine context.
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = service.tts("Bearer ${settings.openAiKey}", OpenAiTtsRequest(input = text))
                val file = saveToFile(response)
                playFile(file, onDone)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun saveToFile(body: ResponseBody): File {
        val file = File(context.cacheDir, "openai_tts.mp3")
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

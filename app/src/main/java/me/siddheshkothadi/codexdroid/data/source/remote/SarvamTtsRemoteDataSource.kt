package me.siddheshkothadi.codexdroid.data.source.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.siddheshkothadi.codexdroid.codex.CodexJson
import me.siddheshkothadi.codexdroid.domain.model.SarvamSynthesisResult
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarvamTtsRemoteDataSource @Inject constructor() {
    private val client =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(CodexJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 45_000
            }
            expectSuccess = false
        }

    suspend fun synthesize(
        text: String,
        settings: SarvamTtsSettings,
        apiKey: String,
    ): SarvamSynthesisResult {
        val response =
            client.post("https://api.sarvam.ai/text-to-speech") {
                header("api-subscription-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    SarvamTtsRequest(
                        text = text,
                        model = "bulbul:v3",
                        speaker = settings.voice,
                        targetLanguageCode = settings.targetLanguageCode,
                        pace = settings.pace,
                        speechSampleRate = settings.speechSampleRate,
                        temperature = settings.temperature,
                        outputAudioCodec = "wav",
                        enablePreprocessing = true,
                    )
                )
            }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
            val suffix = errorBody.take(300).trim()
            val msg =
                if (suffix.isBlank()) {
                    "Sarvam TTS request failed (${response.status.value})."
                } else {
                    "Sarvam TTS request failed (${response.status.value}): $suffix"
                }
            throw IllegalStateException(msg)
        }

        val body = response.body<SarvamTtsResponse>()
        val audioBase64 =
            body.audios.firstOrNull()
                ?.trim()
                ?.substringAfter("base64,", missingDelimiterValue = "")
                .orEmpty()
                .ifBlank {
                    body.audios.firstOrNull()?.trim().orEmpty()
                }

        if (audioBase64.isBlank()) {
            throw IllegalStateException("Sarvam TTS response did not include audio payload.")
        }

        val bytes =
            try {
                Base64.getDecoder().decode(audioBase64)
            } catch (_: IllegalArgumentException) {
                throw IllegalStateException("Sarvam TTS response audio payload could not be decoded.")
            }

        return SarvamSynthesisResult(
            audioBytes = bytes,
            audioCodec = "wav",
        )
    }
}

@Serializable
private data class SarvamTtsRequest(
    val text: String,
    val model: String,
    val speaker: String,
    @SerialName("target_language_code") val targetLanguageCode: String,
    val pace: Float,
    @SerialName("speech_sample_rate") val speechSampleRate: Int,
    val temperature: Float,
    @SerialName("output_audio_codec") val outputAudioCodec: String,
    @SerialName("enable_preprocessing") val enablePreprocessing: Boolean,
)

@Serializable
private data class SarvamTtsResponse(
    val audios: List<String> = emptyList(),
)


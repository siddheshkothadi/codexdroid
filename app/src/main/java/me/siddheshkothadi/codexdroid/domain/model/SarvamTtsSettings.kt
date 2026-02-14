package me.siddheshkothadi.codexdroid.domain.model

data class SarvamTtsSettings(
    val voice: String = DEFAULT_VOICE,
    val targetLanguageCode: String = DEFAULT_TARGET_LANGUAGE_CODE,
    val pace: Float = DEFAULT_PACE,
    val speechSampleRate: Int = DEFAULT_SPEECH_SAMPLE_RATE,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val apiKeyPresent: Boolean = false,
) {
    companion object {
        const val DEFAULT_VOICE = "Shubh"
        const val DEFAULT_TARGET_LANGUAGE_CODE = "en-IN"
        const val DEFAULT_PACE = 1.0f
        const val DEFAULT_SPEECH_SAMPLE_RATE = 24_000
        const val DEFAULT_TEMPERATURE = 0.6f

        val SUPPORTED_VOICES = setOf("Shubh", "Ritu", "Amelia", "Sophia")
        val SUPPORTED_SAMPLE_RATES = setOf(8_000, 16_000, 22_050, 24_000, 32_000, 44_100, 48_000)
        val SUPPORTED_LANGUAGE_CODES = setOf(
            "en-IN",
            "hi-IN",
            "bn-IN",
            "gu-IN",
            "kn-IN",
            "ml-IN",
            "mr-IN",
            "od-IN",
            "pa-IN",
            "ta-IN",
            "te-IN",
        )

        fun sanitize(input: SarvamTtsSettings): SarvamTtsSettings {
            return input.copy(
                voice =
                    input.voice
                        .trim()
                        .takeIf { it in SUPPORTED_VOICES }
                        ?: DEFAULT_VOICE,
                targetLanguageCode =
                    input.targetLanguageCode
                        .trim()
                        .takeIf { it in SUPPORTED_LANGUAGE_CODES }
                        ?: DEFAULT_TARGET_LANGUAGE_CODE,
                pace = input.pace.coerceIn(0.5f, 2.0f),
                speechSampleRate =
                    input.speechSampleRate.takeIf { it in SUPPORTED_SAMPLE_RATES }
                        ?: DEFAULT_SPEECH_SAMPLE_RATE,
                temperature = input.temperature.coerceIn(0.01f, 2.0f),
            )
        }
    }
}

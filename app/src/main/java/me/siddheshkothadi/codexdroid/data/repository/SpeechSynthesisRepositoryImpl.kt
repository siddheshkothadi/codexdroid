package me.siddheshkothadi.codexdroid.data.repository

import me.siddheshkothadi.codexdroid.data.source.remote.SarvamTtsRemoteDataSource
import me.siddheshkothadi.codexdroid.domain.model.SarvamSynthesisResult
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.domain.repository.SpeechSynthesisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechSynthesisRepositoryImpl @Inject constructor(
    private val remoteDataSource: SarvamTtsRemoteDataSource,
) : SpeechSynthesisRepository {
    override suspend fun synthesizeWithSarvam(
        text: String,
        settings: SarvamTtsSettings,
        apiKey: String,
    ): SarvamSynthesisResult {
        return remoteDataSource.synthesize(text, settings, apiKey)
    }
}


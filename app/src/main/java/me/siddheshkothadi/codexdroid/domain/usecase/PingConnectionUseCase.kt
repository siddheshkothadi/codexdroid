package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.codex.CodexApiService
import javax.inject.Inject

class PingConnectionUseCase @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend operator fun invoke(connection: Connection): Boolean {
        return try {
            apiService.ping(connection.baseUrl, connection.secret)
        } catch (_: Exception) {
            false
        }
    }
}

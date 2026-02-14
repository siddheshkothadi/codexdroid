package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import javax.inject.Inject

class PingConnectionUseCase @Inject constructor(
    private val codexSessionRepository: CodexSessionRepository,
) {
    suspend operator fun invoke(connection: Connection): Boolean {
        return codexSessionRepository.ping(connection)
    }
}



package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepository
import javax.inject.Inject

class RefreshThreadsUseCase @Inject constructor(
    private val repository: ThreadRepository
) {
    suspend operator fun invoke(connection: Connection) {
        repository.refreshThreads(connection)
    }
}

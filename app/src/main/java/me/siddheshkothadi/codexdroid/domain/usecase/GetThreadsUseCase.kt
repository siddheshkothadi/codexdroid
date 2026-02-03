package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepository
import javax.inject.Inject

class GetThreadsUseCase @Inject constructor(
    private val repository: ThreadRepository
) {
    operator fun invoke(connectionId: String): Flow<List<Thread>> = repository.getThreads(connectionId)
}

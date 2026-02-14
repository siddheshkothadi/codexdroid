package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.domain.repository.ThreadRepository
import javax.inject.Inject

class ObserveThreadUseCase @Inject constructor(
    private val threadRepository: ThreadRepository,
) {
    operator fun invoke(connectionId: String, threadId: String): Flow<Thread?> {
        return threadRepository.observeThread(connectionId, threadId)
    }
}



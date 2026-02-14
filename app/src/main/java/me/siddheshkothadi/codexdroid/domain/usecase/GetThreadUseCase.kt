package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.domain.repository.ThreadRepository
import javax.inject.Inject

class GetThreadUseCase @Inject constructor(
    private val threadRepository: ThreadRepository,
) {
    suspend operator fun invoke(connectionId: String, threadId: String): Thread? {
        return threadRepository.getThread(connectionId, threadId)
    }
}



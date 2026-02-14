package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.data.repository.ThreadRepository
import javax.inject.Inject

class UpsertThreadUseCase @Inject constructor(
    private val threadRepository: ThreadRepository,
) {
    suspend operator fun invoke(connectionId: String, thread: Thread) {
        threadRepository.upsertThread(connectionId, thread)
    }
}

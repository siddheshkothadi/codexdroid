package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.ThreadRepository
import javax.inject.Inject

class RenameThreadUseCase @Inject constructor(
    private val repository: ThreadRepository,
) {
    suspend operator fun invoke(connection: Connection, threadId: String, newName: String) {
        repository.renameThread(connection, threadId, newName)
    }
}

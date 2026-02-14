package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import javax.inject.Inject

class DeleteConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository,
) {
    suspend operator fun invoke(connectionId: String) {
        repository.deleteConnection(connectionId)
    }
}

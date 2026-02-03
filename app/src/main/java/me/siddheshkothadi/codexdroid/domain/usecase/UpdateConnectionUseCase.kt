package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import javax.inject.Inject

class UpdateConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(id: String, name: String, baseUrl: String, secret: String) {
        repository.updateConnection(id, name, baseUrl, secret)
    }
}

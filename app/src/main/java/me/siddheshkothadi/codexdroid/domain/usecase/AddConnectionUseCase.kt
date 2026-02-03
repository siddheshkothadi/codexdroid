package me.siddheshkothadi.codexdroid.domain.usecase

import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import javax.inject.Inject

class AddConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(name: String, url: String, secret: String) {
        repository.addConnection(name, url, secret)
    }
}

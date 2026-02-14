package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import javax.inject.Inject

class GetConnectionsUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<List<Connection>> = repository.observeConnections()
}



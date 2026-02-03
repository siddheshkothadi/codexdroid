package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.repository.ConnectionRepository
import javax.inject.Inject

class GetConnectionsUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<List<Connection>> = repository.getAllConnections()
}

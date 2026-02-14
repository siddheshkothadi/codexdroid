package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.data.source.local.ConnectionLocalDataSource
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val localDataSource: ConnectionLocalDataSource,
) : ConnectionRepository {
    override fun observeConnections(): Flow<List<Connection>> {
        return localDataSource.observeConnections()
    }

    override suspend fun addConnection(name: String, baseUrl: String, secret: String) {
        localDataSource.addConnection(name, baseUrl, secret)
    }

    override suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) {
        localDataSource.updateConnection(id, name, baseUrl, secret)
    }

    override suspend fun updateLastUsed(id: String) {
        localDataSource.updateLastUsed(id)
    }

    override suspend fun deleteConnection(id: String) {
        localDataSource.deleteConnection(id)
    }

    override suspend fun importConnections(connections: List<Connection>) {
        localDataSource.importConnections(connections)
    }
}

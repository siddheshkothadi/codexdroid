package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.local.ConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val connectionManager: ConnectionManager
) {
    fun getAllConnections(): Flow<List<Connection>> = connectionManager.connections

    suspend fun addConnection(name: String, baseUrl: String, secret: String) {
        connectionManager.addConnection(name, baseUrl, secret)
    }

    suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) {
        connectionManager.updateConnection(id, name, baseUrl, secret)
    }

    suspend fun updateLastUsed(id: String) {
        connectionManager.updateLastUsed(id)
    }

    suspend fun deleteConnection(id: String) {
        connectionManager.deleteConnection(id)
    }
}

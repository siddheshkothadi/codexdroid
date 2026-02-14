package me.siddheshkothadi.codexdroid.domain.repository

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.domain.model.Connection

interface ConnectionRepository {
    fun observeConnections(): Flow<List<Connection>>

    suspend fun addConnection(name: String, baseUrl: String, secret: String)

    suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String)

    suspend fun updateLastUsed(id: String)

    suspend fun deleteConnection(id: String)

    suspend fun importConnections(connections: List<Connection>)
}

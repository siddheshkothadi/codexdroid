package me.siddheshkothadi.codexdroid.data.source.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.siddheshkothadi.codexdroid.data.local.ConnectionDao
import me.siddheshkothadi.codexdroid.data.local.ConnectionEntity
import me.siddheshkothadi.codexdroid.data.local.CryptoManager
import me.siddheshkothadi.codexdroid.domain.model.Connection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionLocalDataSource @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val cryptoManager: CryptoManager,
) {
    fun observeConnections(): Flow<List<Connection>> {
        return connectionDao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }.sortedByDescending { it.updatedAt }
        }
    }

    suspend fun addConnection(name: String, baseUrl: String, secret: String) {
        val entity =
            ConnectionEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                encryptedSecret = encryptSecret(secret),
            )
        connectionDao.upsertConnection(entity)
    }

    suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) {
        val updated =
            ConnectionEntity(
                id = id,
                name = name,
                baseUrl = baseUrl,
                encryptedSecret = encryptSecret(secret),
                updatedAt = System.currentTimeMillis(),
            )
        connectionDao.upsertConnection(updated)
    }

    suspend fun updateLastUsed(id: String) {
        connectionDao.updateLastUsed(id, System.currentTimeMillis())
    }

    suspend fun deleteConnection(id: String) {
        connectionDao.deleteConnectionById(id)
    }

    suspend fun importConnections(connections: List<Connection>) {
        if (connections.isEmpty()) return
        val entities =
            connections.map { conn ->
                ConnectionEntity(
                    id = conn.id,
                    name = conn.name,
                    baseUrl = conn.baseUrl,
                    encryptedSecret = encryptSecret(conn.secret),
                    updatedAt = conn.updatedAt,
                )
            }
        connectionDao.upsertConnections(entities)
    }

    private fun ConnectionEntity.toDomain(): Connection {
        val secret =
            if (encryptedSecret.isBlank()) {
                ""
            } else {
                runCatching { cryptoManager.decrypt(encryptedSecret) }.getOrDefault("")
            }
        return Connection(
            id = id,
            name = name,
            baseUrl = baseUrl,
            secret = secret,
            updatedAt = updatedAt,
        )
    }

    private fun encryptSecret(secret: String): String {
        if (secret.isBlank()) return ""
        return cryptoManager.encrypt(secret)
    }
}

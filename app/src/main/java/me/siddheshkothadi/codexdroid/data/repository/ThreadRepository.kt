package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.local.ThreadDao
import me.siddheshkothadi.codexdroid.data.local.ThreadEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing threads both locally (Room) and remotely (Codex RPC).
 */
@Singleton
class ThreadRepository @Inject constructor(
    private val threadDao: ThreadDao,
    private val apiService: CodexApiService
) {
    private val tag = "ThreadRepository"

    /**
     * Returns a flow of threads for a specific connection from the local database.
     */
    fun getThreads(connectionId: String): Flow<List<Thread>> {
        return threadDao.getThreadsByConnection(connectionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getThread(connectionId: String, threadId: String): Thread? {
        val entity = threadDao.getThreadById(connectionId, threadId) ?: return null
        return entity.toDomain()
    }

    fun observeThread(connectionId: String, threadId: String): Flow<Thread?> {
        return threadDao.observeThreadById(connectionId, threadId).map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * Fetches the latest threads from the server and updates the local database.
     */
    suspend fun refreshThreads(connection: Connection) {
        try {
            val resp = apiService.listThreads(connection.baseUrl, connection.secret)
            val threads = resp.result?.data ?: emptyList()
            
            val entities =
                threads.map { thread ->
                    val existing = threadDao.getThreadById(connection.id, thread.id)?.toDomain()
                    val merged =
                        if (existing != null) {
                            val mergedTurns =
                                if (existing.turns.isNotEmpty() && thread.turns.isEmpty()) existing.turns else thread.turns
                            thread.copy(
                                turns = mergedTurns,
                                clientModel = existing.clientModel,
                                clientEffort = existing.clientEffort,
                            )
                        } else thread
                    merged.toEntity(connection.id)
                }
            threadDao.insertThreads(entities)
        } catch (e: Exception) {
            Log.w(tag, "Failed to refresh threads", e)
        }
    }

    suspend fun upsertThread(connectionId: String, thread: Thread) {
        threadDao.upsertThread(thread.toEntity(connectionId))
    }

    // --- Mappers ---

    private fun Thread.toEntity(connectionId: String) = ThreadEntity(
        id = id,
        preview = preview,
        modelProvider = modelProvider,
        createdAt = createdAt,
        updatedAt = updatedAt,
        path = path,
        cwd = cwd,
        connectionId = connectionId,
        threadJson = try {
            CodexJson.encodeToString(Thread.serializer(), this)
        } catch (_: Exception) {
            null
        }
    )

    private fun ThreadEntity.toDomain(): Thread {
        val decoded =
            try {
                threadJson?.let { CodexJson.decodeFromString(Thread.serializer(), it) }
            } catch (_: Exception) {
                null
            }
        if (decoded != null) return decoded
        return Thread(
            id = id,
            preview = preview,
            modelProvider = modelProvider,
            createdAt = createdAt,
            updatedAt = updatedAt,
            path = path,
            cwd = cwd,
        )
    }
}

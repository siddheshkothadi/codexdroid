package me.siddheshkothadi.codexdroid.data.repository

import kotlinx.coroutines.flow.Flow
import android.util.Log
import me.siddheshkothadi.codexdroid.codex.*
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.data.source.local.ThreadLocalDataSource
import me.siddheshkothadi.codexdroid.data.source.remote.ThreadRemoteDataSource
import me.siddheshkothadi.codexdroid.domain.repository.ThreadRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing threads both locally (Room) and remotely (Codex RPC).
 */
@Singleton
class ThreadRepositoryImpl @Inject constructor(
    private val localDataSource: ThreadLocalDataSource,
    private val remoteDataSource: ThreadRemoteDataSource,
) : ThreadRepository {
    private val tag = "ThreadRepository"

    /**
     * Returns a flow of threads for a specific connection from the local database.
     */
    override fun observeThreads(connectionId: String): Flow<List<Thread>> {
        return localDataSource.observeThreads(connectionId)
    }

    override suspend fun getThread(connectionId: String, threadId: String): Thread? {
        return localDataSource.getThread(connectionId, threadId)
    }

    override fun observeThread(connectionId: String, threadId: String): Flow<Thread?> {
        return localDataSource.observeThread(connectionId, threadId)
    }

    /**
     * Fetches the latest threads from the server and updates the local database.
     */
    override suspend fun refreshThreads(connection: Connection) {
        try {
            val threads = remoteDataSource.listThreads(connection)
            val mergedThreads =
                threads.map { thread ->
                    val existing = localDataSource.getThread(connection.id, thread.id)
                    if (existing != null) {
                        val mergedTurns =
                            if (existing.turns.isNotEmpty() && thread.turns.isEmpty()) existing.turns else thread.turns
                        thread.copy(
                            turns = mergedTurns,
                            clientName = existing.clientName,
                            clientModel = existing.clientModel,
                            clientEffort = existing.clientEffort,
                        )
                    } else {
                        thread
                    }
                }
            localDataSource.upsertThreads(connection.id, mergedThreads)
        } catch (e: Exception) {
            Log.w(tag, "Failed to refresh threads", e)
        }
    }

    override suspend fun upsertThread(connectionId: String, thread: Thread) {
        localDataSource.upsertThread(connectionId, thread)
    }

    override suspend fun renameThread(connection: Connection, threadId: String, newName: String) {
        val normalized = newName.trim()
        if (normalized.isBlank()) return

        val existing = localDataSource.getThread(connection.id, threadId)
        if (existing != null) {
            localDataSource.upsertThread(connection.id, existing.copy(clientName = normalized))
        }

        runCatching {
            remoteDataSource.setThreadName(connection, threadId, normalized)
        }.onFailure { error ->
            Log.w(tag, "Failed to set thread name on server", error)
        }
    }

    override suspend fun archiveThread(connection: Connection, threadId: String) {
        remoteDataSource.archiveThread(connection, threadId)
        localDataSource.deleteThread(connection.id, threadId)
    }
}

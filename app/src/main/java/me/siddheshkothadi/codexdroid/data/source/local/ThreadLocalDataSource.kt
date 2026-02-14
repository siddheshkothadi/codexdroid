package me.siddheshkothadi.codexdroid.data.source.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.siddheshkothadi.codexdroid.codex.CodexJson
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.data.local.ThreadDao
import me.siddheshkothadi.codexdroid.data.local.ThreadEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreadLocalDataSource @Inject constructor(
    private val threadDao: ThreadDao,
) {
    fun observeThreads(connectionId: String): Flow<List<Thread>> {
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

    suspend fun upsertThread(connectionId: String, thread: Thread) {
        threadDao.upsertThread(thread.toEntity(connectionId))
    }

    suspend fun upsertThreads(connectionId: String, threads: List<Thread>) {
        threadDao.insertThreads(threads.map { it.toEntity(connectionId) })
    }

    suspend fun deleteThread(connectionId: String, threadId: String) {
        threadDao.deleteThreadById(connectionId, threadId)
    }

    private fun Thread.toEntity(connectionId: String) =
        ThreadEntity(
            id = id,
            preview = preview,
            modelProvider = modelProvider,
            createdAt = createdAt,
            updatedAt = updatedAt,
            path = path,
            cwd = cwd,
            connectionId = connectionId,
            threadJson =
                try {
                    CodexJson.encodeToString(Thread.serializer(), this)
                } catch (_: Exception) {
                    null
                },
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

package me.siddheshkothadi.codexdroid.domain.repository

import kotlinx.coroutines.flow.Flow
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.domain.model.Connection

interface ThreadRepository {
    fun observeThreads(connectionId: String): Flow<List<Thread>>

    suspend fun getThread(connectionId: String, threadId: String): Thread?

    fun observeThread(connectionId: String, threadId: String): Flow<Thread?>

    suspend fun refreshThreads(connection: Connection)

    suspend fun upsertThread(connectionId: String, thread: Thread)
}

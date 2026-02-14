package me.siddheshkothadi.codexdroid.data.source.remote

import me.siddheshkothadi.codexdroid.codex.CodexApiService
import me.siddheshkothadi.codexdroid.codex.Thread
import me.siddheshkothadi.codexdroid.domain.model.Connection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreadRemoteDataSource @Inject constructor(
    private val apiService: CodexApiService,
) {
    suspend fun listThreads(connection: Connection): List<Thread> {
        val response = apiService.listThreads(connection.baseUrl, connection.secret)
        return response.result?.data ?: emptyList()
    }

    suspend fun archiveThread(connection: Connection, threadId: String) {
        val response = apiService.archiveThread(connection.baseUrl, connection.secret, threadId)
        response.error?.let { error ->
            throw IllegalStateException(error.message.ifBlank { "Failed to archive thread." })
        }
    }

    suspend fun setThreadName(connection: Connection, threadId: String, name: String) {
        val response = apiService.setThreadName(connection.baseUrl, connection.secret, threadId, name)
        response.error?.let { error ->
            throw IllegalStateException(error.message.ifBlank { "Failed to rename thread." })
        }
    }
}

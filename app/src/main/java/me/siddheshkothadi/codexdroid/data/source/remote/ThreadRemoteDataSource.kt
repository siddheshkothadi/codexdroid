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
}

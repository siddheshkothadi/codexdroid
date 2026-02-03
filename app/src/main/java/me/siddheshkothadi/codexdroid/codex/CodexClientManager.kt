package me.siddheshkothadi.codexdroid.codex

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodexClientManager @Inject constructor() {
    private val lock = Mutex()
    private var activeKey: String? = null
    private var activeClient: CodexWsClient? = null

    private fun key(baseUrl: String, secret: String?): String {
        return "${baseUrl.trim()}|${secret ?: ""}"
    }

    suspend fun get(baseUrl: String, secret: String?): CodexWsClient {
        val normalizedSecret = secret?.takeIf { it.isNotBlank() }
        val k = key(baseUrl, normalizedSecret)

        return lock.withLock {
            val existing = activeClient
            if (existing != null && activeKey == k) {
                existing
            } else {
                existing?.close()
                val created = CodexWsClient(baseUrl = baseUrl, secret = normalizedSecret)
                activeKey = k
                activeClient = created
                created
            }
        }.also { it.start() }
    }

    suspend fun closeActive() {
        lock.withLock {
            activeClient?.close()
            activeClient = null
            activeKey = null
        }
    }
}


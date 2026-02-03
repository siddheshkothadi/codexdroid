package me.siddheshkothadi.codexdroid.codex

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.data.local.Connection
import me.siddheshkothadi.codexdroid.data.local.ConnectionManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the Codex WebSocket connection aligned with app foreground/background.
 *
 * - Foreground (onStart): connect (if a connection exists)
 * - Background (onStop): keep connected (background WS is enabled)
 */
@Singleton
class CodexAppLifecycle @Inject constructor(
    connectionManager: ConnectionManager,
    private val clientManager: CodexClientManager,
) : DefaultLifecycleObserver {
    private val tag = "CodexAppLifecycle"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var activeConnection: Connection? = null
    private val _isForeground = MutableStateFlow(false)
    val isForeground = _isForeground.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            connectionManager.connections.collectLatest { list ->
                activeConnection = list.firstOrNull()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        _isForeground.value = true
        val conn = activeConnection
        if (conn == null || conn.baseUrl.isBlank()) return
        scope.launch {
            try {
                clientManager.get(conn.baseUrl, conn.secret)
            } catch (e: Exception) {
                Log.w(tag, "Failed to connect Codex WS on foreground", e)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        _isForeground.value = false
        // Intentionally keep the WebSocket connected in the background.
    }
}

package me.siddheshkothadi.codexdroid.codex

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.di.MainDispatcher
import me.siddheshkothadi.codexdroid.domain.usecase.GetConnectionsUseCase
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
    getConnectionsUseCase: GetConnectionsUseCase,
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val clientManager: CodexClientManager,
) : DefaultLifecycleObserver {
    private val tag = "CodexAppLifecycle"
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    @Volatile private var activeConnection: Connection? = null
    private val _isForeground = MutableStateFlow(false)
    val isForeground = _isForeground.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            getConnectionsUseCase().collectLatest { list ->
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

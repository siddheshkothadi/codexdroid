package me.siddheshkothadi.codexdroid

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import me.siddheshkothadi.codexdroid.codex.CodexAppLifecycle
import me.siddheshkothadi.codexdroid.codex.CodexEventRouter
import me.siddheshkothadi.codexdroid.codex.CodexKeepAliveService
import me.siddheshkothadi.codexdroid.data.local.ConnectionStorageMigration
import me.siddheshkothadi.codexdroid.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CodexDroidApplication : Application() {
    @Inject lateinit var codexAppLifecycle: CodexAppLifecycle
    @Inject lateinit var codexEventRouter: CodexEventRouter
    @Inject lateinit var connectionStorageMigration: ConnectionStorageMigration
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { connectionStorageMigration.migrateIfNeeded() }
        }
        // Touch the singleton so it registers its ProcessLifecycle observer.
        codexAppLifecycle.hashCode()
        // Touch the singleton so it starts the global event router.
        codexEventRouter.hashCode()
        // Always keep a foreground service so the WS connection can remain active in background.
        CodexKeepAliveService.start(this)
    }
}

package me.siddheshkothadi.codexdroid.codex

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.siddheshkothadi.codexdroid.MainActivity
import me.siddheshkothadi.codexdroid.R
import me.siddheshkothadi.codexdroid.data.local.ConnectionManager
import me.siddheshkothadi.codexdroid.navigation.CodexDroidAppLinkKeys
import javax.inject.Inject

/**
 * Foreground service to keep the process alive so the Codex WS connection can remain active
 * even when the app is backgrounded.
 *
 * Android reality: without a foreground service, the OS may stop background networking and/or kill
 * the process, even if we "try to keep connected".
 */
@AndroidEntryPoint
class CodexKeepAliveService : Service() {
    private val tag = "CodexKeepAliveService"

    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var clientManager: CodexClientManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startForegroundSafely()

        scope.launch {
            connectionManager.connections.collectLatest { connections ->
                val active = connections.firstOrNull()
                if (active == null || active.baseUrl.isBlank()) {
                    try {
                        clientManager.closeActive()
                    } catch (_: Exception) {
                    }
                    return@collectLatest
                }
                try {
                    clientManager.get(active.baseUrl, active.secret)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to connect Codex WS in keep-alive service", e)
                }
            }
        }
    }

    private fun startForegroundSafely() {
        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // If notifications are blocked (Android 13+ POST_NOTIFICATIONS), we cannot reliably keep a foreground service.
            // We still keep running best-effort; the OS may stop us.
            Log.w(tag, "Unable to start foreground (notifications permission?)", e)
        } catch (e: Exception) {
            Log.w(tag, "Unable to start foreground", e)
        }
    }

    private fun createNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Codex connection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps Codex connected in the background."
                    setShowBadge(false)
                }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(CodexDroidAppLinkKeys.EXTRA_OPEN_LATEST, true)
            }
        val openAppPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Codex is connected")
            .setContentText("Keeping your Codex session active.")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep alive until explicitly stopped.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            runCatching { clientManager.closeActive() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "codex_keep_alive"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, CodexKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

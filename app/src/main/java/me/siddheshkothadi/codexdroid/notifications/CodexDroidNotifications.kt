package me.siddheshkothadi.codexdroid.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import me.siddheshkothadi.codexdroid.R

object CodexDroidNotifications {
    // Use a versioned channel ID because Android preserves existing channel settings by ID.
    const val TURN_CHANNEL_ID = "turn_events_v2"

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureTurnChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(TURN_CHANNEL_ID)
        if (existing != null) return

        val channel =
            NotificationChannel(
                TURN_CHANNEL_ID,
                "Turn updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when Codex finishes a turn."
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }
        manager.createNotificationChannel(channel)
    }

    fun notifyTurnCompleted(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        contentIntent: PendingIntent,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureTurnChannel(context)

        val appIcon =
            runCatching {
                context.packageManager
                    .getApplicationIcon(context.applicationInfo)
                    .toBitmap()
            }.getOrNull()

        val notification =
            NotificationCompat.Builder(context, TURN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(appIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Best-effort only: permission can still be denied or revoked at runtime.
        }
    }
}

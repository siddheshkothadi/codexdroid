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
import me.siddheshkothadi.codexdroid.R

object CodexDroidNotifications {
    const val TURN_CHANNEL_ID = "turn_events"

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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when Codex finishes a turn."
                setShowBadge(true)
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
        if (!canPostNotifications(context)) return
        ensureTurnChannel(context)

        val notification =
            NotificationCompat.Builder(context, TURN_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}


package app.spotmycar.android.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.spotmycar.android.MainActivity
import app.spotmycar.android.R
import app.spotmycar.android.ui.theme.LightPalette
import androidx.compose.ui.graphics.toArgb

/** Les notifications de l'app : « Place enregistrée » et les alertes de stationnement. */
object Notifier {
    const val EXTRA_OPEN_DETAILS = "app.spotmycar.android.OPEN_DETAILS"
    const val EXTRA_OPEN_GUIDE = "app.spotmycar.android.OPEN_GUIDE"

    private const val CHANNEL_SPOT = "parking_spot"
    private const val CHANNEL_ALERTS = "parking_alerts"
    private const val NOTIFICATION_SAVED = 1001

    fun canNotify(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Après un enregistrement automatique : propose d'ajouter le niveau. */
    fun showAutoSaved(context: Context, hasPosition: Boolean) {
        val text = context.getString(
            if (hasPosition) R.string.notification_saved_text else R.string.notification_saved_text_no_gps,
        )
        post(
            context = context,
            channel = CHANNEL_SPOT,
            id = NOTIFICATION_SAVED,
            title = context.getString(R.string.notification_saved_title),
            text = text,
            extra = EXTRA_OPEN_DETAILS,
        )
    }

    /** Une alerte (fermeture, durée, gratuité) : ouvre le guidage vers la voiture. */
    fun showAlert(context: Context, id: Int, title: String, text: String) {
        post(
            context = context,
            channel = CHANNEL_ALERTS,
            id = id,
            title = title,
            text = text,
            extra = EXTRA_OPEN_GUIDE,
        )
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, channel: String, id: Int, title: String, text: String, extra: String) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(extra, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_pin)
            .setColor(LightPalette.accent.toArgb())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Autorisation retirée entre-temps : rien à faire.
        }
    }

    private fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_SPOT) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SPOT,
                    context.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        if (manager.getNotificationChannel(CHANNEL_ALERTS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    context.getString(R.string.notification_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }
}

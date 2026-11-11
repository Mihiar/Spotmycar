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

/** La petite notification « Place enregistrée » après un enregistrement automatique. */
object Notifier {
    const val EXTRA_OPEN_DETAILS = "app.spotmycar.android.OPEN_DETAILS"

    private const val CHANNEL_ID = "parking_spot"
    private const val NOTIFICATION_ID = 1001

    fun canNotify(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun showAutoSaved(context: Context, hasPosition: Boolean) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_DETAILS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = context.getString(
            if (hasPosition) R.string.notification_saved_text else R.string.notification_saved_text_no_gps,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pin)
            .setContentTitle(context.getString(R.string.notification_saved_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Autorisation retirée entre-temps : la place est notée quand même.
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}

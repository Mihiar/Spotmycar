package app.spotmycar.android.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.spotmycar.android.R
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.notify.Notifier
import app.spotmycar.android.util.SpotText

/**
 * Reçoit les rappels programmés par [AlertScheduler] et affiche la notification.
 * Au redémarrage du téléphone, Android oublie les alarmes : on les reprogramme.
 */
class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                AlertScheduler.sync(appContext, ParkingStore.current(appContext))
            }

            ACTION_ALERT -> showAlert(appContext, intent)
        }
    }

    private fun showAlert(context: Context, intent: Intent) {
        val kind = try {
            AlertKind.valueOf(intent.getStringExtra(AlertScheduler.EXTRA_KIND) ?: return)
        } catch (e: IllegalArgumentException) {
            return
        }
        // La voiture a peut-être déjà été retrouvée, ou une autre place notée entre-temps.
        val spot = ParkingStore.current(context) ?: return
        if (spot.savedAt != intent.getLongExtra(AlertScheduler.EXTRA_SPOT, -1L)) return

        val settings = AppSettings(context)
        val title = when (kind) {
            AlertKind.CLOSING -> context.getString(
                R.string.alert_closing_title,
                SpotText.clockOfDay(context, settings.closingMinutes),
            )

            AlertKind.DURATION -> context.getString(
                R.string.alert_duration_title,
                SpotText.duration(context, System.currentTimeMillis() - spot.savedAt),
            )

            AlertKind.FREE -> context.getString(
                R.string.alert_free_title,
                SpotText.clockOfDay(context, settings.freeMinutes),
            )
        }
        Notifier.showAlert(context, kind.requestCode, title, SpotText.reminder(context, spot))
    }

    companion object {
        const val ACTION_ALERT = "app.spotmycar.android.ACTION_ALERT"
    }
}

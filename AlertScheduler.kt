package app.spotmycar.android.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.data.ParkingSpot
import java.util.Calendar

/** Les alertes possibles pendant qu'une voiture est garée. */
enum class AlertKind(val requestCode: Int) {
    CLOSING(201),
    DURATION(202),
    FREE(203),
}

/**
 * Programme (ou annule) les rappels liés à la place en cours.
 *
 * On utilise des alarmes « inexactes » d'Android : elles peuvent arriver avec
 * quelques minutes de décalage, mais ne demandent aucune autorisation spéciale
 * et économisent la batterie.
 */
object AlertScheduler {
    const val EXTRA_KIND = "app.spotmycar.android.ALERT_KIND"
    const val EXTRA_SPOT = "app.spotmycar.android.ALERT_SPOT"

    private const val MINUTE = 60_000L
    private const val CLOSING_ADVANCE = 15 * MINUTE

    /** À appeler à chaque changement de place ou de réglage d'alerte. */
    fun sync(context: Context, spot: ParkingSpot?) {
        val appContext = context.applicationContext
        val settings = AppSettings(appContext)
        val now = System.currentTimeMillis()

        AlertKind.entries.forEach { kind ->
            val time = if (spot == null) null else when (kind) {
                AlertKind.CLOSING -> if (settings.closingAlert) closingTime(settings, now) else null
                AlertKind.DURATION -> if (settings.durationAlert) durationTime(settings, spot, now) else null
                AlertKind.FREE -> if (settings.freeAlert) freeTime(settings, spot, now) else null
            }
            if (time == null || spot == null) cancel(appContext, kind) else schedule(appContext, kind, spot, time)
        }
    }

    /** 15 minutes avant la prochaine fermeture (tout de suite si elle est imminente). */
    private fun closingTime(settings: AppSettings, now: Long): Long {
        val closing = nextOccurrence(settings.closingMinutes, now)
        return (closing - CLOSING_ADVANCE).coerceAtLeast(now + 30_000L)
    }

    private fun durationTime(settings: AppSettings, spot: ParkingSpot, now: Long): Long? {
        val time = spot.savedAt + settings.maxHours * 60 * MINUTE
        return time.takeIf { it > now }
    }

    /** Le jour même seulement : si l'on s'est garé après l'heure, la rue est déjà gratuite. */
    private fun freeTime(settings: AppSettings, spot: ParkingSpot, now: Long): Long? {
        val sameDay = atMinutes(spot.savedAt, settings.freeMinutes)
        return sameDay.takeIf { it > spot.savedAt && it > now }
    }

    private fun atMinutes(reference: Long, minutesOfDay: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = reference
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun nextOccurrence(minutesOfDay: Int, now: Long): Long {
        val today = atMinutes(now, minutesOfDay)
        return if (today > now) today else today + 24 * 60 * MINUTE
    }

    private fun schedule(context: Context, kind: AlertKind, spot: ParkingSpot, time: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent(context, kind, spot.savedAt))
        } catch (e: SecurityException) {
            // Très rare (certains fabricants) : le rappel n'arrivera pas, la place reste notée.
        }
    }

    private fun cancel(context: Context, kind: AlertKind) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(pendingIntent(context, kind, 0L))
    }

    /** Même « requestCode » par type d'alerte : reprogrammer remplace l'ancienne. */
    private fun pendingIntent(context: Context, kind: AlertKind, spotSavedAt: Long): PendingIntent {
        val intent = Intent(context, AlertReceiver::class.java)
            .setAction(AlertReceiver.ACTION_ALERT)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_SPOT, spotSavedAt)
        return PendingIntent.getBroadcast(
            context,
            kind.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

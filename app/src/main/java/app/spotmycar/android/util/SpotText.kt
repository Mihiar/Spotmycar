package app.spotmycar.android.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.PastSpot
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Les petites phrases construites à partir d'une place (écrans, partage, notifications). */
object SpotText {
    /** « Niveau -2 · Zone C · Place 148 », ou vide s'il n'y a aucun repère garage. */
    fun garageLine(context: Context, level: String, zone: String, place: String): String =
        listOfNotNull(
            level.takeIf { it.isNotBlank() }?.let { context.getString(R.string.garage_level, it) },
            zone.takeIf { it.isNotBlank() }?.let { context.getString(R.string.garage_zone, it) },
            place.takeIf { it.isNotBlank() }?.let { context.getString(R.string.garage_place, it) },
        ).joinToString(" · ")

    fun garageLine(context: Context, spot: ParkingSpot): String =
        garageLine(context, spot.level, spot.zone, spot.place)

    /** Version courte pour l'historique : « N-2 · C148 ». */
    fun shortGarage(context: Context, past: PastSpot): String {
        val parts = mutableListOf<String>()
        if (past.level.isNotBlank()) parts += context.getString(R.string.garage_level_short, past.level)
        val zonePlace = past.zone + past.place
        if (zonePlace.isNotBlank()) parts += zonePlace
        return parts.joinToString(" · ")
    }

    /** Titre d'une ligne d'historique : « Carrefour Anfa · N-2 · C148 ». */
    fun historyTitle(context: Context, past: PastSpot): String {
        val parts = mutableListOf<String>()
        if (past.placeName.isNotBlank()) parts += past.placeName
        val garage = shortGarage(context, past)
        if (garage.isNotBlank()) parts += garage
        return if (parts.isEmpty()) context.getString(R.string.history_unnamed) else parts.joinToString(" · ")
    }

    /** « Garée il y a 42 minutes » (géré dans la langue du téléphone). */
    fun ago(context: Context, savedAt: Long, now: Long): String =
        if (now - savedAt < DateUtils.MINUTE_IN_MILLIS) {
            context.getString(R.string.time_just_now)
        } else {
            DateUtils.getRelativeTimeSpanString(savedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
        }

    /** « 14:07 » ou « 2:07 PM » selon le réglage du téléphone. */
    fun clock(context: Context, time: Long): String = DateFormat.getTimeFormat(context).format(Date(time))

    /** « 23:00 » à partir de minutes depuis minuit. */
    fun clockOfDay(context: Context, minutesOfDay: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
        }
        return clock(context, calendar.timeInMillis)
    }

    /** « dim. 31 août ». */
    fun day(time: Long): String {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
        return java.text.SimpleDateFormat(pattern, locale).format(Date(time))
    }

    /** « 3 h 10 min » ou « 45 min ». */
    fun duration(context: Context, millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(1L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            context.getString(R.string.duration_hm, hours, minutes)
        } else {
            context.getString(R.string.duration_m, minutes)
        }
    }

    /** Ce qu'on rappelle de la voiture dans une notification. */
    fun reminder(context: Context, spot: ParkingSpot): String {
        val garage = garageLine(context, spot)
        return when {
            garage.isNotBlank() -> garage
            spot.note.isNotBlank() -> spot.note
            else -> context.getString(R.string.alert_generic_text)
        }
    }
}

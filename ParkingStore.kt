package app.spotmycar.android.data

import android.content.Context
import android.content.SharedPreferences
import app.spotmycar.android.alerts.AlertScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Mémoire de l'application : la place en cours, gardée sur le téléphone
 * (SharedPreferences). Aucun serveur, aucun compte.
 *
 * L'écran et le récepteur Bluetooth passent tous les deux par ici, donc
 * l'écran se met à jour tout seul après un enregistrement automatique.
 * Chaque changement remet aussi les alertes à jour.
 */
object ParkingStore {
    private const val PREFS = "spotmycar_spot"
    private const val K_SAVED_AT = "saved_at"
    private const val K_LAT = "lat"
    private const val K_LNG = "lng"
    private const val K_ACCURACY = "accuracy"
    private const val K_SOURCE = "source"
    private const val K_LEVEL = "level"
    private const val K_ZONE = "zone"
    private const val K_PLACE = "place"
    private const val K_NOTE = "note"
    private const val K_PHOTO = "photo"
    private const val K_PLACE_NAME = "place_name"
    private const val K_AUTO = "auto"
    private const val K_PRESSURE = "pressure"

    /** Ancienne clé (« étage / zone » en texte libre) : reprise dans le niveau. */
    private const val K_LEGACY_FLOOR = "floor"

    private val state = MutableStateFlow<ParkingSpot?>(null)
    val spot: StateFlow<ParkingSpot?> = state.asStateFlow()

    private var loaded = false

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        state.value = read(prefs(context))
        loaded = true
    }

    @Synchronized
    fun current(context: Context): ParkingSpot? {
        ensureLoaded(context)
        return state.value
    }

    /**
     * Remplace la place en cours par une nouvelle. L'ancienne part dans
     * l'historique et sa photo est supprimée.
     */
    @Synchronized
    fun replace(context: Context, spot: ParkingSpot) {
        val previous = current(context)
        if (previous != null) {
            HistoryStore.add(context, previous, endedAt = spot.savedAt)
            val previousPhoto = previous.photoPath
            if (previousPhoto != null && previousPhoto != spot.photoPath) deleteFile(previousPhoto)
        }
        write(prefs(context), spot)
        state.value = spot
        AlertScheduler.sync(context, spot)
    }

    /** Modifie la place en cours, s'il y en a une. */
    @Synchronized
    fun update(context: Context, transform: (ParkingSpot) -> ParkingSpot) {
        val before = current(context) ?: return
        val after = transform(before)
        if (after == before) return
        val oldPhoto = before.photoPath
        if (oldPhoto != null && oldPhoto != after.photoPath) deleteFile(oldPhoto)
        write(prefs(context), after)
        state.value = after
        AlertScheduler.sync(context, after)
    }

    /** « Je suis à ma voiture » : la place part dans l'historique, la photo est effacée. */
    @Synchronized
    fun finish(context: Context) {
        val spot = current(context) ?: return
        HistoryStore.add(context, spot, endedAt = System.currentTimeMillis())
        discard(context)
    }

    /** Efface la place en cours sans la garder (ex. « Annuler » juste après l'avoir notée). */
    @Synchronized
    fun discard(context: Context) {
        current(context)?.photoPath?.let { deleteFile(it) }
        prefs(context).edit().clear().apply()
        state.value = null
        AlertScheduler.sync(context, null)
    }

    private fun read(prefs: SharedPreferences): ParkingSpot? {
        val savedAt = prefs.getLong(K_SAVED_AT, 0L)
        if (savedAt <= 0L) return null
        val source = try {
            LocationSource.valueOf(prefs.getString(K_SOURCE, null) ?: LocationSource.NONE.name)
        } catch (e: IllegalArgumentException) {
            LocationSource.NONE
        }
        val level = prefs.getString(K_LEVEL, null) ?: prefs.getString(K_LEGACY_FLOOR, "") ?: ""
        return ParkingSpot(
            savedAt = savedAt,
            latitude = prefs.getString(K_LAT, null)?.toDoubleOrNull(),
            longitude = prefs.getString(K_LNG, null)?.toDoubleOrNull(),
            accuracyMeters = prefs.getString(K_ACCURACY, null)?.toFloatOrNull(),
            source = source,
            level = level,
            zone = prefs.getString(K_ZONE, "") ?: "",
            place = prefs.getString(K_PLACE, "") ?: "",
            note = prefs.getString(K_NOTE, "") ?: "",
            photoPath = prefs.getString(K_PHOTO, null),
            placeName = prefs.getString(K_PLACE_NAME, "") ?: "",
            auto = prefs.getBoolean(K_AUTO, false),
            pressureHpa = prefs.getString(K_PRESSURE, null)?.toFloatOrNull(),
        )
    }

    private fun write(prefs: SharedPreferences, spot: ParkingSpot) {
        prefs.edit()
            .clear()
            .putLong(K_SAVED_AT, spot.savedAt)
            .putString(K_LAT, spot.latitude?.toString())
            .putString(K_LNG, spot.longitude?.toString())
            .putString(K_ACCURACY, spot.accuracyMeters?.toString())
            .putString(K_SOURCE, spot.source.name)
            .putString(K_LEVEL, spot.level)
            .putString(K_ZONE, spot.zone)
            .putString(K_PLACE, spot.place)
            .putString(K_NOTE, spot.note)
            .putString(K_PHOTO, spot.photoPath)
            .putString(K_PLACE_NAME, spot.placeName)
            .putBoolean(K_AUTO, spot.auto)
            .putString(K_PRESSURE, spot.pressureHpa?.toString())
            .apply()
    }

    private fun deleteFile(path: String) {
        try {
            File(path).delete()
        } catch (e: SecurityException) {
            // Rien à faire : le fichier restera dans le dossier privé de l'app.
        }
    }
}

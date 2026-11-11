package app.spotmycar.android.data

/** D'où vient la position enregistrée. */
enum class LocationSource {
    /** Position obtenue au moment de se garer. */
    GPS,

    /** Dernière position connue avant de perdre le signal (souvent l'entrée du parking). */
    LAST_KNOWN,

    /** Aucune position : cas normal en parking souterrain. */
    NONE,
}

/** Une position obtenue auprès du téléphone. */
data class Fix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val source: LocationSource,
)

/** La place de parking en cours. Tout est facultatif sauf l'heure. */
data class ParkingSpot(
    val savedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val source: LocationSource = LocationSource.NONE,
    val floor: String = "",
    val note: String = "",
    val photoPath: String? = null,
    val auto: Boolean = false,
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    val hasDetails: Boolean
        get() = floor.isNotBlank() || note.isNotBlank() || photoPath != null

    fun withFix(fix: Fix): ParkingSpot = copy(
        latitude = fix.latitude,
        longitude = fix.longitude,
        accuracyMeters = fix.accuracyMeters,
        source = fix.source,
    )
}

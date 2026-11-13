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

/**
 * La place de parking en cours. Tout est facultatif sauf l'heure.
 *
 * Le « modèle garage » : niveau, zone, numéro de place. En sous-sol, ce sont
 * eux (et la photo) qui permettent de retrouver la voiture, pas le GPS.
 */
data class ParkingSpot(
    val savedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val source: LocationSource = LocationSource.NONE,
    /** Niveau, ex. « -2 ». */
    val level: String = "",
    /** Zone, ex. « C ». */
    val zone: String = "",
    /** Numéro de place, ex. « 148 ». */
    val place: String = "",
    /** Repère libre, ex. « près de l'escalier B ». */
    val note: String = "",
    val photoPath: String? = null,
    /** Nom du lieu trouvé à partir du GPS (ex. « Carrefour Anfa »), si disponible. */
    val placeName: String = "",
    /** Vrai si la place a été notée par la coupure Bluetooth. */
    val auto: Boolean = false,
    /** Pression de l'air (hPa) mesurée près de la voiture : sert à estimer l'écart de niveaux au retour. */
    val pressureHpa: Float? = null,
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    /** Vrai si au moins un repère « garage » est rempli. */
    val hasGarageInfo: Boolean
        get() = level.isNotBlank() || zone.isNotBlank() || place.isNotBlank()

    val hasDetails: Boolean
        get() = hasGarageInfo || note.isNotBlank() || photoPath != null

    fun withFix(fix: Fix): ParkingSpot = copy(
        latitude = fix.latitude,
        longitude = fix.longitude,
        accuracyMeters = fix.accuracyMeters,
        source = fix.source,
    )
}

/** Une ancienne place, gardée dans l'historique (sans photo, pour la vie privée et la place). */
data class PastSpot(
    val savedAt: Long,
    val endedAt: Long,
    val level: String,
    val zone: String,
    val place: String,
    val note: String,
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?,
    val auto: Boolean,
)

package app.spotmycar.android.location

import app.spotmycar.android.data.LocationSource
import app.spotmycar.android.data.ParkingSpot

/**
 * La fiabilité de la position enregistrée, de 0 à 100, calculée à partir de ce
 * que le téléphone a réellement capté (GPS, Bluetooth de la voiture, baromètre)
 * et de ce que l'utilisateur a confirmé (niveau, zone, place, photo).
 *
 * Règle du prototype v2 : automatique par défaut ; le mode de secours ne
 * s'active que si la fiabilité est trop basse.
 */
object Reliability {
    enum class Grade { HIGH, MEDIUM, LOW }

    /** En dessous, on demande un geste à l'utilisateur (mode de secours). */
    const val FALLBACK_BELOW = 50

    /** À partir d'ici, la direction est annoncée comme fiable. */
    const val HIGH_FROM = 70

    private const val PRECISE_METERS = 25f
    private const val USABLE_METERS = 60f

    fun score(spot: ParkingSpot): Int {
        var points = 0
        val accuracy = spot.accuracyMeters
        if (spot.hasCoordinates) {
            points += when {
                spot.source == LocationSource.LAST_KNOWN -> 25
                accuracy == null -> 40
                accuracy <= PRECISE_METERS -> 70
                accuracy <= USABLE_METERS -> 50
                else -> 30
            }
        }
        if (spot.auto) points += 10
        if (spot.pressureHpa != null) points += 10
        if (spot.level.isNotBlank()) points += 15
        if (spot.zone.isNotBlank() || spot.place.isNotBlank()) points += 5
        if (spot.photoPath != null) points += 10
        return points.coerceIn(0, 98)
    }

    fun grade(score: Int): Grade = when {
        score >= HIGH_FROM -> Grade.HIGH
        score >= FALLBACK_BELOW -> Grade.MEDIUM
        else -> Grade.LOW
    }

    fun grade(spot: ParkingSpot): Grade = grade(score(spot))

    /** Vrai quand il faut passer par le mode de secours après la détection. */
    fun needsHelp(spot: ParkingSpot): Boolean = score(spot) < FALLBACK_BELOW
}

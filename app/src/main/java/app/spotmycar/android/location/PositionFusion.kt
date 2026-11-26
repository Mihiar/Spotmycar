package app.spotmycar.android.location

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * « Où suis-je maintenant ? », pendant le guidage vers la voiture.
 *
 * Une cascade pensée pour quelqu'un qui marche vers sa voiture, du plus sûr au moins sûr :
 *
 * 1. **GPS précis** (≤ 15 m, frais) : il gagne toujours. Il sert aussi de point de départ aux pas
 *    et à mesurer la longueur réelle du pas de l'utilisateur.
 * 2. **Les pas** : sous terre, chaque pas avance la position dans la direction de la boussole,
 *    à partir du dernier point sûr. L'incertitude grandit à chaque pas.
 * 3. **Position imprécise** (Wi-Fi, antennes : 15 à 60 m) : acceptée seulement si elle est plus
 *    précise que ce qu'on a déjà ET qu'elle ne contredit pas l'estimation par les pas.
 *    Sans podomètre, elle sert à suivre la marche faute de mieux.
 * 4. **Dernière position connue** du téléphone, à l'ouverture : point de départ quand on ouvre
 *    le guidage déjà sous terre. Son incertitude tient compte de son âge (on a pu marcher depuis).
 * 5. Au-delà de 60 m d'incertitude, on ne prétend plus rien : ce sont les repères
 *    (niveau, zone, photo) qui guident.
 *
 * Aucun appel Android ici : la logique est testable seule.
 */
class PositionFusion(strideMeters: Float = DEFAULT_STRIDE) {

    enum class Source {
        /** Position satellite précise. */
        GPS,
        /** Estimée avec les pas depuis le dernier point sûr. */
        STEPS,
        /** Position approximative (Wi-Fi, antennes, ou dernière position connue). */
        APPROXIMATE,
    }

    /** Une position donnée par le téléphone. [ageMillis] : son âge au moment où on la reçoit. */
    data class Fix(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val ageMillis: Long = 0L,
    )

    data class Estimate(
        val latitude: Double,
        val longitude: Double,
        /** Rayon d'incertitude en mètres. */
        val accuracy: Float,
        val source: Source,
    ) {
        /** Vrai quand l'estimation est trop vague pour annoncer une distance honnête. */
        val usable: Boolean get() = accuracy <= MAX_USABLE_METERS
    }

    /** La meilleure estimation actuelle, ou rien. */
    var current: Estimate? = null
        private set

    /** Longueur du pas de l'utilisateur, affinée avec le GPS quand il marche dehors. */
    var stride: Float = strideMeters.coerceIn(MIN_STRIDE, MAX_STRIDE)
        private set

    // Mesure de la longueur du pas : un point GPS très précis, et les pas comptés depuis.
    private var calibrationStart: Fix? = null
    private var stepsSinceCalibration = 0

    /**
     * Dernière position connue du téléphone, proposée à l'ouverture de l'écran.
     * Ignorée dès qu'une position plus récente est arrivée.
     * @return vrai si elle a servi de point de départ.
     */
    fun onLastKnown(fix: Fix): Boolean {
        if (current != null || fix.ageMillis > MAX_LAST_KNOWN_AGE_MILLIS) return false
        // Depuis cette position, on a pu marcher : l'incertitude grandit avec l'âge.
        val accuracy = fix.accuracy + WALKING_SPEED_MPS * fix.ageMillis / 1000f
        if (accuracy > MAX_USABLE_METERS) return false
        current = Estimate(fix.latitude, fix.longitude, accuracy, Source.APPROXIMATE)
        return true
    }

    /**
     * Nouvelle position du téléphone.
     * @return vrai si elle a été retenue.
     */
    fun onFix(fix: Fix): Boolean {
        if (fix.ageMillis > MAX_FIX_AGE_MILLIS) return false // position périmée
        val now = current
        val accepted = when {
            fix.accuracy <= PRECISE_METERS -> true
            now == null -> true // mieux que rien
            now.source == Source.STEPS ->
                // Les pas sont plus sûrs qu'une position vague, sauf si elle est plus précise
                // et cohérente avec eux, ou si les pas ont trop dérivé pour servir encore.
                (fix.accuracy < now.accuracy && agrees(fix, now)) || !now.usable
            fix.accuracy <= now.accuracy -> true
            // Sans les pas, une position un peu moins précise suit quand même la marche.
            else -> fix.accuracy <= MAX_USABLE_METERS
        }
        if (!accepted) return false

        val precise = fix.accuracy <= PRECISE_METERS
        current = Estimate(
            fix.latitude,
            fix.longitude,
            fix.accuracy,
            if (precise) Source.GPS else Source.APPROXIMATE,
        )
        calibrate(fix)
        return true
    }

    /**
     * Un pas détecté, le téléphone pointant vers [bearingDegrees] (0 = nord géographique).
     * @return vrai si la position a avancé (il faut un point de départ).
     */
    fun onStep(bearingDegrees: Float): Boolean {
        stepsSinceCalibration += 1
        val now = current ?: return false
        val (lat, lng) = offset(now.latitude, now.longitude, stride, bearingDegrees)
        current = Estimate(lat, lng, now.accuracy + DRIFT_PER_STEP_METERS, Source.STEPS)
        return true
    }

    /**
     * Longueur du pas mesurée sur une ligne droite au GPS : distance parcourue / nombre de pas.
     * Seulement avec des positions très précises, assez loin l'une de l'autre.
     */
    private fun calibrate(fix: Fix) {
        if (fix.accuracy > CALIBRATION_ACCURACY_METERS) {
            calibrationStart = null
            stepsSinceCalibration = 0
            return
        }
        val start = calibrationStart
        if (start == null) {
            calibrationStart = fix
            stepsSinceCalibration = 0
            return
        }
        val walked = distance(start.latitude, start.longitude, fix.latitude, fix.longitude)
        if (walked < CALIBRATION_MIN_METERS) return // pas encore assez loin
        val steps = stepsSinceCalibration
        if (steps >= CALIBRATION_MIN_STEPS) {
            val measured = walked / steps
            // Une valeur hors des pas humains = trajet non rectiligne ou GPS qui saute : on l'ignore.
            if (measured in MIN_STRIDE..MAX_STRIDE) {
                stride = (stride * 0.7f + measured * 0.3f).coerceIn(MIN_STRIDE, MAX_STRIDE)
            }
        }
        calibrationStart = fix
        stepsSinceCalibration = 0
    }

    /** La position vague tombe-t-elle là où les pas disent qu'on est (aux incertitudes près) ? */
    private fun agrees(fix: Fix, estimate: Estimate): Boolean =
        distance(fix.latitude, fix.longitude, estimate.latitude, estimate.longitude) <=
            fix.accuracy + estimate.accuracy

    companion object {
        /** Position satellite précise : elle gagne toujours. */
        const val PRECISE_METERS = 15f
        /** Au-delà, on n'annonce plus de distance : les repères prennent le relais. */
        const val MAX_USABLE_METERS = 60f
        /** Une position plus vieille que ça ne dit plus où l'on est. */
        const val MAX_FIX_AGE_MILLIS = 10_000L
        /** Dernière position connue acceptée comme point de départ jusqu'à cet âge. */
        const val MAX_LAST_KNOWN_AGE_MILLIS = 3 * 60_000L
        /** Vitesse de marche, pour vieillir la dernière position connue. */
        const val WALKING_SPEED_MPS = 0.3f

        /** Pas moyen d'un adulte, avant la première mesure. */
        const val DEFAULT_STRIDE = 0.7f
        const val MIN_STRIDE = 0.45f
        const val MAX_STRIDE = 0.95f
        /** Erreur ajoutée à chaque pas (longueur de pas et boussole imparfaites). */
        const val DRIFT_PER_STEP_METERS = 0.15f

        const val CALIBRATION_ACCURACY_METERS = 8f
        const val CALIBRATION_MIN_METERS = 30f
        const val CALIBRATION_MIN_STEPS = 25

        private const val EARTH_RADIUS = 6_371_000.0

        /** Distance en mètres (approximation suffisante sur quelques kilomètres). */
        fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
            val meanLat = Math.toRadians((lat1 + lat2) / 2)
            val dNorth = Math.toRadians(lat2 - lat1) * EARTH_RADIUS
            val dEast = Math.toRadians(lng2 - lng1) * EARTH_RADIUS * cos(meanLat)
            return sqrt(dNorth * dNorth + dEast * dEast).toFloat()
        }

        /** Le point situé à [meters] mètres dans la direction [bearingDegrees]. */
        fun offset(lat: Double, lng: Double, meters: Float, bearingDegrees: Float): Pair<Double, Double> {
            val radians = Math.toRadians(bearingDegrees.toDouble())
            val north = meters * cos(radians)
            val east = meters * sin(radians)
            val latRad = Math.toRadians(lat)
            val dLat = Math.toDegrees(north / EARTH_RADIUS)
            val dLng = Math.toDegrees(east / (EARTH_RADIUS * cos(latRad).coerceAtLeast(1e-6)))
            return lat + dLat to lng + dLng
        }
    }
}

package app.spotmycar.android.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Trouve un nom lisible pour l'endroit (ex. « Carrefour Anfa », « Bd d'Anfa »).
 * Bonus seulement : sans Internet ou sans service de géocodage, on n'affiche rien.
 */
object PlaceNamer {
    private const val TIMEOUT_MILLIS = 6_000L

    @Suppress("DEPRECATION")
    suspend fun nameFor(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
        val address = withTimeoutOrNull(TIMEOUT_MILLIS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<Address?> { continuation ->
                    try {
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        })
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } ?: return null
        return pickName(address)
    }

    /** Le nom d'un lieu (centre commercial…) s'il existe, sinon la rue, sinon le quartier. */
    private fun pickName(address: Address): String? {
        val feature = address.featureName?.trim()
        val street = address.thoroughfare?.trim()
        val featureIsUseful = !feature.isNullOrBlank() &&
            feature.any { it.isLetter() } &&
            feature != street &&
            feature != address.subThoroughfare
        return when {
            featureIsUseful -> feature
            !street.isNullOrBlank() -> street
            !address.subLocality.isNullOrBlank() -> address.subLocality
            else -> address.locality
        }?.take(40)
    }
}

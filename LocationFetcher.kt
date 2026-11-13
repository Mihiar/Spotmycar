package app.spotmycar.android.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.spotmycar.android.data.Fix
import app.spotmycar.android.data.LocationSource
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Demande la position au téléphone, sans jamais bloquer l'utilisateur :
 * 1. une position fraîche pendant quelques secondes ;
 * 2. sinon la dernière position connue, si elle est récente (entrée du parking) ;
 * 3. sinon rien. En sous-sol c'est le cas normal, pas une erreur.
 */
object LocationFetcher {
    /** Au-delà, la « dernière position connue » est trop vieille pour être utile. */
    private const val MAX_LAST_KNOWN_AGE_NANOS = 20L * 60L * 1_000_000_000L

    fun hasForegroundPermission(context: Context): Boolean =
        isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** « Toujours autoriser » : nécessaire pour l'enregistrement automatique, app fermée. */
    fun hasBackgroundPermission(context: Context): Boolean {
        if (!hasForegroundPermission(context)) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            isGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Version « coroutine », pour l'écran. */
    suspend fun fetchSuspending(context: Context, timeoutMillis: Long): Fix? =
        suspendCancellableCoroutine { continuation ->
            fetch(context, timeoutMillis) { fix ->
                if (continuation.isActive) continuation.resume(fix)
            }
        }

    /** Version « callback », pour le récepteur Bluetooth. [onResult] est appelé une seule fois. */
    @SuppressLint("MissingPermission")
    fun fetch(context: Context, timeoutMillis: Long, onResult: (Fix?) -> Unit) {
        val appContext = context.applicationContext
        if (!hasForegroundPermission(appContext)) {
            onResult(null)
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(appContext)
        val delivered = AtomicBoolean(false)
        val cancellation = CancellationTokenSource()
        val handler = Handler(Looper.getMainLooper())

        fun deliver(fix: Fix?) {
            if (delivered.compareAndSet(false, true)) {
                handler.removeCallbacksAndMessages(null)
                onResult(fix)
            }
        }

        fun fallBackToLastKnown() {
            if (delivered.get()) return
            try {
                client.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        val usable = location != null && isRecent(location)
                        deliver(if (usable) location!!.toFix(LocationSource.LAST_KNOWN) else null)
                    }
                    .addOnFailureListener { deliver(null) }
            } catch (e: SecurityException) {
                deliver(null)
            }
        }

        // Filet de sécurité : quoi qu'il arrive, on répond.
        handler.postDelayed({
            if (!delivered.get()) {
                cancellation.cancel()
                fallBackToLastKnown()
            }
        }, timeoutMillis + 1_500L)
        handler.postDelayed({ deliver(null) }, timeoutMillis + 4_000L)

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(timeoutMillis)
            .setMaxUpdateAgeMillis(10_000L)
            .build()

        try {
            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) deliver(location.toFix(LocationSource.GPS)) else fallBackToLastKnown()
                }
                .addOnFailureListener { fallBackToLastKnown() }
        } catch (e: SecurityException) {
            deliver(null)
        }
    }

    /** Distance en mètres entre deux points. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    private fun isRecent(location: Location): Boolean =
        SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos <= MAX_LAST_KNOWN_AGE_NANOS

    private fun Location.toFix(source: LocationSource): Fix = Fix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        source = source,
    )
}

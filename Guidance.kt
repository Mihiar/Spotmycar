package app.spotmycar.android.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Ce qu'il faut pour l'écran « Retour à la voiture » : la position en direct
 * et la direction vers laquelle pointe le téléphone (boussole).
 */
object Guidance {

    /** Positions successives, toutes les deux secondes environ, tant que l'écran est ouvert. */
    @SuppressLint("MissingPermission")
    fun liveLocation(context: Context): Flow<Location> = callbackFlow {
        val appContext = context.applicationContext
        if (!LocationFetcher.hasForegroundPermission(appContext)) {
            close()
            return@callbackFlow
        }
        val client = LocationServices.getFusedLocationProviderClient(appContext)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close()
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /**
     * Cap du téléphone en degrés par rapport au nord magnétique (0 = nord, 90 = est).
     * Rien n'est émis si le téléphone n'a pas de boussole.
     */
    fun heading(context: Context): Flow<Float> = callbackFlow {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensors == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothed: Float? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val degrees = normalize(Math.toDegrees(orientation[0].toDouble()).toFloat())
                // Lissage : la boussole tremble beaucoup, on adoucit sans retarder.
                val previous = smoothed
                smoothed = if (previous == null) degrees else normalize(previous + 0.2f * delta(previous, degrees))
                smoothed?.let { trySend(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }

    /** Écart entre nord magnétique et nord géographique à cet endroit. */
    fun declination(location: Location): Float =
        GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination

    /** Ramène un angle entre 0 et 360. */
    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    /** Plus court chemin d'un angle à l'autre, entre -180 et 180. */
    fun delta(from: Float, to: Float): Float {
        var d = normalize(to) - normalize(from)
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    /** 0..7 : nord, nord-est, est… pour dire la direction avec des mots. */
    fun octant(bearing: Float): Int = ((normalize(bearing) + 22.5f) / 45f).toInt() % 8

    fun isClose(distanceMeters: Float, accuracyMeters: Float): Boolean =
        distanceMeters <= maxOf(10f, abs(accuracyMeters).coerceAtMost(25f))
}

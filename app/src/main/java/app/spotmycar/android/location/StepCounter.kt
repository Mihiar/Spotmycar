package app.spotmycar.android.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Le podomètre du téléphone (le même capteur que celui des apps de santé),
 * lu directement : pas de compte Google, rien ne quitte le téléphone.
 *
 * Sert au guidage sous terre : quand le GPS ne passe plus, chaque pas fait
 * avancer la position estimée dans la direction de la boussole.
 */
object StepCounter {
    /** Longueur moyenne d'un pas d'adulte. Une estimation : elle varie selon la personne. */
    const val STRIDE_METERS = 0.7f

    /** Erreur ajoutée à chaque pas (longueur de pas et boussole imparfaites). */
    const val DRIFT_PER_STEP_METERS = 0.15f

    fun isAvailable(context: Context): Boolean =
        context.getSystemService(SensorManager::class.java)
            ?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    /** Android 10 et plus demandent l'autorisation « Activité physique ». */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Émet un signal à chaque pas détecté, tant que l'écran de guidage est ouvert. */
    fun steps(context: Context): Flow<Unit> = callbackFlow {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (sensors == null || sensor == null || !hasPermission(context)) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(Unit)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        awaitClose { sensors.unregisterListener(listener) }
    }

    /**
     * La position un pas plus loin, dans la direction [bearingDegrees] (0 = nord géographique).
     * Son « accuracy » grandit à chaque pas : plus on marche sans GPS, moins on est sûr.
     */
    fun stepFrom(from: Location, bearingDegrees: Float): Location {
        val radians = Math.toRadians(bearingDegrees.toDouble())
        val north = STRIDE_METERS * cos(radians)
        val east = STRIDE_METERS * sin(radians)
        val metersPerDegreeLat = 111_320.0
        val metersPerDegreeLng = (metersPerDegreeLat * cos(Math.toRadians(from.latitude))).coerceAtLeast(1.0)
        return Location("steps").apply {
            latitude = from.latitude + north / metersPerDegreeLat
            longitude = from.longitude + east / metersPerDegreeLng
            accuracy = from.accuracy + DRIFT_PER_STEP_METERS
            time = System.currentTimeMillis()
        }
    }
}

package app.spotmycar.android.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Le podomètre du téléphone (le même capteur que celui des apps de santé),
 * lu directement : pas de compte Google, rien ne quitte le téléphone.
 *
 * Sert au guidage sous terre : quand le GPS ne passe plus, chaque pas fait
 * avancer la position estimée (voir [PositionFusion]).
 */
object StepCounter {
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
}

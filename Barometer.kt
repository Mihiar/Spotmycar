package app.spotmycar.android.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Le baromètre du téléphone (quand il en a un) : la pression de l'air change
 * d'environ 0,12 hPa par mètre de hauteur, donc d'un niveau de parking à l'autre.
 *
 * Deux limites à connaître :
 * - tous les téléphones n'ont pas de baromètre ;
 * - la pression change aussi avec la météo : la comparaison avec la mesure
 *   prise près de la voiture n'est fiable que quelques heures.
 */
object Barometer {
    /** Écart de pression pour un niveau de parking (environ 3 m). */
    const val HPA_PER_LEVEL = 0.36f

    /** Au-delà, la météo a pu changer la pression : on ne compare plus avec la mesure de la voiture. */
    const val MAX_COMPARE_AGE_MILLIS = 2L * 60L * 60L * 1000L

    private const val SAMPLES = 5

    fun isAvailable(context: Context): Boolean =
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    /**
     * Une mesure (moyenne de quelques valeurs), ou null sans baromètre ou après [timeoutMillis].
     * [onResult] est appelé une seule fois, sur le fil principal.
     */
    fun readOnce(context: Context, timeoutMillis: Long, onResult: (Float?) -> Unit) {
        val sensors = context.applicationContext.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (sensors == null || sensor == null) {
            onResult(null)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var delivered = false
        var sum = 0f
        var count = 0
        lateinit var listener: SensorEventListener

        fun deliver() {
            if (delivered) return
            delivered = true
            sensors.unregisterListener(listener)
            handler.removeCallbacksAndMessages(null)
            onResult(if (count > 0) sum / count else null)
        }

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull() ?: return
                if (value <= 0f) return
                sum += value
                count += 1
                if (count >= SAMPLES) deliver()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI, handler)
        handler.postDelayed({ deliver() }, timeoutMillis)
    }

    /** Version « coroutine », pour l'écran. */
    suspend fun readOnceSuspending(context: Context, timeoutMillis: Long): Float? =
        suspendCancellableCoroutine { continuation ->
            readOnce(context, timeoutMillis) { value ->
                if (continuation.isActive) continuation.resume(value)
            }
        }

    /** Pression en direct, lissée, tant que l'écran de guidage est ouvert. Rien n'est émis sans baromètre. */
    fun live(context: Context): Flow<Float> = callbackFlow {
        val sensors = context.applicationContext.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (sensors == null || sensor == null) {
            close()
            return@callbackFlow
        }
        var smoothed: Float? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull() ?: return
                if (value <= 0f) return
                val previous = smoothed
                val next = if (previous == null) value else previous + 0.15f * (value - previous)
                smoothed = next
                trySend(next)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }

    /**
     * Nombre de niveaux à monter (positif) ou à descendre (négatif) pour rejoindre
     * l'endroit où [carHpa] a été mesurée, quand on mesure [hereHpa].
     * Plus on est bas, plus la pression est forte.
     */
    fun levelsToCar(hereHpa: Float, carHpa: Float): Int =
        ((hereHpa - carHpa) / HPA_PER_LEVEL).roundToInt()
}

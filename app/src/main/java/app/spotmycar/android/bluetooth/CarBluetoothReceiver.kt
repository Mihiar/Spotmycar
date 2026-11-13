package app.spotmycar.android.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.data.Fix
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.location.Barometer
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.notify.Notifier

/**
 * Le cœur de SpotMyCar : quand le téléphone se déconnecte du Bluetooth de la
 * voiture, c'est qu'on vient de se garer. On note la place tout seul, sans
 * rien demander à l'utilisateur.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_DISCONNECTED) return

        val settings = AppSettings(context)
        if (!settings.autoSaveEnabled) return
        val carAddress = settings.carAddress ?: return

        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java,
        ) ?: return
        if (!device.address.equals(carAddress, ignoreCase = true)) return

        // Obtenir la position prend quelques secondes : on demande au système de patienter.
        val pending = goAsync()
        val appContext = context.applicationContext
        LocationFetcher.fetch(appContext, FETCH_TIMEOUT_MILLIS) { fix ->
            // Le baromètre (s'il existe) : une seconde de plus, pour estimer l'étage au retour.
            Barometer.readOnce(appContext, PRESSURE_TIMEOUT_MILLIS) { pressure ->
                try {
                    val changed = saveAutomatically(appContext, fix, pressure)
                    if (changed) Notifier.showAutoSaved(appContext, hasPosition = fix != null)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    /** @return vrai si une nouvelle place a été notée (faux si on a gardé l'existante). */
    private fun saveAutomatically(context: Context, fix: Fix?, pressure: Float?): Boolean {
        val now = System.currentTimeMillis()
        val existing = ParkingStore.current(context)

        if (existing != null && isSameParking(existing, fix, now)) {
            // Le Bluetooth s'est reconnecté puis déconnecté au même endroit
            // (contact remis, coffre…) : on garde l'étage, la note et la photo.
            if (fix != null && isBetter(fix, existing)) {
                ParkingStore.update(context) { it.withFix(fix) }
            }
            return false
        }

        val spot = ParkingSpot(savedAt = now, auto = true, pressureHpa = pressure)
        ParkingStore.replace(context, if (fix != null) spot.withFix(fix) else spot)
        return true
    }

    private fun isSameParking(existing: ParkingSpot, fix: Fix?, now: Long): Boolean {
        val existingLat = existing.latitude
        val existingLng = existing.longitude
        if (fix != null && existingLat != null && existingLng != null) {
            val distance = LocationFetcher.distanceMeters(existingLat, existingLng, fix.latitude, fix.longitude)
            return distance <= SAME_PLACE_METERS
        }
        // Sans position pour comparer : même parking si la place vient d'être notée.
        return now - existing.savedAt <= SAME_PLACE_WINDOW_MILLIS
    }

    private fun isBetter(fix: Fix, existing: ParkingSpot): Boolean {
        val newAccuracy = fix.accuracyMeters ?: return false
        val oldAccuracy = existing.accuracyMeters ?: return true
        return newAccuracy < oldAccuracy
    }

    private companion object {
        /** Un récepteur doit répondre en une dizaine de secondes : on reste en dessous. */
        const val FETCH_TIMEOUT_MILLIS = 4_500L
        const val PRESSURE_TIMEOUT_MILLIS = 700L
        const val SAME_PLACE_METERS = 75f
        const val SAME_PLACE_WINDOW_MILLIS = 10L * 60L * 1000L
    }
}

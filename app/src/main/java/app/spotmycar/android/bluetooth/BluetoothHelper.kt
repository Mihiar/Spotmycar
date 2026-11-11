package app.spotmycar.android.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.location.LocationFetcher

/** Un appareil Bluetooth déjà associé au téléphone (ex. l'autoradio de la voiture). */
data class PairedDevice(val name: String, val address: String)

object BluetoothHelper {
    /** À partir d'Android 12, lire les appareils associés demande une autorisation. */
    fun hasConnectPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedDevices(context: Context): List<PairedDevice> {
        if (!hasConnectPermission(context)) return emptyList()
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        val adapter = manager.adapter ?: return emptyList()
        return try {
            adapter.bondedDevices.orEmpty()
                .map { device -> PairedDevice(name = device.name ?: device.address, address = device.address) }
                .sortedBy { it.name.lowercase() }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Vrai quand tout est en place pour que l'enregistrement automatique fonctionne. */
    fun isAutoSaveReady(context: Context): Boolean {
        val settings = AppSettings(context)
        return settings.autoSaveEnabled &&
            settings.carAddress != null &&
            hasConnectPermission(context) &&
            LocationFetcher.hasBackgroundPermission(context)
    }
}

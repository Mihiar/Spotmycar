package app.spotmycar.android.data

import android.content.Context
import android.content.SharedPreferences

/** Réglages de l'enregistrement automatique (gardés sur le téléphone). */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spotmycar_settings", Context.MODE_PRIVATE)

    var autoSaveEnabled: Boolean
        get() = prefs.getBoolean(K_AUTO_SAVE, false)
        set(value) = prefs.edit().putBoolean(K_AUTO_SAVE, value).apply()

    /** Adresse Bluetooth de la voiture choisie par l'utilisateur. */
    var carAddress: String?
        get() = prefs.getString(K_CAR_ADDRESS, null)
        set(value) = prefs.edit().putString(K_CAR_ADDRESS, value).apply()

    var carName: String?
        get() = prefs.getString(K_CAR_NAME, null)
        set(value) = prefs.edit().putString(K_CAR_NAME, value).apply()

    private companion object {
        const val K_AUTO_SAVE = "auto_save_enabled"
        const val K_CAR_ADDRESS = "car_address"
        const val K_CAR_NAME = "car_name"
    }
}

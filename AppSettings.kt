package app.spotmycar.android.data

import android.content.Context
import android.content.SharedPreferences

/** Réglages de l'app (gardés sur le téléphone). */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spotmycar_settings", Context.MODE_PRIVATE)

    // ── Enregistrement automatique par Bluetooth ────────────────────────────
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

    // ── Introduction ────────────────────────────────────────────────────────
    var onboardingDone: Boolean
        get() = prefs.getBoolean(K_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(K_ONBOARDING_DONE, value).apply()

    // ── Alertes ─────────────────────────────────────────────────────────────
    /** « Le parking va fermer » : prévenir 15 minutes avant l'heure de fermeture. */
    var closingAlert: Boolean
        get() = prefs.getBoolean(K_CLOSING_ON, false)
        set(value) = prefs.edit().putBoolean(K_CLOSING_ON, value).apply()

    /** Heure de fermeture, en minutes depuis minuit (23:00 par défaut). */
    var closingMinutes: Int
        get() = prefs.getInt(K_CLOSING_MIN, 23 * 60)
        set(value) = prefs.edit().putInt(K_CLOSING_MIN, value).apply()

    /** « Durée max dépassée » : prévenir après [maxHours] heures de stationnement. */
    var durationAlert: Boolean
        get() = prefs.getBoolean(K_DURATION_ON, false)
        set(value) = prefs.edit().putBoolean(K_DURATION_ON, value).apply()

    var maxHours: Int
        get() = prefs.getInt(K_MAX_HOURS, 3)
        set(value) = prefs.edit().putInt(K_MAX_HOURS, value).apply()

    /** « Stationnement gratuit » : prévenir à l'heure où la rue devient gratuite. */
    var freeAlert: Boolean
        get() = prefs.getBoolean(K_FREE_ON, false)
        set(value) = prefs.edit().putBoolean(K_FREE_ON, value).apply()

    var freeMinutes: Int
        get() = prefs.getInt(K_FREE_MIN, 18 * 60)
        set(value) = prefs.edit().putInt(K_FREE_MIN, value).apply()

    private companion object {
        const val K_AUTO_SAVE = "auto_save_enabled"
        const val K_CAR_ADDRESS = "car_address"
        const val K_CAR_NAME = "car_name"
        const val K_ONBOARDING_DONE = "onboarding_done"
        const val K_CLOSING_ON = "alert_closing"
        const val K_CLOSING_MIN = "alert_closing_minutes"
        const val K_DURATION_ON = "alert_duration"
        const val K_MAX_HOURS = "alert_max_hours"
        const val K_FREE_ON = "alert_free"
        const val K_FREE_MIN = "alert_free_minutes"
    }
}

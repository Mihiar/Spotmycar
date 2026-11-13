package app.spotmycar.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Historique des places précédentes, gardé sur le téléphone (SharedPreferences,
 * au format JSON). Les plus récentes d'abord, au plus [MAX_ENTRIES].
 */
object HistoryStore {
    private const val PREFS = "spotmycar_history"
    private const val K_ENTRIES = "entries"
    private const val MAX_ENTRIES = 50

    private val state = MutableStateFlow<List<PastSpot>>(emptyList())
    val entries: StateFlow<List<PastSpot>> = state.asStateFlow()

    private var loaded = false

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        state.value = read(prefs(context))
        loaded = true
    }

    /** Ajoute une place terminée en tête de l'historique. */
    @Synchronized
    fun add(context: Context, spot: ParkingSpot, endedAt: Long) {
        ensureLoaded(context)
        val entry = PastSpot(
            savedAt = spot.savedAt,
            endedAt = endedAt,
            level = spot.level,
            zone = spot.zone,
            place = spot.place,
            note = spot.note,
            placeName = spot.placeName,
            latitude = spot.latitude,
            longitude = spot.longitude,
            auto = spot.auto,
        )
        val updated = (listOf(entry) + state.value).take(MAX_ENTRIES)
        write(prefs(context), updated)
        state.value = updated
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().remove(K_ENTRIES).apply()
        state.value = emptyList()
        loaded = true
    }

    private fun read(prefs: SharedPreferences): List<PastSpot> {
        val raw = prefs.getString(K_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val o = array.optJSONObject(index) ?: return@mapNotNull null
                PastSpot(
                    savedAt = o.optLong("savedAt"),
                    endedAt = o.optLong("endedAt"),
                    level = o.optString("level"),
                    zone = o.optString("zone"),
                    place = o.optString("place"),
                    note = o.optString("note"),
                    placeName = o.optString("placeName"),
                    latitude = if (o.has("lat")) o.optDouble("lat") else null,
                    longitude = if (o.has("lng")) o.optDouble("lng") else null,
                    auto = o.optBoolean("auto"),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun write(prefs: SharedPreferences, list: List<PastSpot>) {
        val array = JSONArray()
        list.forEach { s ->
            val o = JSONObject()
                .put("savedAt", s.savedAt)
                .put("endedAt", s.endedAt)
                .put("level", s.level)
                .put("zone", s.zone)
                .put("place", s.place)
                .put("note", s.note)
                .put("placeName", s.placeName)
                .put("auto", s.auto)
            s.latitude?.let { o.put("lat", it) }
            s.longitude?.let { o.put("lng", it) }
            array.put(o)
        }
        prefs.edit().putString(K_ENTRIES, array.toString()).apply()
    }
}

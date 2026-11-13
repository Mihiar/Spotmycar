package app.spotmycar.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import java.io.File
import java.util.Locale

/** Tout ce qui ouvre une autre application : cartes, partage, réglages du téléphone. */
object Actions {
    /** À compléter avec l'adresse définitive de la page (GitHub Pages). */
    const val PRIVACY_POLICY_URL = "https://spotmycar.example/privacy.html"

    private fun coordinates(spot: ParkingSpot): String? {
        val lat = spot.latitude ?: return null
        val lng = spot.longitude ?: return null
        // Locale.US : toujours des chiffres latins et un point décimal, même en arabe ou en français.
        return String.format(Locale.US, "%.6f,%.6f", lat, lng)
    }

    private fun mapsLink(spot: ParkingSpot): String? =
        coordinates(spot)?.let { "https://maps.google.com/?q=$it" }

    /** Ouvre l'itinéraire à pied vers la voiture. @return faux si aucune app de cartes. */
    fun navigateTo(context: Context, spot: ParkingSpot): Boolean {
        val coords = coordinates(spot) ?: return false
        val attempts = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coords&mode=w"))
                .setPackage("com.google.android.apps.maps"),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$coords?q=$coords")),
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$coords&travelmode=walking"),
            ),
        )
        return attempts.any { start(context, it) }
    }

    /** Le message envoyé à un proche : lien de la carte, niveau / zone / place, repère. */
    fun shareText(context: Context, spot: ParkingSpot): String {
        val link = mapsLink(spot)
        return buildString {
            if (link != null) {
                append(context.getString(R.string.share_with_coords, link))
            } else {
                append(context.getString(R.string.share_without_coords))
            }
            val garage = SpotText.garageLine(context, spot)
            if (garage.isNotBlank()) append('\n').append(garage)
            if (spot.note.isNotBlank()) {
                append('\n').append(context.getString(R.string.share_note, spot.note))
            }
        }
    }

    /** Partage la place (texte et photo) avec l'application de son choix. */
    fun share(context: Context, spot: ParkingSpot) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText(context, spot))
        }
        val photo = spot.photoPath?.let { File(it) }
        if (photo != null && photo.exists()) {
            try {
                val uri = Photos.uriFor(context, photo)
                send.type = "image/jpeg"
                send.putExtra(Intent.EXTRA_STREAM, uri)
                send.clipData = ClipData.newRawUri(null, uri)
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: IllegalArgumentException) {
                // Photo introuvable par le FileProvider : on partage le texte seul.
            }
        }
        start(context, Intent.createChooser(send, context.getString(R.string.share_chooser)))
    }

    /** Envoie le message par WhatsApp (ou WhatsApp Business). @return faux si absent. */
    fun shareToWhatsApp(context: Context, text: String): Boolean =
        listOf("com.whatsapp", "com.whatsapp.w4b").any { pkg ->
            start(
                context,
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .setPackage(pkg),
            )
        }

    /** Ouvre un SMS prêt à envoyer. */
    fun shareBySms(context: Context, text: String): Boolean =
        start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text))

    fun openUrl(context: Context, url: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun openAppSettings(context: Context): Boolean = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )

    fun openBluetoothSettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) || openAppSettings(context)

    fun openBatterySettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) || openAppSettings(context)

    /**
     * Écran « démarrage automatique » des marques qui arrêtent les apps en
     * arrière-plan (Xiaomi, Oppo, Vivo, Huawei). À défaut : la fiche de l'app.
     */
    fun openAutostartSettings(context: Context): Boolean {
        val screens = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        )
        return screens.any { start(context, Intent().setComponent(it)) } || openAppSettings(context)
    }

    private fun start(context: Context, intent: Intent): Boolean {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}

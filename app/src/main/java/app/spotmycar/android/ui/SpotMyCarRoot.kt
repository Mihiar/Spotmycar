package app.spotmycar.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.location.LocationFetcher
import kotlinx.coroutines.launch

/** Écran ouvert par-dessus l'écran principal. */
private enum class Overlay { NONE, DETAILS, SETTINGS }

/** Les quatre écrans de l'app. */
private enum class Screen { PARK, DETAILS, FIND, SETTINGS }

/**
 * Chef d'orchestre : décide quel écran montrer.
 * - pas de place notée  -> « Garer ici »
 * - place notée         -> « Ma voiture »
 * - par-dessus          -> détails (étage, photo) ou réglages
 */
@Composable
fun SpotMyCarRoot(
    resumeTick: Int,
    openDetailsRequest: Boolean,
    onOpenDetailsConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spot by ParkingStore.spot.collectAsState()
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    var locating by remember { mutableStateOf(false) }

    // Garde la dernière place affichée, pour que l'écran ne se vide pas pendant le fondu de sortie.
    val lastSpot = remember { arrayOfNulls<ParkingSpot>(1) }
    spot?.let { lastSpot[0] = it }
    val shownSpot = spot ?: lastSpot[0]

    // Arrivée depuis la notification « Place enregistrée » : on ouvre directement les détails.
    LaunchedEffect(openDetailsRequest) {
        if (openDetailsRequest) {
            if (ParkingStore.current(context) != null) overlay = Overlay.DETAILS
            onOpenDetailsConsumed()
        }
    }

    BackHandler(enabled = overlay != Overlay.NONE) { overlay = Overlay.NONE }

    // Plus de place notée : l'écran des détails n'a plus lieu d'être.
    LaunchedEffect(spot) {
        if (spot == null && overlay == Overlay.DETAILS) overlay = Overlay.NONE
    }

    val onParked: () -> Unit = {
        // 1. On note la place tout de suite : l'utilisateur n'attend jamais le GPS.
        val newSpot = ParkingSpot(savedAt = System.currentTimeMillis())
        ParkingStore.replace(context, newSpot)
        overlay = Overlay.DETAILS

        // 2. La position arrive ensuite, si le téléphone en trouve une.
        if (LocationFetcher.hasForegroundPermission(context)) {
            locating = true
            scope.launch {
                val fix = LocationFetcher.fetchSuspending(context, 12_000L)
                if (fix != null) {
                    ParkingStore.update(context) { current ->
                        if (current.savedAt == newSpot.savedAt) current.withFix(fix) else current
                    }
                }
                locating = false
            }
        }
    }

    val screen = when {
        overlay == Overlay.SETTINGS -> Screen.SETTINGS
        spot == null -> Screen.PARK
        overlay == Overlay.DETAILS -> Screen.DETAILS
        else -> Screen.FIND
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Crossfade(targetState = screen, label = "screen") { current ->
                when (current) {
                    Screen.PARK -> ParkScreen(
                        resumeTick = resumeTick,
                        onParked = onParked,
                        onOpenSettings = { overlay = Overlay.SETTINGS },
                    )

                    Screen.DETAILS -> shownSpot?.let { parked ->
                        DetailsScreen(
                            spot = parked,
                            locating = locating,
                            onDone = { overlay = Overlay.NONE },
                        )
                    }

                    Screen.FIND -> shownSpot?.let { parked ->
                        FindScreen(
                            spot = parked,
                            onEdit = { overlay = Overlay.DETAILS },
                            onOpenSettings = { overlay = Overlay.SETTINGS },
                            onFound = {
                                overlay = Overlay.NONE
                                ParkingStore.clear(context)
                            },
                        )
                    }

                    Screen.SETTINGS -> SettingsScreen(
                        resumeTick = resumeTick,
                        onBack = { overlay = Overlay.NONE },
                    )
                }
            }
        }
    }
}

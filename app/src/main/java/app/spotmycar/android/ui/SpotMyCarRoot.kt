package app.spotmycar.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.data.HistoryStore
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.location.PlaceNamer
import app.spotmycar.android.ui.theme.Smc
import kotlinx.coroutines.launch

/** Les écrans de l'app (comme dans le prototype 3a). */
enum class Screen { ONBOARDING, HOME, HISTORY, ALERTS, SAVE, GUIDE, SETTINGS }

/** Les trois onglets du bas. */
private val TAB_SCREENS = setOf(Screen.HOME, Screen.HISTORY, Screen.ALERTS)

/**
 * Chef d'orchestre : décide quel écran montrer.
 * Introduction (une fois) → Ma voiture / Historique / Alertes (onglets)
 * → par-dessus : Nouvelle place, Retour à la voiture, Réglages, photo, partage.
 */
@Composable
fun SpotMyCarRoot(
    resumeTick: Int,
    openDetailsRequest: Boolean,
    onOpenDetailsConsumed: () -> Unit,
    openGuideRequest: Boolean,
    onOpenGuideConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { AppSettings(context) }
    val spot by ParkingStore.spot.collectAsState()
    val history by HistoryStore.entries.collectAsState()

    var screen by rememberSaveable {
        mutableStateOf(if (settings.onboardingDone) Screen.HOME else Screen.ONBOARDING)
    }
    /** Vrai quand l'écran « Nouvelle place » vient d'une place créée à l'instant. */
    var saveIsNew by rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var photoOpen by rememberSaveable { mutableStateOf(false) }
    var shareOpen by rememberSaveable { mutableStateOf(false) }

    // Garde la dernière place affichée, pour que l'écran ne se vide pas pendant le fondu de sortie.
    val lastSpot = remember { arrayOfNulls<ParkingSpot>(1) }
    spot?.let { lastSpot[0] = it }
    val shownSpot = spot ?: lastSpot[0]

    // Arrivée depuis la notification « Place enregistrée » : on ouvre l'écran de la place.
    LaunchedEffect(openDetailsRequest) {
        if (openDetailsRequest) {
            if (ParkingStore.current(context) != null) {
                saveIsNew = false
                screen = Screen.SAVE
            }
            onOpenDetailsConsumed()
        }
    }
    // Arrivée depuis une alerte : directement le guidage.
    LaunchedEffect(openGuideRequest) {
        if (openGuideRequest) {
            if (ParkingStore.current(context) != null) screen = Screen.GUIDE
            onOpenGuideConsumed()
        }
    }

    // Plus de place notée : les écrans qui en dépendent n'ont plus lieu d'être.
    LaunchedEffect(spot) {
        if (spot == null && (screen == Screen.SAVE || screen == Screen.GUIDE)) screen = Screen.HOME
        if (spot == null) {
            photoOpen = false
            shareOpen = false
        }
    }

    // Nom du lieu (« Carrefour Anfa ») : trouvé après coup, quand Internet le permet.
    LaunchedEffect(spot?.savedAt, spot?.hasCoordinates) {
        val current = spot ?: return@LaunchedEffect
        val lat = current.latitude ?: return@LaunchedEffect
        val lng = current.longitude ?: return@LaunchedEffect
        if (current.placeName.isNotBlank()) return@LaunchedEffect
        val name = PlaceNamer.nameFor(context, lat, lng) ?: return@LaunchedEffect
        ParkingStore.update(context) { if (it.savedAt == current.savedAt) it.copy(placeName = name) else it }
    }

    val goHome: () -> Unit = {
        photoOpen = false
        shareOpen = false
        screen = Screen.HOME
    }

    BackHandler(enabled = photoOpen || shareOpen || (screen != Screen.HOME && screen != Screen.ONBOARDING)) {
        when {
            photoOpen -> photoOpen = false
            shareOpen -> shareOpen = false
            else -> screen = Screen.HOME
        }
    }

    // « Enregistrer ma place » : la place est notée tout de suite, la position arrive ensuite.
    val onParked: () -> Unit = {
        val newSpot = ParkingSpot(savedAt = System.currentTimeMillis())
        ParkingStore.replace(context, newSpot)
        saveIsNew = true
        screen = Screen.SAVE

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Smc.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = screen, label = "screen") { current ->
                    when (current) {
                        Screen.ONBOARDING -> OnboardingScreen(
                            onFinished = {
                                settings.onboardingDone = true
                                screen = Screen.HOME
                            },
                        )

                        Screen.HOME -> HomeScreen(
                            spot = spot,
                            locating = locating,
                            history = history,
                            resumeTick = resumeTick,
                            onParked = onParked,
                            onGuide = { screen = Screen.GUIDE },
                            onEdit = {
                                saveIsNew = false
                                screen = Screen.SAVE
                            },
                            onOpenPhoto = { photoOpen = true },
                            onShare = { shareOpen = true },
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onOpenHistory = { screen = Screen.HISTORY },
                        )

                        Screen.HISTORY -> HistoryScreen(spot = spot, history = history)

                        Screen.ALERTS -> AlertsScreen(
                            spot = spot,
                            resumeTick = resumeTick,
                            onOpenSettings = { screen = Screen.SETTINGS },
                        )

                        Screen.SAVE -> shownSpot?.let { parked ->
                            SaveScreen(
                                spot = parked,
                                isNew = saveIsNew,
                                locating = locating,
                                onCancel = { discardNew ->
                                    if (discardNew) ParkingStore.discard(context)
                                    goHome()
                                },
                                onSaved = goHome,
                            )
                        }

                        Screen.GUIDE -> shownSpot?.let { parked ->
                            GuideScreen(
                                spot = parked,
                                onBack = goHome,
                                onOpenPhoto = { photoOpen = true },
                                onArrived = {
                                    ParkingStore.finish(context)
                                    goHome()
                                },
                            )
                        }

                        Screen.SETTINGS -> SettingsScreen(
                            resumeTick = resumeTick,
                            onBack = goHome,
                            onReplayIntro = {
                                settings.onboardingDone = false
                                screen = Screen.ONBOARDING
                            },
                        )
                    }
                }
            }
            if (screen in TAB_SCREENS) {
                BottomTabs(
                    current = screen,
                    onSelect = { target ->
                        photoOpen = false
                        shareOpen = false
                        screen = target
                    },
                )
            }
        }

        val overlaySpot = shownSpot
        if (photoOpen && overlaySpot != null) {
            PhotoViewer(spot = overlaySpot, onClose = { photoOpen = false })
        }
        if (shareOpen && overlaySpot != null) {
            ShareSheet(spot = overlaySpot, onClose = { shareOpen = false })
        }
    }
}

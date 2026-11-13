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
import app.spotmycar.android.location.Barometer
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.location.PlaceNamer
import app.spotmycar.android.location.Reliability
import app.spotmycar.android.ui.theme.Smc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Les écrans de l'app (parcours du prototype v2, plus Historique, Alertes et Réglages). */
enum class Screen {
    SPLASH, ONBOARDING, HOME, HISTORY, ALERTS,
    DETECT, LOCATED, FALLBACK, SAVE, SAVED,
    FIND, GUIDE, ARRIVED,
    SETTINGS,
}

/** Les écrans qui n'ont de sens que s'il y a une voiture enregistrée. */
private val SPOT_SCREENS = setOf(
    Screen.DETECT, Screen.LOCATED, Screen.FALLBACK, Screen.SAVE, Screen.SAVED,
    Screen.FIND, Screen.GUIDE, Screen.ARRIVED,
)

/** Les trois onglets du bas. */
private val TAB_SCREENS = setOf(Screen.HOME, Screen.HISTORY, Screen.ALERTS)

/**
 * Chef d'orchestre : décide quel écran montrer.
 * Lancement → Introduction (une fois) → Accueil / Historique / Alertes (onglets).
 * « Park » : Détection auto → Voiture localisée (ou Mode de secours → repères) → Enregistrée.
 * « Find » : Recherche → Guidage → Trouvée !
 * Par-dessus : Réglages, photo, partage.
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

    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    // L'écran de lancement ne dure qu'un instant.
    LaunchedEffect(Unit) {
        if (screen == Screen.SPLASH) {
            delay(1_500L)
            if (screen == Screen.SPLASH) {
                screen = if (settings.onboardingDone) Screen.HOME else Screen.ONBOARDING
            }
        }
    }
    /** Vrai quand l'écran « Nouvelle place » vient d'une place créée à l'instant. */
    var saveIsNew by rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    /** Vrai pendant la mesure du baromètre, juste après « Park my car ». */
    var measuringPressure by remember { mutableStateOf(false) }
    /** Vrai quand on arrive sur les repères par « Prendre une photo » (mode de secours). */
    var saveWithPhoto by rememberSaveable { mutableStateOf(false) }
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
                saveWithPhoto = false
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
        if (spot == null && screen in SPOT_SCREENS) screen = Screen.HOME
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

    BackHandler(
        enabled = photoOpen || shareOpen ||
            (screen != Screen.HOME && screen != Screen.ONBOARDING && screen != Screen.SPLASH),
    ) {
        when {
            photoOpen -> photoOpen = false
            shareOpen -> shareOpen = false
            else -> screen = Screen.HOME
        }
    }

    // « Park my car » : la place est notée tout de suite, puis le téléphone écoute ses
    // capteurs (GPS, baromètre) pendant que l'écran « Détection automatique » s'affiche.
    val onParked: () -> Unit = {
        val newSpot = ParkingSpot(savedAt = System.currentTimeMillis())
        ParkingStore.replace(context, newSpot)
        saveIsNew = true
        saveWithPhoto = false
        screen = Screen.DETECT

        measuringPressure = true
        scope.launch {
            val pressure = Barometer.readOnceSuspending(context, 1_500L)
            if (pressure != null) {
                ParkingStore.update(context) { current ->
                    if (current.savedAt == newSpot.savedAt) current.copy(pressureHpa = pressure) else current
                }
            }
            measuringPressure = false
        }

        if (LocationFetcher.hasForegroundPermission(context)) {
            locating = true
            scope.launch {
                val fix = LocationFetcher.fetchSuspending(context, 7_000L)
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
                        Screen.SPLASH -> SplashScreen()

                        Screen.DETECT -> shownSpot?.let { parked ->
                            DetectScreen(
                                spot = parked,
                                locating = locating,
                                measuringPressure = measuringPressure,
                                resumeTick = resumeTick,
                                onFinished = {
                                    // Automatique par défaut ; le mode de secours seulement si la fiabilité est trop basse.
                                    if (screen == Screen.DETECT) {
                                        val latest = ParkingStore.current(context)
                                        screen = when {
                                            latest == null -> Screen.HOME
                                            Reliability.needsHelp(latest) -> Screen.FALLBACK
                                            else -> Screen.LOCATED
                                        }
                                    }
                                },
                            )
                        }

                        Screen.LOCATED -> shownSpot?.let { parked ->
                            LocatedScreen(
                                spot = parked,
                                onConfirm = { screen = Screen.SAVED },
                                onCorrect = {
                                    saveWithPhoto = false
                                    screen = Screen.SAVE
                                },
                            )
                        }

                        Screen.FALLBACK -> FallbackScreen(
                            onConfirmLevel = {
                                saveWithPhoto = false
                                screen = Screen.SAVE
                            },
                            onTakePhoto = {
                                saveWithPhoto = true
                                screen = Screen.SAVE
                            },
                            onSkip = { screen = Screen.SAVED },
                        )

                        Screen.SAVED -> shownSpot?.let { parked ->
                            SavedScreen(spot = parked, onDone = goHome)
                        }

                        Screen.FIND -> shownSpot?.let { parked ->
                            FindScreen(
                                spot = parked,
                                onFinished = { if (screen == Screen.FIND) screen = Screen.GUIDE },
                            )
                        }

                        Screen.ARRIVED -> shownSpot?.let { parked ->
                            ArrivedScreen(
                                spot = parked,
                                onDone = {
                                    ParkingStore.finish(context)
                                    goHome()
                                },
                                onParkAnother = {
                                    ParkingStore.finish(context)
                                    onParked()
                                },
                            )
                        }

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
                            onFind = { screen = Screen.FIND },
                            onEdit = {
                                saveIsNew = false
                                saveWithPhoto = false
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
                                startWithPhoto = saveWithPhoto,
                                onCancel = { discardNew ->
                                    if (discardNew) ParkingStore.discard(context)
                                    goHome()
                                },
                                // Après une nouvelle place : l'écran « Votre voiture est enregistrée ! ».
                                onSaved = { if (saveIsNew) screen = Screen.SAVED else goHome() },
                            )
                        }

                        Screen.GUIDE -> shownSpot?.let { parked ->
                            GuideScreen(
                                spot = parked,
                                onBack = goHome,
                                onOpenPhoto = { photoOpen = true },
                                onArrived = { screen = Screen.ARRIVED },
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

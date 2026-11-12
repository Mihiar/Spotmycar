package app.spotmycar.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import app.spotmycar.android.data.HistoryStore
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.notify.Notifier
import app.spotmycar.android.ui.SpotMyCarRoot
import app.spotmycar.android.ui.theme.SpotMyCarTheme

/** L'unique écran Android de l'app : tout le reste est dessiné avec Compose. */
class MainActivity : ComponentActivity() {
    /** Augmente à chaque retour dans l'app : sert à revérifier les autorisations. */
    private val resumeTick = mutableIntStateOf(0)

    /** Vrai quand on arrive depuis la notification « Place enregistrée ». */
    private val openDetailsRequest = mutableStateOf(false)

    /** Vrai quand on arrive depuis une alerte (fermeture, durée…). */
    private val openGuideRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Thème toujours sombre : barres système claires sur fond graphite.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        ParkingStore.ensureLoaded(this)
        HistoryStore.ensureLoaded(this)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            SpotMyCarTheme {
                SpotMyCarRoot(
                    resumeTick = resumeTick.intValue,
                    openDetailsRequest = openDetailsRequest.value,
                    onOpenDetailsConsumed = { openDetailsRequest.value = false },
                    openGuideRequest = openGuideRequest.value,
                    onOpenGuideConsumed = { openGuideRequest.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue = resumeTick.intValue + 1
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(Notifier.EXTRA_OPEN_DETAILS, false) == true) {
            openDetailsRequest.value = true
        }
        if (intent?.getBooleanExtra(Notifier.EXTRA_OPEN_GUIDE, false) == true) {
            openGuideRequest.value = true
        }
    }
}

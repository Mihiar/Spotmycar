package app.spotmycar.android.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import app.spotmycar.android.util.Actions

/**
 * Une demande d'autorisation qui sait quoi faire après un refus définitif.
 *
 * Après deux refus, Android n'affiche plus sa fenêtre : la demande échoue aussitôt,
 * sans rien montrer. [request] ouvre alors la fiche de l'app dans les paramètres,
 * seul endroit où l'autorisation peut encore être accordée.
 */
class PermissionGate internal constructor(
    /** Vrai quand Android ne propose plus la fenêtre : le bouton doit dire « Ouvrir les paramètres ». */
    val blocked: Boolean,
    val request: () -> Unit,
)

/** En dessous de cette durée, personne n'a pu répondre : c'est Android qui a refusé tout seul. */
private const val INSTANT_DENIAL_MS = 500L

@Composable
fun rememberPermissionGate(
    vararg permissions: String,
    onResult: (granted: Boolean) -> Unit = {},
): PermissionGate {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult by rememberUpdatedState(onResult)

    var blocked by rememberSaveable { mutableStateOf(false) }
    /** Android demandait-il une explication avant la demande ? (signe d'un premier refus) */
    var hadRationale by rememberSaveable { mutableStateOf(false) }
    var launchedAt by rememberSaveable { mutableLongStateOf(0L) }

    fun shouldExplain(): Boolean = activity != null &&
        permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            blocked = false
        } else if (!shouldExplain()) {
            val instant = SystemClock.elapsedRealtime() - launchedAt < INSTANT_DENIAL_MS
            // Refus sans fenêtre, ou deuxième refus : la fenêtre ne reviendra plus.
            if (instant || hadRationale) blocked = true
            // Rien ne s'est affiché : on ne laisse pas l'utilisateur devant un bouton sans effet.
            if (instant) Actions.openAppSettings(context)
        }
        currentOnResult(granted)
    }

    return PermissionGate(blocked = blocked) {
        if (blocked) {
            Actions.openAppSettings(context)
        } else {
            hadRationale = shouldExplain()
            launchedAt = SystemClock.elapsedRealtime()
            launcher.launch(arrayOf(*permissions))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

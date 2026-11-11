package app.spotmycar.android.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.spotmycar.android.R
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.location.LocationFetcher

/**
 * Écran d'accueil, volontairement presque vide : un seul geste possible.
 * (Loi de Hick : moins de choix, décision plus rapide au moment de se garer.)
 */
@Composable
fun ParkScreen(
    resumeTick: Int,
    onParked: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var dropping by remember { mutableStateOf(false) }
    val autoSaveReady = remember(resumeTick) { BluetoothHelper.isAutoSaveReady(context) }

    // Qu'on accepte ou qu'on refuse la position, la place est notée :
    // sans GPS, l'étage et la photo font le travail.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { dropping = true }

    val onTap: () -> Unit = {
        if (!dropping) {
            if (LocationFetcher.hasForegroundPermission(context)) {
                dropping = true
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(title = stringResource(R.string.app_name), onOpenSettings = onOpenSettings)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.park_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PinDropButton(
                label = stringResource(R.string.park_button),
                dropping = dropping,
                onClick = onTap,
                onDropFinished = onParked,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.park_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (autoSaveReady) {
            Text(
                text = stringResource(R.string.park_auto_on),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        } else {
            TextButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_bluetooth),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(text = stringResource(R.string.park_auto_cta))
            }
        }
    }
}

/**
 * Le gros bouton rond. Au repos, des cercles s'éloignent doucement ;
 * à l'appui, l'épingle tombe et une onde part du centre.
 */
@Composable
private fun PinDropButton(
    label: String,
    dropping: Boolean,
    onClick: () -> Unit,
    onDropFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    val currentOnDropFinished by rememberUpdatedState(onDropFinished)

    val idle = rememberInfiniteTransition(label = "idle")
    val pulse by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val pinDrop = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    LaunchedEffect(dropping) {
        if (dropping) {
            pinDrop.snapTo(-1f)
            pinDrop.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            )
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
            currentOnDropFinished()
        }
    }

    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2f
            val inner = outer * 0.62f
            for (index in 0 until 3) {
                val progress = (pulse + index / 3f) % 1f
                drawCircle(
                    color = primary.copy(alpha = (1f - progress) * 0.28f),
                    radius = inner + (outer - inner) * progress,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            val wave = burst.value
            if (wave > 0f && wave < 1f) {
                drawCircle(
                    color = accent.copy(alpha = (1f - wave) * 0.45f),
                    radius = inner + (outer - inner) * wave,
                )
            }
        }
        Surface(
            onClick = onClick,
            modifier = Modifier.size(188.dp),
            shape = CircleShape,
            color = primary,
            shadowElevation = 10.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(68.dp)
                        .offset(y = (pinDrop.value * 110f).dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

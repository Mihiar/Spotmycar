package app.spotmycar.android.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.util.Actions
import kotlinx.coroutines.delay
import java.util.Date

/**
 * L'écran « retrouver » : le moment qui compte le plus (règle du pic et de la fin).
 * Le repère le plus utile est en grand, l'itinéraire est à un appui.
 */
@Composable
fun FindScreen(
    spot: ParkingSpot,
    onEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onFound: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnFound by rememberUpdatedState(onFound)

    // L'heure affichée (« il y a 2 heures ») se met à jour toute seule.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    var showPhoto by remember { mutableStateOf(false) }

    // « J'ai retrouvé ma voiture » : l'épingle fait un petit saut avant de disparaître.
    var leaving by remember { mutableStateOf(false) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (leaving) {
            hop.animateTo(-1f, tween(durationMillis = 170, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            currentOnFound()
        }
    }

    val photoPath = spot.photoPath

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.find_title), onOpenSettings = onOpenSettings)
        Spacer(Modifier.height(8.dp))

        if (photoPath != null) {
            SpotPhoto(
                path = photoPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showPhoto = true },
            )
        } else {
            PinHero(hop = hop.value)
        }

        Spacer(Modifier.height(20.dp))

        if (spot.floor.isNotBlank()) {
            Text(
                text = stringResource(R.string.floor_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = spot.floor,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (spot.note.isNotBlank()) {
            Text(
                text = spot.note,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
        }
        if (!spot.hasDetails) {
            Text(
                text = stringResource(R.string.find_no_details),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = parkedSince(spot, now),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (spot.auto) {
            Text(
                text = stringResource(R.string.find_auto_badge),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(Modifier.height(12.dp))
        LocationStatusLine(spot = spot, locating = false)
        Spacer(Modifier.height(20.dp))

        // Boutons empilés, du plus utile au moins utile.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (spot.hasCoordinates) {
                Button(
                    onClick = {
                        if (!Actions.navigateTo(context, spot)) {
                            Toast.makeText(context, R.string.error_no_maps, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_navigation),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.find_navigate),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (!spot.hasDetails) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(text = stringResource(R.string.find_add_details), style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedButton(
                onClick = { Actions.share(context, spot) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text = stringResource(R.string.find_share), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = { if (!leaving) leaving = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.find_found),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (spot.hasDetails) {
                TextButton(onClick = onEdit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.find_edit))
                }
            }
        }
    }

    if (showPhoto && photoPath != null) {
        Dialog(
            onDismissRequest = { showPhoto = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showPhoto = false },
                contentAlignment = Alignment.Center,
            ) {
                SpotPhoto(
                    path = photoPath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

/** « Garée il y a 2 heures, à 14:32 ». */
@Composable
private fun parkedSince(spot: ParkingSpot, now: Long): String {
    val context = LocalContext.current
    val elapsed = now - spot.savedAt
    val relative = if (elapsed < DateUtils.MINUTE_IN_MILLIS) {
        stringResource(R.string.time_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(spot.savedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    val clock = DateFormat.getTimeFormat(context).format(Date(spot.savedAt))
    return stringResource(R.string.find_parked, relative) + ", " + stringResource(R.string.find_parked_at, clock)
}

/** Sans photo : l'épingle, avec des cercles qui reviennent vers elle. */
@Composable
private fun PinHero(hop: Float, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.secondary
    val idle = rememberInfiniteTransition(label = "return")
    val pulse by idle.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2f
            val inner = outer * 0.3f
            for (index in 0 until 3) {
                val progress = (pulse + index / 3f) % 1f
                drawCircle(
                    color = accent.copy(alpha = (1f - progress) * 0.4f),
                    radius = inner + (outer - inner) * progress,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(92.dp)
                .offset(y = (hop * 40f).dp),
        )
    }
}

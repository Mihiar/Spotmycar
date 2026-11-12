package app.spotmycar.android.ui

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.location.Guidance
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.ui.theme.BarlowCondensed
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.Actions
import java.text.NumberFormat
import kotlin.math.roundToInt

private val DIRECTIONS = listOf(
    R.string.dir_n, R.string.dir_ne, R.string.dir_e, R.string.dir_se,
    R.string.dir_s, R.string.dir_sw, R.string.dir_w, R.string.dir_nw,
)

/**
 * « Retour à la voiture » : la flèche et la distance en direct quand il y a du
 * GPS, et toujours les repères (niveau, zone, place, photo) pour le sous-sol.
 */
@Composable
fun GuideScreen(
    spot: ParkingSpot,
    onBack: () -> Unit,
    onOpenPhoto: () -> Unit,
    onArrived: () -> Unit,
) {
    val context = LocalContext.current
    var permissionTick by remember { mutableIntStateOf(0) }
    val hasPermission = remember(permissionTick) { LocationFetcher.hasForegroundPermission(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionTick += 1 }

    var here by remember { mutableStateOf<Location?>(null) }
    var heading by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(hasPermission, spot.hasCoordinates) {
        if (hasPermission && spot.hasCoordinates) Guidance.liveLocation(context).collect { here = it }
    }
    LaunchedEffect(spot.hasCoordinates) {
        if (spot.hasCoordinates) Guidance.heading(context).collect { heading = it }
    }

    // Calculs : distance, direction, flèche.
    val target = remember(spot.latitude, spot.longitude) {
        val lat = spot.latitude
        val lng = spot.longitude
        if (lat != null && lng != null) Location("spot").apply { latitude = lat; longitude = lng } else null
    }
    val position = here
    val distance = if (target != null && position != null) position.distanceTo(target) else null
    val bearing = if (target != null && position != null) position.bearingTo(target) else null
    val compass = heading
    val arrowAngle = when {
        bearing == null -> null
        compass != null && position != null -> bearing - (compass + Guidance.declination(position))
        else -> bearing
    }
    val arrived = distance != null && position != null && Guidance.isClose(distance, position.accuracy)

    // La flèche tourne en douceur, toujours par le plus court chemin.
    val arrow = remember { Animatable(0f) }
    LaunchedEffect(arrowAngle) {
        if (arrowAngle != null) {
            val goal = arrow.value + Guidance.delta(arrow.value, arrowAngle)
            arrow.animateTo(goal, tween(durationMillis = 250))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Smc.Muted,
                )
            }
            Spacer(Modifier.width(4.dp))
            BarTitle(text = stringResource(R.string.guide_title))
        }

        // La boussole.
        Spacer(Modifier.height(14.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Smc.SurfaceHigh, Smc.Surface)))
                    .border(1.dp, Smc.Off, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (arrowAngle != null && !arrived) {
                    Icon(
                        painter = painterResource(R.drawable.ic_navigation),
                        contentDescription = null,
                        tint = Smc.Amber,
                        modifier = Modifier
                            .size(128.dp)
                            .rotate(arrow.value),
                    )
                } else {
                    // Pas de GPS (ou arrivé) : les ondes reviennent vers l'épingle.
                    PinRings(inward = true, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = Smc.Amber,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = distance?.let { formatDistance(it) } ?: "—",
                color = Smc.Text,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 66.sp,
                lineHeight = 70.sp,
            )
            Text(
                text = guideStatus(spot, hasPermission, position, bearing, arrived, compass != null).uppercase(),
                color = Smc.Dim,
                fontSize = 13.5.sp,
                letterSpacing = tracking(0.1f),
                textAlign = TextAlign.Center,
            )
        }

        if (spot.hasCoordinates && !hasPermission) {
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = stringResource(R.string.guide_allow_location),
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                textColor = Smc.Amber,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Les niveaux du parking, celui de la voiture en ambre.
        if (spot.level.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                levelStrip(spot.level).forEach { level ->
                    val isCar = level == spot.level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SmcShape)
                            .background(if (isCar) Smc.Amber else Smc.SurfaceHigh)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = level,
                            color = if (isCar) Smc.OnAmber else Smc.Faint,
                            fontFamily = BarlowCondensed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                }
            }
        }

        // Les étapes, du plus général au plus précis.
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val steps = guideSteps(spot)
            if (steps.isEmpty()) {
                StepLine(text = stringResource(R.string.guide_no_landmarks), lit = false)
            } else {
                steps.forEach { StepLine(text = it, lit = true) }
            }
        }

        // Les actions, empilées : la plus utile en premier.
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (arrived) {
                AmberButton(text = stringResource(R.string.guide_arrived_button), onClick = onArrived)
            } else if (spot.hasCoordinates) {
                AmberButton(
                    text = stringResource(R.string.guide_maps),
                    onClick = {
                        if (!Actions.navigateTo(context, spot)) {
                            Toast.makeText(context, R.string.error_no_maps, Toast.LENGTH_LONG).show()
                        }
                    },
                    icon = painterResource(R.drawable.ic_navigation),
                )
            }
            if (spot.photoPath != null) {
                GhostButton(
                    text = stringResource(R.string.guide_photo),
                    onClick = onOpenPhoto,
                    icon = painterResource(R.drawable.ic_camera),
                    textColor = Smc.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!arrived) {
                GhostButton(
                    text = stringResource(R.string.guide_arrived_button),
                    onClick = onArrived,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StepLine(text: String, lit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = if (lit) Smc.Amber else Smc.BorderStrong)
        Spacer(Modifier.width(12.dp))
        Text(text = text, color = if (lit) Smc.Text else Smc.Muted, fontSize = 15.sp)
    }
}

@Composable
private fun guideSteps(spot: ParkingSpot): List<String> = buildList {
    if (spot.level.isNotBlank()) add(stringResource(R.string.guide_step_level, spot.level))
    if (spot.zone.isNotBlank()) add(stringResource(R.string.guide_step_zone, spot.zone))
    if (spot.place.isNotBlank()) add(stringResource(R.string.guide_step_place, spot.place))
    if (spot.note.isNotBlank()) add(stringResource(R.string.guide_step_note, spot.note))
}

@Composable
private fun guideStatus(
    spot: ParkingSpot,
    hasPermission: Boolean,
    position: Location?,
    bearing: Float?,
    arrived: Boolean,
    hasCompass: Boolean,
): String = when {
    !spot.hasCoordinates -> stringResource(R.string.guide_no_gps)
    !hasPermission -> stringResource(R.string.guide_need_location)
    position == null || bearing == null -> stringResource(R.string.guide_locating)
    arrived -> stringResource(R.string.guide_arrived)
    hasCompass -> stringResource(R.string.guide_direction, stringResource(DIRECTIONS[Guidance.octant(bearing)]))
    else -> stringResource(R.string.guide_direction_no_compass, stringResource(DIRECTIONS[Guidance.octant(bearing)]))
}

/** « 38 m » ou « 1,2 km ». */
@Composable
private fun formatDistance(meters: Float): String =
    if (meters < 1000f) {
        stringResource(R.string.distance_m, meters.roundToInt())
    } else {
        val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
        stringResource(R.string.distance_km, format.format(meters / 1000f))
    }

/** Quatre niveaux autour de celui de la voiture, du plus haut au plus bas. */
private fun levelStrip(carLevel: String): List<String> {
    val number = carLevel.trim().toIntOrNull() ?: return listOf(carLevel)
    val top = if (number >= 0) number + 1 else minOf(0, number + 2)
    return (0 until 4).map { (top - it).toString() }
}

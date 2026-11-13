package app.spotmycar.android.ui

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.location.Barometer
import app.spotmycar.android.location.Guidance
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.location.Reliability
import app.spotmycar.android.ui.theme.Jakarta
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.Actions
import app.spotmycar.android.util.SpotText
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val DIRECTIONS = listOf(
    R.string.dir_n, R.string.dir_ne, R.string.dir_e, R.string.dir_se,
    R.string.dir_s, R.string.dir_sw, R.string.dir_w, R.string.dir_nw,
)

/** En dessous de cette incertitude, la direction est annoncée comme fiable. */
private const val RELIABLE_METERS = 25f

/**
 * Guidage (prototype v2) : des pas mènent vers la voiture, la distance en gros,
 * et la fiabilité toujours visible. Sans GPS, les repères (niveau, zone, place,
 * photo) prennent le relais. Le baromètre signale un changement d'étage.
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
    /** Niveaux à monter (positif) ou à descendre (négatif), d'après le baromètre. */
    var levels by remember { mutableIntStateOf(0) }

    LaunchedEffect(hasPermission, spot.hasCoordinates) {
        if (hasPermission && spot.hasCoordinates) Guidance.liveLocation(context).collect { here = it }
    }
    LaunchedEffect(spot.hasCoordinates) {
        if (spot.hasCoordinates) Guidance.heading(context).collect { heading = it }
    }
    // Baromètre : comparaison avec la mesure prise près de la voiture, tant qu'elle est récente
    // (au-delà de quelques heures, la météo fausse la comparaison).
    val carPressure = spot.pressureHpa
    LaunchedEffect(carPressure, spot.savedAt) {
        val recent = System.currentTimeMillis() - spot.savedAt <= Barometer.MAX_COMPARE_AGE_MILLIS
        if (carPressure != null && recent) {
            Barometer.live(context).collect { pressure ->
                val raw = (pressure - carPressure) / Barometer.HPA_PER_LEVEL
                // Marge anti-tremblement : on ne change d'avis que si l'écart est net.
                if (abs(raw - levels) > 0.7f) levels = raw.roundToInt()
            }
        }
    }

    // Calculs : distance, direction, angle des pas.
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

    // Fiabilité de la direction : celle de la place enregistrée et celle de la position actuelle.
    val uncertainty = if (position != null) maxOf(spot.accuracyMeters ?: 0f, position.accuracy) else null
    val reliable = uncertainty != null &&
        uncertainty <= RELIABLE_METERS &&
        Reliability.grade(spot) == Reliability.Grade.HIGH

    // Les pas tournent en douceur, toujours par le plus court chemin.
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
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Smc.Text,
                )
            }
            Spacer(Modifier.width(4.dp))
            BarTitle(text = stringResource(R.string.guide_title))
        }

        // Ce qu'on sait de la place, puis la fiabilité (toujours visible pendant le guidage).
        val garage = SpotText.garageLine(context, spot)
        if (garage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = garage,
                color = Smc.Text,
                fontFamily = Jakarta,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .softShadow(SmcPillShape, 4.dp)
                    .clip(SmcPillShape)
                    .background(Smc.Surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        when {
            !spot.hasCoordinates -> Pill(
                text = stringResource(R.string.guide_conf_landmarks),
                background = Smc.WarnSoft,
                color = Smc.OnWarnSoft,
                dot = Smc.WarnBright,
            )
            uncertainty == null -> Pill(
                text = stringResource(if (hasPermission) R.string.guide_locating else R.string.guide_need_location),
                background = Smc.WarnSoft,
                color = Smc.OnWarnSoft,
                dot = Smc.WarnBright,
            )
            reliable -> Pill(
                text = stringResource(R.string.guide_conf_high),
                background = Smc.SuccessSoft,
                color = Smc.OnSuccessSoft,
                dot = Smc.Success,
            )
            else -> Pill(
                text = stringResource(R.string.guide_conf_est, uncertainty.roundToInt()),
                background = Smc.WarnSoft,
                color = Smc.OnWarnSoft,
                dot = Smc.WarnBright,
            )
        }

        if (levels != 0) {
            Spacer(Modifier.height(14.dp))
            FloorCard(levels = levels, carLevel = spot.level)
        }

        // Les pas vers la voiture (ou les ondes, sans GPS).
        Spacer(Modifier.height(10.dp))
        if (arrowAngle != null && !arrived) {
            FootstepTrail(angle = arrow.value, dimmed = !reliable)
        } else {
            PinRings(inward = true, color = Smc.Violet, modifier = Modifier.size(220.dp)) {
                CarBeacon(color = Smc.Violet)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = distance?.let { (if (reliable) "" else "≈ ") + formatDistance(it) } ?: "—",
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp,
            lineHeight = 48.sp,
        )
        Text(
            text = guideStatus(spot, hasPermission, position, bearing, arrived, compass != null),
            color = Smc.Muted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )

        if (spot.hasCoordinates && !hasPermission) {
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = stringResource(R.string.guide_allow_location),
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                textColor = Smc.Blue,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Les repères, du plus général au plus précis.
        Spacer(Modifier.height(18.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val steps = guideSteps(spot)
                if (steps.isEmpty()) {
                    StepLine(text = stringResource(R.string.guide_no_landmarks), lit = false)
                } else {
                    steps.forEach { StepLine(text = it, lit = true) }
                }
            }
        }

        // Les actions, empilées : la plus utile en premier.
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (arrived) {
                PrimaryButton(text = stringResource(R.string.guide_arrived_button), onClick = onArrived)
            } else if (spot.hasCoordinates) {
                PrimaryButton(
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
                if (spot.hasCoordinates) {
                    GhostButton(
                        text = stringResource(R.string.guide_arrived_button),
                        onClick = onArrived,
                        textColor = Smc.Blue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PrimaryButton(text = stringResource(R.string.guide_arrived_button), onClick = onArrived)
                }
            }
        }
    }
}

/** La voiture seule, au centre du guidage (la spirale a été retirée à cette étape). */
@Composable
private fun CarBeacon(color: Color, modifier: Modifier = Modifier, iconRotation: Float = 0f) {
    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(56.dp)
                .rotate(iconRotation),
        )
    }
}

/**
 * Des empreintes de pas qui partent de vous (en bas) vers la voiture (en haut).
 * L'ensemble tourne de [angle] degrés : 0 = la voiture est droit devant.
 */
@Composable
private fun FootstepTrail(angle: Float, dimmed: Boolean) {
    val transition = rememberInfiniteTransition(label = "steps")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000, easing = LinearEasing), RepeatMode.Restart),
        label = "steps-wave",
    )
    Box(
        modifier = Modifier
            .size(310.dp)
            .rotate(angle),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 8
            val startY = size.height * 0.95f
            val endY = size.height * 0.40f
            for (i in 0 until count) {
                val t = i / (count - 1f)
                val side = if (i % 2 == 0) -1f else 1f
                val footScale = 1.5f + (0.65f - 1.5f) * t
                var opacity = if (t <= wave) maxOf(0.2f, 1f - (wave - t) * 1.3f) else 0.12f
                if (dimmed) opacity *= if (t > 0.55f) 0.45f else 0.8f
                val color = when {
                    t < 0.34f -> Color(0xFF1F5FFF)
                    t < 0.67f -> Color(0xFF4F7BFF)
                    else -> Color(0xFF9AA9FF)
                }
                drawFoot(
                    x = size.width / 2f + side * 12.dp.toPx() * footScale,
                    y = startY + (endY - startY) * t,
                    footScale = footScale,
                    tilt = side * 9f,
                    color = color.copy(alpha = opacity.coerceIn(0f, 1f)),
                )
            }
        }
        // L'icône reste droite, même quand l'ensemble tourne.
        CarBeacon(color = Smc.Blue, iconRotation = -angle, modifier = Modifier.offset(y = 6.dp))
    }
}

/** Une empreinte : un ovale (la plante) et un rond (le talon), dessinés. */
private fun DrawScope.drawFoot(x: Float, y: Float, footScale: Float, tilt: Float, color: Color) {
    val soleWidth = 13.dp.toPx() * footScale
    val soleHeight = 20.dp.toPx() * footScale
    val heel = 5.dp.toPx() * footScale
    val gap = 2.dp.toPx() * footScale
    withTransform({ rotate(degrees = tilt, pivot = Offset(x, y)) }) {
        drawOval(
            color = color,
            topLeft = Offset(x - soleWidth / 2f, y - soleHeight / 2f),
            size = Size(soleWidth, soleHeight),
        )
        drawCircle(color = color, radius = heel, center = Offset(x, y + soleHeight / 2f + gap + heel))
    }
}

/** « Montez de 2 niveaux » : l'écart d'étage estimé par le baromètre. */
@Composable
private fun FloorCard(levels: Int, carLevel: String) {
    val up = levels > 0
    val transition = rememberInfiniteTransition(label = "floor")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1760, easing = LinearEasing), RepeatMode.Restart),
        label = "floor-bob",
    )
    val carNumber = carLevel.trim().toIntOrNull()
    SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Smc.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = null,
                        tint = Smc.Blue,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = (sin(bob) * 3f).dp)
                            .rotate(if (up) 0f else 180f),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (up) R.string.guide_up else R.string.guide_down, abs(levels)),
                        color = Smc.Text,
                        fontFamily = Jakarta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_height),
                            contentDescription = null,
                            tint = Smc.Dim,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(text = stringResource(R.string.guide_floor_source), color = Smc.Dim, fontSize = 12.sp)
                    }
                }
            }
            if (carNumber != null) {
                Spacer(Modifier.height(10.dp))
                FloorLine(
                    label = stringResource(R.string.guide_floor_now),
                    value = (carNumber - levels).toString(),
                    color = Smc.Muted,
                )
                HorizontalDivider(color = Smc.Border)
                FloorLine(label = stringResource(R.string.guide_floor_car), value = carNumber.toString(), color = Smc.Blue)
            }
        }
    }
}

@Composable
private fun FloorLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_parking),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = Smc.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = color, fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun StepLine(text: String, lit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = if (lit) Smc.Blue else Smc.BorderStrong)
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
    hasCompass -> stringResource(R.string.guide_follow_steps)
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

package app.spotmycar.android.ui

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import app.spotmycar.android.ui.theme.Outfit
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.TabularNums
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.Actions
import app.spotmycar.android.util.SpotText
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private val DIRECTIONS = listOf(
    R.string.dir_n, R.string.dir_ne, R.string.dir_e, R.string.dir_se,
    R.string.dir_s, R.string.dir_sw, R.string.dir_w, R.string.dir_nw,
)

/** En dessous de cette incertitude, la direction est annoncée comme fiable. */
private const val RELIABLE_METERS = 25f

/** Vitesse de marche retenue pour le temps estimé (spec : 1,2 m/s). */
private const val WALK_METERS_PER_SECOND = 1.2f

/** Nombre d'empreintes sur la ligne, de vous jusqu'à la voiture. */
private const val STEP_COUNT = 9

/** Côté de la scène (pas + voiture) : elle tourne sur elle-même vers la voiture. */
private val STAGE = 320.dp

/** Distance entre le centre de la scène et la voiture. */
private val CAR_FROM_CENTER = 116.dp

/**
 * Aller à ma voiture, variante « Ligne droite » (Spotmy Car 4c) : pas de carte,
 * des pas en ligne droite vers une grande voiture vue de dessus, les mètres
 * restants en gros. Un pas n'apparaît que là où vous êtes déjà passé ; le
 * dernier pas est votre position. Toute la ligne tourne vers la voiture
 * (boussole). Arrivée : « Trouvée ! », la voiture pulse et ses phares clignotent.
 *
 * Sans GPS, les repères (niveau, zone, place, photo) prennent le relais.
 * Le baromètre signale un changement d'étage.
 */
@Composable
fun GuideScreen(
    spot: ParkingSpot,
    onBack: () -> Unit,
    onOpenPhoto: () -> Unit,
    onArrived: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
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

    // Calculs : distance, direction, angle de la ligne de pas.
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

    // Progression p (0 → 1) : la plus grande distance vue sert de point de départ.
    var startDistance by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(distance) {
        val d = distance ?: return@LaunchedEffect
        val start = startDistance
        if (start == null || d > start) startDistance = d
    }
    val progress = run {
        val d = distance ?: return@run null
        val start = startDistance ?: d
        if (start <= 0f) 1f else (1f - d / start).coerceIn(0f, 1f)
    }
    // Pas déjà posés : jamais devant vous. Le dernier = votre position.
    val shown = when {
        arrived -> STEP_COUNT
        progress == null -> 0
        else -> (floor(progress * (STEP_COUNT - 1)).toInt() + 1).coerceAtMost(STEP_COUNT)
    }

    // La ligne tourne en douceur, toujours par le plus court chemin.
    val turn = remember { Animatable(0f) }
    LaunchedEffect(arrowAngle) {
        if (arrowAngle != null) {
            val goal = turn.value + Guidance.delta(turn.value, arrowAngle)
            turn.animateTo(goal, tween(durationMillis = 250))
        }
    }

    // Arrivée : une vibration, une seule fois.
    LaunchedEffect(arrived) {
        if (arrived) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // En-tête : retour, et la place au centre (« P2 · Place 14 »).
        val garage = SpotText.garageLine(context, spot)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Smc.Text,
                )
            }
            Text(
                text = garage.ifBlank { stringResource(R.string.guide_title) },
                color = Smc.Muted,
                fontFamily = Outfit,
                fontSize = 15.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 56.dp),
            )
        }

        // La fiabilité, toujours visible pendant le guidage.
        Spacer(Modifier.height(4.dp))
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

        // Toi → pas → voiture.
        Spacer(Modifier.height(8.dp))
        StraightTrail(
            angle = if (arrowAngle != null) turn.value else 0f,
            shown = shown,
            arrived = arrived,
            dimmed = !reliable,
        )

        // Les mètres restants en gros, ou « Trouvée ! ».
        if (arrived) {
            Text(
                text = stringResource(R.string.arrived_title),
                color = Smc.Text,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                letterSpacing = tracking(-0.02f),
            )
        } else {
            Text(
                text = distance?.let { (if (reliable) "" else "≈ ") + formatDistance(it) } ?: "—",
                color = Smc.Text,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 52.sp,
                letterSpacing = tracking(-0.03f),
                style = TextStyle(fontFeatureSettings = TabularNums),
            )
        }
        val eta = distance?.takeIf { !arrived }?.let { meters ->
            val minutes = maxOf(1, ceil(meters / WALK_METERS_PER_SECOND / 60f).toInt())
            stringResource(R.string.guide_eta_min, minutes)
        }
        Text(
            text = listOfNotNull(eta, guideStatus(spot, hasPermission, position, bearing, arrived, compass != null))
                .joinToString(" · "),
            color = Smc.Muted,
            fontFamily = Outfit,
            fontSize = 15.sp,
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
                textColor = Smc.AccentDeep,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Les repères, du plus général au plus précis.
        Spacer(Modifier.height(20.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
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
        Spacer(Modifier.height(16.dp))
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
                        textColor = Smc.AccentDeep,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PrimaryButton(text = stringResource(R.string.guide_arrived_button), onClick = onArrived)
                }
            }
        }
    }
}

/**
 * La scène 4c : une ligne pointillée, les pas déjà faits (de vous, en bas, vers
 * la voiture, en haut) et la voiture. L'ensemble tourne de [angle] degrés
 * (0 = la voiture est droit devant) ; la voiture, elle, reste droite.
 */
@Composable
private fun StraightTrail(angle: Float, shown: Int, arrived: Boolean, dimmed: Boolean) {
    // Le nouveau pas arrive un peu plus grand (1,45 → 1), en ≈ 250 ms.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(shown) {
        if (shown > 0 && !arrived) {
            pop.snapTo(1.45f)
            pop.animateTo(1f, tween(durationMillis = 250, easing = LinearOutSlowInEasing))
        }
    }
    val radians = angle * PI.toFloat() / 180f

    Box(modifier = Modifier.size(STAGE), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle),
        ) {
            val cx = size.width / 2f
            val userY = size.height - 20.dp.toPx()
            val lastStepY = size.height / 2f - CAR_FROM_CENTER.toPx() + 62.dp.toPx()

            // La ligne droite jusqu'à la voiture.
            drawLine(
                color = Smc.Border,
                start = Offset(cx, userY + 14.dp.toPx()),
                end = Offset(cx, lastStepY - 18.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
            )

            // Les pas faits : gauche / droite, ±10 dp, ±6°. Les plus anciens s'estompent.
            val newest = shown - 1
            for (i in 0 until shown) {
                val t = i / (STEP_COUNT - 1f)
                val side = if (i % 2 == 0) -1f else 1f
                val fade = maxOf(0.3f, 1f - (newest - i) * 0.07f)
                drawFoot(
                    x = cx + side * 10.dp.toPx(),
                    y = userY + (lastStepY - userY) * t,
                    footScale = if (i == newest && !arrived) pop.value else 1f,
                    tilt = side * 6f,
                    color = Smc.Accent.copy(alpha = if (dimmed) fade * 0.6f else fade),
                )
            }
        }
        TopViewCar(
            arrived = arrived,
            modifier = Modifier.offset(
                x = CAR_FROM_CENTER * sin(radians),
                y = -CAR_FROM_CENTER * cos(radians),
            ),
        )
    }
}

/**
 * La voiture vue de dessus, dessinée (spec, section 3) : carrosserie terracotta,
 * pare-brise, lunette arrière, deux phares, et deux anneaux qui pulsent.
 * À l'arrivée, elle « respire » et ses phares clignotent toutes les 140 ms.
 */
@Composable
private fun TopViewCar(arrived: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "car")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing), RepeatMode.Restart),
        label = "car-pulse",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1005, easing = LinearEasing), RepeatMode.Restart),
        label = "car-breath",
    )
    var lightsOn by remember { mutableStateOf(false) }
    LaunchedEffect(arrived) {
        lightsOn = false
        while (arrived) {
            lightsOn = !lightsOn
            delay(140L)
        }
    }
    val accent = Smc.Accent
    val headlight = Smc.Headlight
    val glass = Color(0xFF2A1A12)

    Box(modifier = modifier.size(92.dp), contentAlignment = Alignment.Center) {
        // Deux anneaux décalés d'une demi-période : échelle 1 → 2,6, opacité 0,7 → 0.
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (phase in floatArrayOf(pulse, (pulse + 0.5f) % 1f)) {
                drawCircle(
                    color = accent.copy(alpha = (1f - phase) * 0.7f),
                    radius = (size.minDimension / 2f) * (1f + phase * 1.6f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        val bodyShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .scale(if (arrived) 1f + 0.08f * sin(breath) else 1f)
                .size(width = 46.dp, height = 80.dp)
                .shadow(10.dp, bodyShape, clip = false, ambientColor = accent, spotColor = accent),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(color = accent, cornerRadius = CornerRadius(16.dp.toPx()))
                // Pare-brise et lunette arrière.
                drawRoundRect(
                    color = glass.copy(alpha = 0.45f),
                    topLeft = Offset(6.dp.toPx(), 17.dp.toPx()),
                    size = Size(size.width - 12.dp.toPx(), 15.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
                drawRoundRect(
                    color = glass.copy(alpha = 0.35f),
                    topLeft = Offset(8.dp.toPx(), size.height - 19.dp.toPx()),
                    size = Size(size.width - 16.dp.toPx(), 9.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                // Phares : éteints pendant la marche, clignotants à l'arrivée.
                if (arrived) {
                    val alpha = if (lightsOn) 1f else 0.15f
                    val r = 5.5.dp.toPx()
                    for (x in floatArrayOf(5.dp.toPx() + r, size.width - 5.dp.toPx() - r)) {
                        val center = Offset(x, -8.dp.toPx() + r)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(headlight.copy(alpha = 0.9f * alpha), Color.Transparent),
                                center = center,
                                radius = r + 14.dp.toPx(),
                            ),
                            radius = r + 14.dp.toPx(),
                            center = center,
                        )
                        drawCircle(color = headlight.copy(alpha = alpha), radius = r, center = center)
                    }
                }
            }
        }
    }
}

/** Une empreinte (spec) : la plante, un ovale de 10 × 16 dp, et le talon, un rond de 8 dp, 2 dp plus bas. */
private fun DrawScope.drawFoot(x: Float, y: Float, footScale: Float, tilt: Float, color: Color) {
    val soleWidth = 10.dp.toPx()
    val soleHeight = 16.dp.toPx()
    val heel = 4.dp.toPx()
    val gap = 2.dp.toPx()
    val top = y - (soleHeight + gap + heel * 2f) / 2f
    withTransform({
        rotate(degrees = tilt, pivot = Offset(x, y))
        scale(footScale, footScale, pivot = Offset(x, y))
    }) {
        drawOval(color = color, topLeft = Offset(x - soleWidth / 2f, top), size = Size(soleWidth, soleHeight))
        drawCircle(color = color, radius = heel, center = Offset(x, top + soleHeight + gap + heel))
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
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Smc.AccentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_up),
                        contentDescription = null,
                        tint = Smc.AccentDeep,
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
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
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
                        Text(text = stringResource(R.string.guide_floor_source), color = Smc.Dim, fontSize = 13.sp)
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
                FloorLine(label = stringResource(R.string.guide_floor_car), value = carNumber.toString(), color = Smc.AccentDeep)
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
        Text(text = label, color = Smc.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = color, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun StepLine(text: String, lit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = if (lit) Smc.Accent else Smc.BorderStrong)
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
    arrived -> stringResource(R.string.arrived_body)
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

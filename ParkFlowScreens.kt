package app.spotmycar.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.data.LocationSource
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.location.Reliability
import app.spotmycar.android.ui.theme.Jakarta
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.SpotText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Le parcours « Park my car » du prototype v2 :
 * Détection automatique → Voiture localisée (ou Mode de secours) → Voiture enregistrée.
 * Automatique par défaut : aucune saisie. Le mode de secours ne s'affiche que si
 * la fiabilité est trop basse, et ne demande qu'un seul geste.
 */

/** Écran de lancement : la spirale tourne, le nom, la phrase d'accroche. */
@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 10_800, easing = LinearEasing), RepeatMode.Restart),
        label = "splash-spin",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF1FF), Color(0xFFCFE0FF)))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spiral(
            color = Smc.Blue,
            strokeFraction = 0.055f,
            modifier = Modifier
                .size(120.dp)
                .rotate(angle),
        )
        Spacer(Modifier.height(22.dp))
        Wordmark(size = 30.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.splash_tagline),
            color = Smc.Dim,
            fontFamily = Jakarta,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Les ondes autour de la voiture, avec le titre et le sous-titre au-dessus. */
@Composable
fun RingsHeader(title: String, subtitle: String, color: Color, soft: Color, inward: Boolean) {
    Text(
        text = title,
        color = Smc.Text,
        fontFamily = Jakarta,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        color = Smc.Dim,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PinRings(inward = inward, color = color, modifier = Modifier.size(220.dp)) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(soft),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, clip = false, ambientColor = color, spotColor = color)
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_car),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

/**
 * « Détection automatique… » : on montre ce que le téléphone capte vraiment
 * (GPS, Bluetooth de la voiture, baromètre). Dès que tout a répondu, on passe à la suite.
 */
@Composable
fun DetectScreen(
    spot: ParkingSpot,
    locating: Boolean,
    measuringPressure: Boolean,
    resumeTick: Int,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val autoSaveReady = remember(resumeTick) { BluetoothHelper.isAutoSaveReady(context) }
    val hasPermission = remember(resumeTick) { LocationFetcher.hasForegroundPermission(context) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    val done = !locating && !measuringPressure
    LaunchedEffect(done) {
        if (done) {
            // On laisse le temps de lire le résultat avant de changer d'écran.
            delay(1_600L)
            currentOnFinished()
        }
    }

    val accuracy = spot.accuracyMeters
    val gpsState: SignalState
    val gpsDetail: String
    when {
        locating && !spot.hasCoordinates -> {
            gpsState = SignalState.PENDING
            gpsDetail = stringResource(R.string.gps_short_searching)
        }
        spot.hasCoordinates && spot.source == LocationSource.LAST_KNOWN -> {
            gpsState = SignalState.WARN
            gpsDetail = stringResource(R.string.signal_gps_entrance)
        }
        spot.hasCoordinates -> {
            gpsState = SignalState.OK
            gpsDetail = stringResource(R.string.signal_gps_ok, (accuracy ?: 0f).roundToInt())
        }
        !hasPermission -> {
            gpsState = SignalState.NA
            gpsDetail = stringResource(R.string.signal_gps_denied)
        }
        else -> {
            gpsState = SignalState.WARN
            gpsDetail = stringResource(R.string.signal_gps_none)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 44.dp, bottom = 24.dp),
    ) {
        RingsHeader(
            title = stringResource(R.string.detect_title),
            subtitle = stringResource(R.string.detect_sub),
            color = Smc.Blue,
            soft = Smc.BlueSoft,
            inward = false,
        )
        Spacer(Modifier.height(10.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                SignalRow(
                    icon = rememberVectorPainter(Icons.Filled.LocationOn),
                    label = stringResource(R.string.signal_gps),
                    detail = gpsDetail,
                    state = gpsState,
                )
                SignalRow(
                    icon = painterResource(R.drawable.ic_bluetooth),
                    label = stringResource(R.string.signal_bt),
                    detail = stringResource(if (autoSaveReady) R.string.signal_bt_ready else R.string.signal_bt_off),
                    state = if (autoSaveReady) SignalState.OK else SignalState.NA,
                )
                SignalRow(
                    icon = painterResource(R.drawable.ic_height),
                    label = stringResource(R.string.signal_baro),
                    detail = stringResource(
                        when {
                            measuringPressure -> R.string.signal_checking
                            spot.pressureHpa != null -> R.string.signal_baro_ok
                            else -> R.string.signal_baro_none
                        },
                    ),
                    state = when {
                        measuringPressure -> SignalState.PENDING
                        spot.pressureHpa != null -> SignalState.OK
                        else -> SignalState.NA
                    },
                )
            }
        }
    }
}

/** « Voiture localisée automatiquement » : la fiabilité, et rien à saisir. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocatedScreen(spot: ParkingSpot, onConfirm: () -> Unit, onCorrect: () -> Unit) {
    val context = LocalContext.current
    val accuracy = spot.accuracyMeters
    val chips = buildList {
        if (spot.placeName.isNotBlank()) add(spot.placeName)
        if (spot.hasCoordinates && accuracy != null) add(stringResource(R.string.located_gps_chip, accuracy.roundToInt()))
        if (spot.level.isNotBlank()) add(stringResource(R.string.garage_level, spot.level))
        if (spot.zone.isNotBlank()) add(stringResource(R.string.garage_zone, spot.zone))
        add(stringResource(R.string.saved_at, SpotText.clock(context, spot.savedAt)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 56.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PopBadge(
            icon = rememberVectorPainter(Icons.Filled.Place),
            size = 72.dp,
            brush = SolidColor(Smc.BlueSoft),
            tint = Smc.Blue,
        )
        Spacer(Modifier.height(18.dp))
        PopBadge(
            icon = rememberVectorPainter(Icons.Filled.Check),
            size = 34.dp,
            brush = SolidColor(Smc.Blue),
            tint = Color.White,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.located_title),
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                ReliabilityBar(score = Reliability.score(spot))
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    chips.forEach { chip ->
                        Text(
                            text = chip,
                            color = Color(0xFF1D4ED8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(SmcPillShape)
                                .background(Smc.SurfaceHigh)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = stringResource(R.string.located_ok), onClick = onConfirm)
        TextButton(onClick = onCorrect, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = stringResource(R.string.located_fix),
                color = Smc.Blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** « Position incertaine » : le mode de secours, un seul geste. */
@Composable
fun FallbackScreen(onConfirmLevel: () -> Unit, onTakePhoto: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 64.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PopBadge(
            icon = rememberVectorPainter(Icons.Filled.Search),
            size = 72.dp,
            brush = SolidColor(Smc.WarnSoft),
            tint = Smc.Warn,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.fallback_title),
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fallback_body),
            color = Smc.Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))
        HelpOption(
            icon = painterResource(R.drawable.ic_layers),
            label = stringResource(R.string.fallback_level),
            detail = stringResource(R.string.fallback_level_sub),
            recommended = true,
            onClick = onConfirmLevel,
        )
        Spacer(Modifier.height(10.dp))
        HelpOption(
            icon = painterResource(R.drawable.ic_camera),
            label = stringResource(R.string.fallback_photo),
            detail = stringResource(R.string.fallback_photo_sub),
            recommended = false,
            onClick = onTakePhoto,
        )

        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onSkip) {
            Text(text = stringResource(R.string.fallback_skip), color = Smc.Dim, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HelpOption(icon: Painter, label: String, detail: String, recommended: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(shape, if (recommended) 8.dp else 4.dp)
            .clip(shape)
            .background(Smc.Surface)
            .border(BorderStroke(2.dp, if (recommended) Smc.Blue else Color.Transparent), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, size = 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Smc.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = detail, color = Smc.Dim, fontSize = 12.sp)
        }
        if (recommended) {
            Spacer(Modifier.width(8.dp))
            Pill(text = stringResource(R.string.fallback_quick), background = Smc.Blue, color = Color.White)
        }
    }
}

/** « Votre voiture est enregistrée ! » : le récapitulatif et les sources utilisées. */
@Composable
fun SavedScreen(spot: ParkingSpot, onDone: () -> Unit) {
    val context = LocalContext.current
    val sources = buildList {
        if (spot.hasCoordinates) add(stringResource(R.string.signal_gps))
        if (spot.auto) add(stringResource(R.string.source_bt))
        if (spot.pressureHpa != null) add(stringResource(R.string.signal_baro))
        if (spot.hasGarageInfo) add(stringResource(R.string.source_you))
        if (spot.photoPath != null) add(stringResource(R.string.source_photo))
        if (isEmpty()) add(stringResource(R.string.source_time))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 72.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PopBadge(
            icon = rememberVectorPainter(Icons.Filled.Check),
            size = 84.dp,
            brush = Smc.BlueGradient,
            tint = Color.White,
            glow = Smc.Blue,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.saved_title),
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SummaryRow(
                    icon = painterResource(R.drawable.ic_parking),
                    text = spot.placeName.ifBlank {
                        stringResource(R.string.saved_at, SpotText.clock(context, spot.savedAt))
                    },
                )
                if (spot.level.isNotBlank()) {
                    SummaryRow(
                        icon = painterResource(R.drawable.ic_layers),
                        text = stringResource(R.string.garage_level, spot.level),
                    )
                }
                if (spot.zone.isNotBlank()) {
                    SummaryRow(
                        icon = rememberVectorPainter(Icons.Filled.LocationOn),
                        text = stringResource(R.string.garage_zone, spot.zone),
                    )
                }
                SummaryRow(
                    icon = painterResource(R.drawable.ic_car),
                    text = if (spot.place.isNotBlank()) {
                        stringResource(R.string.garage_place, spot.place)
                    } else {
                        stringResource(R.string.saved_no_place)
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.saved_sources, sources.joinToString(" · ")),
            color = Smc.Dim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = stringResource(R.string.action_done), onClick = onDone)
    }
}

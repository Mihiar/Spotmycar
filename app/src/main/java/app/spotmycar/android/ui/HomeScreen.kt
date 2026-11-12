package app.spotmycar.android.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.data.LocationSource
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.PastSpot
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.ui.theme.Barlow
import app.spotmycar.android.ui.theme.BarlowCondensed
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.SpotText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** En dessous de cette marge d'erreur, on considère la position comme précise. */
private const val PRECISE_ACCURACY_METERS = 60f

/** Onglet « Ma voiture » : la place en cours, ou l'invitation à l'enregistrer. */
@Composable
fun HomeScreen(
    spot: ParkingSpot?,
    locating: Boolean,
    history: List<PastSpot>,
    resumeTick: Int,
    onParked: () -> Unit,
    onGuide: () -> Unit,
    onEdit: () -> Unit,
    onOpenPhoto: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    // L'heure affichée (« il y a 2 heures ») se met à jour toute seule.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                color = Smc.Muted,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                letterSpacing = tracking(0.2f),
                modifier = Modifier.weight(1f),
            )
            OutlinedIconButton(
                onClick = onOpenSettings,
                shape = SmcShape,
                border = BorderStroke(1.dp, Smc.BorderStrong),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Smc.Muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (spot != null) {
            SpotSummary(
                spot = spot,
                locating = locating,
                now = now,
                onGuide = onGuide,
                onEdit = onEdit,
                onOpenPhoto = onOpenPhoto,
                onShare = onShare,
            )
        } else {
            EmptyState(resumeTick = resumeTick, onParked = onParked, onOpenSettings = onOpenSettings)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(text = stringResource(R.string.home_previous), modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenHistory) {
                    Text(text = stringResource(R.string.home_see_all), color = Smc.Amber, fontSize = 13.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(3).forEach { past -> HistoryRow(past = past, compact = true) }
            }
        }
    }
}

@Composable
private fun SpotSummary(
    spot: ParkingSpot,
    locating: Boolean,
    now: Long,
    onGuide: () -> Unit,
    onEdit: () -> Unit,
    onOpenPhoto: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val parked = stringResource(R.string.find_parked, SpotText.ago(context, spot.savedAt, now))
    val subtitle = listOf(spot.placeName, parked).filter { it.isNotBlank() }.joinToString(" · ")

    Spacer(Modifier.height(20.dp))
    SectionLabel(text = stringResource(R.string.home_saved_title))
    Spacer(Modifier.height(4.dp))
    Text(text = subtitle, color = Smc.Dim, fontSize = 13.sp)

    // Niveau en très grand, zone et place à côté.
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 96.dp)
                .fillMaxHeight()
                .clip(SmcShape)
                .background(Smc.Amber)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.level_label).uppercase(),
                color = Smc.OnAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = tracking(0.16f),
            )
            Text(
                text = spot.level.ifBlank { "—" },
                color = Smc.OnAmber,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 54.sp,
                maxLines = 1,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ValueTile(label = stringResource(R.string.zone_label), value = spot.zone)
            ValueTile(label = stringResource(R.string.place_label), value = spot.place)
        }
    }

    // Photo de repère.
    Spacer(Modifier.height(14.dp))
    val photoPath = spot.photoPath
    if (photoPath != null) {
        SpotPhoto(
            path = photoPath,
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(SmcCardShape)
                .border(1.dp, Smc.Border, SmcCardShape)
                .clickable(onClick = onOpenPhoto),
        )
    } else {
        PhotoPlaceholder(
            caption = stringResource(R.string.home_photo_empty),
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clickable(onClick = onEdit),
        )
    }

    // Ce qu'on sait de la place.
    Spacer(Modifier.height(16.dp))
    InfoRow(label = stringResource(R.string.home_gps_label), value = gpsText(spot, locating))
    InfoRow(
        label = stringResource(R.string.home_saving_label),
        value = stringResource(
            if (spot.auto) R.string.home_saving_auto else R.string.home_saving_manual,
            SpotText.clock(context, spot.savedAt),
        ),
    )
    InfoRow(
        label = stringResource(R.string.home_note_label),
        value = spot.note.ifBlank { stringResource(R.string.home_note_none) },
        divider = false,
    )

    Spacer(Modifier.height(18.dp))
    AmberButton(
        text = stringResource(R.string.home_guide),
        onClick = onGuide,
        icon = painterResource(R.drawable.ic_navigation),
    )
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GhostButton(
            text = stringResource(R.string.home_edit),
            onClick = onEdit,
            modifier = Modifier.weight(1f),
        )
        OutlinedIconButton(
            onClick = onShare,
            shape = SmcShape,
            border = BorderStroke(1.dp, Smc.BorderStrong),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = stringResource(R.string.share_title),
                tint = Smc.Muted,
            )
        }
    }
}

@Composable
private fun ValueTile(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmcShape)
            .background(Smc.SurfaceHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = Smc.Dim,
            fontSize = 12.sp,
            letterSpacing = tracking(0.14f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.ifBlank { "—" },
            color = Smc.Text,
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun gpsText(spot: ParkingSpot, locating: Boolean): String {
    val accuracy = spot.accuracyMeters
    return when {
        !spot.hasCoordinates && locating -> stringResource(R.string.gps_short_searching)
        !spot.hasCoordinates -> stringResource(R.string.gps_short_none)
        spot.source == LocationSource.LAST_KNOWN -> stringResource(R.string.gps_short_entrance)
        accuracy != null && accuracy > PRECISE_ACCURACY_METERS ->
            stringResource(R.string.gps_short_rough, accuracy.roundToInt())
        else -> stringResource(R.string.gps_short_precise, (accuracy ?: 0f).roundToInt())
    }
}

/**
 * Aucune place : un seul geste possible (loi de Hick).
 * Au toucher, l'épingle tombe et une onde part, puis la place est notée.
 */
@Composable
private fun EmptyState(resumeTick: Int, onParked: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val autoSaveReady = remember(resumeTick) { BluetoothHelper.isAutoSaveReady(context) }
    var dropping by remember { mutableStateOf(false) }
    val currentOnParked by rememberUpdatedState(onParked)
    val pinDrop = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    // Qu'on accepte ou non la position, la place est notée : sans GPS, niveau et photo font le travail.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { dropping = true }

    LaunchedEffect(dropping) {
        if (dropping) {
            pinDrop.snapTo(-1f)
            pinDrop.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
            currentOnParked()
            dropping = false
        }
    }

    val onTap: () -> Unit = {
        if (!dropping) {
            if (LocationFetcher.hasForegroundPermission(context)) {
                dropping = true
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }

    Spacer(Modifier.height(22.dp))
    CondensedTitle(text = stringResource(R.string.home_empty_title), size = 40.sp)
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.home_empty_body),
        color = Smc.Muted,
        fontFamily = Barlow,
        fontSize = 15.5.sp,
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(SmcCardShape)
            .background(Smc.Surface)
            .border(1.dp, Smc.Border, SmcCardShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        PinRings(inward = false, modifier = Modifier.size(180.dp))
        Canvas(modifier = Modifier.size(180.dp)) {
            val wave = burst.value
            if (wave > 0f && wave < 1f) {
                drawCircle(
                    color = Smc.Amber.copy(alpha = (1f - wave) * 0.5f),
                    radius = size.minDimension / 2f * wave,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 6.dp.toPx()),
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = Smc.Amber,
            modifier = Modifier
                .size(54.dp)
                .offset(y = (pinDrop.value * 90f - 14f).dp),
        )
    }

    Spacer(Modifier.height(18.dp))
    AmberButton(text = stringResource(R.string.home_save_spot), onClick = onTap)

    Spacer(Modifier.height(8.dp))
    if (autoSaveReady) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(color = Smc.Success)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.park_auto_on), color = Smc.Muted, fontSize = 13.sp)
        }
    } else {
        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(R.drawable.ic_bluetooth),
                contentDescription = null,
                tint = Smc.Amber,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.park_auto_cta), color = Smc.Amber, fontSize = 14.sp)
        }
    }
}

/** Une ligne d'historique (aussi utilisée sur l'accueil, en version courte). */
@Composable
fun HistoryRow(past: PastSpot, compact: Boolean) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmcShape)
            .background(Smc.Surface)
            .border(1.dp, Smc.Border, SmcShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 46.dp else 56.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .hatched(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = past.level.ifBlank { "P" },
                color = Smc.Muted,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = SpotText.historyTitle(context, past), color = Smc.Text, fontSize = 15.sp, maxLines = 1)
            Text(
                text = SpotText.day(past.savedAt) + " · " + SpotText.duration(context, past.endedAt - past.savedAt),
                color = Smc.Dim,
                fontSize = 12.5.sp,
            )
            if (!compact && past.note.isNotBlank()) {
                Text(text = past.note, color = Smc.Faint, fontSize = 12.5.sp, maxLines = 2)
            }
        }
    }
}

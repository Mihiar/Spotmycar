package app.spotmycar.android.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.spotmycar.android.data.PastSpot
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.location.Reliability
import app.spotmycar.android.ui.theme.Jakarta
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.SpotText
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** En dessous de cette marge d'erreur, on considère la position comme précise. */
private const val PRECISE_ACCURACY_METERS = 60f

/** Les deux grands titres de l'accueil : ce sont des noms de marque, non traduits. */
private const val PARK_TITLE = "PARK\nMY CAR"
private const val FIND_TITLE = "FIND\nMY CAR"

/**
 * Accueil (prototype v2) : deux grandes cartes, « Park my car » et « Find my car ».
 * Sans voiture enregistrée, « Park » est mise en avant ; avec une voiture, c'est « Find ».
 */
@Composable
fun HomeScreen(
    spot: ParkingSpot?,
    locating: Boolean,
    history: List<PastSpot>,
    resumeTick: Int,
    onParked: () -> Unit,
    onFind: () -> Unit,
    onEdit: () -> Unit,
    onOpenPhoto: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val autoSaveReady = remember(resumeTick) { BluetoothHelper.isAutoSaveReady(context) }
    var confirmReplace by remember { mutableStateOf(false) }

    // L'heure affichée (« il y a 2 heures ») se met à jour toute seule.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    // Qu'on accepte ou non la position, la détection démarre : sans GPS, le mode de secours prend le relais.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onParked() }

    val startPark: () -> Unit = {
        if (LocationFetcher.hasForegroundPermission(context)) {
            onParked()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Wordmark(size = 19.sp)
            IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = Smc.Muted,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.home_question),
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        ActionCard(
            title = PARK_TITLE,
            subtitle = stringResource(R.string.home_park_sub),
            look = if (spot == null) ParkPrimary else ParkQuiet,
            badge = null,
            onClick = { if (spot != null) confirmReplace = true else startPark() },
        )

        Spacer(Modifier.height(6.dp))
        if (autoSaveReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    tint = Smc.Success,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.home_auto_on), color = Smc.Muted, fontSize = 12.sp)
            }
        } else {
            TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    tint = Smc.Blue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_auto_off),
                    color = Smc.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        ActionCard(
            title = FIND_TITLE,
            subtitle = stringResource(R.string.home_find_sub),
            look = if (spot != null) FindPrimary else FindQuiet,
            badge = if (spot != null) stringResource(R.string.home_saved_title) else null,
            onClick = {
                if (spot != null) {
                    onFind()
                } else {
                    Toast.makeText(context, R.string.home_no_car, Toast.LENGTH_SHORT).show()
                }
            },
        )

        if (spot != null) {
            Spacer(Modifier.height(18.dp))
            SpotSummary(
                spot = spot,
                locating = locating,
                now = now,
                onEdit = onEdit,
                onOpenPhoto = onOpenPhoto,
                onShare = onShare,
            )
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(text = stringResource(R.string.home_previous), modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenHistory) {
                    Text(
                        text = stringResource(R.string.home_see_all),
                        color = Smc.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(3).forEach { past -> HistoryRow(past = past, compact = true) }
            }
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            containerColor = Smc.Surface,
            title = { Text(text = stringResource(R.string.home_replace_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text(text = stringResource(R.string.home_replace_body), color = Smc.Muted, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    startPark()
                }) {
                    Text(text = stringResource(R.string.home_replace_confirm), color = Smc.Blue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = Smc.Muted)
                }
            },
        )
    }
}

/** L'habillage d'une grande carte d'action. */
private class CardLook(
    val brush: Brush?,
    val solid: Color,
    val glow: Color?,
    val title: Color,
    val spiral: Color,
    val icon: Color,
    val chevronBackground: Color,
    val chevron: Color,
    val subtitle: Color,
)

private val ParkPrimary = CardLook(
    brush = Brush.linearGradient(listOf(Smc.BlueLight, Smc.BlueDark)),
    solid = Smc.Blue,
    glow = Smc.Blue,
    title = Color.White,
    spiral = Color.White,
    icon = Color.White,
    chevronBackground = Color.White.copy(alpha = 0.22f),
    chevron = Color.White,
    subtitle = Color.White,
)
private val ParkQuiet = CardLook(
    brush = null,
    solid = Smc.BlueSoft,
    glow = null,
    title = Color(0xFF1D4ED8),
    spiral = Smc.Blue,
    icon = Color(0xFF1D4ED8),
    chevronBackground = Color.White,
    chevron = Color(0xFF1D4ED8),
    subtitle = Color(0xFF3B4F8F),
)
private val FindPrimary = CardLook(
    brush = Brush.linearGradient(listOf(Smc.VioletLight, Color(0xFF8B5CF6))),
    solid = Smc.Violet,
    glow = Smc.Violet,
    title = Color.White,
    spiral = Color.White,
    icon = Color.White,
    chevronBackground = Color.White.copy(alpha = 0.22f),
    chevron = Color.White,
    subtitle = Color.White,
)
private val FindQuiet = CardLook(
    brush = null,
    solid = Smc.Surface,
    glow = Color(0x33142850),
    title = Smc.TextSoft,
    spiral = Smc.Violet,
    icon = Color(0xFFA78BFA),
    chevronBackground = Smc.VioletSoft,
    chevron = Smc.Violet,
    subtitle = Smc.Muted,
)

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    look: CardLook,
    badge: String?,
    onClick: () -> Unit,
) {
    val glow = look.glow
    val brush = look.brush
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (glow != null) {
                    Modifier.shadow(12.dp, SmcBigCardShape, clip = false, ambientColor = glow, spotColor = glow)
                } else {
                    Modifier
                },
            )
            .clip(SmcBigCardShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(look.solid))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 18.dp, top = if (badge != null) 14.dp else 18.dp, bottom = 16.dp),
    ) {
        if (badge != null) {
            Pill(
                text = badge,
                background = Smc.SuccessSoft,
                color = Smc.OnSuccessSoft,
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spiral(color = look.spiral, modifier = Modifier.size(82.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Icon(
                    painter = painterResource(R.drawable.ic_car),
                    contentDescription = null,
                    tint = look.icon,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    color = look.title,
                    fontFamily = Jakarta,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(look.chevronBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = look.chevron,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = subtitle,
            color = look.subtitle,
            fontFamily = Jakarta,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Le résumé de la voiture enregistrée : repères, fiabilité, photo, modifier, partager. */
@Composable
private fun SpotSummary(
    spot: ParkingSpot,
    locating: Boolean,
    now: Long,
    onEdit: () -> Unit,
    onOpenPhoto: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val parked = stringResource(R.string.find_parked, SpotText.ago(context, spot.savedAt, now))
    val subtitle = listOf(spot.placeName, parked).filter { it.isNotBlank() }.joinToString(" · ")
    val garage = SpotText.garageLine(context, spot).ifBlank { stringResource(R.string.saved_no_place) }

    SmcCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = painterResource(R.drawable.ic_parking))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = garage,
                        color = Smc.Text,
                        fontFamily = Jakarta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(text = subtitle, color = Smc.Dim, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            ReliabilityBar(score = Reliability.score(spot))

            val photoPath = spot.photoPath
            if (photoPath != null) {
                Spacer(Modifier.height(12.dp))
                SpotPhoto(
                    path = photoPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(SmcShape)
                        .clickable(onClick = onOpenPhoto),
                )
            }

            Spacer(Modifier.height(10.dp))
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

            Spacer(Modifier.height(12.dp))
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

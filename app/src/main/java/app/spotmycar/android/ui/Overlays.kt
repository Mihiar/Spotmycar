package app.spotmycar.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.Actions
import app.spotmycar.android.util.SpotText

/** La photo de repère en plein écran, avec le niveau, la zone et la place en légende. */
@Composable
fun PhotoViewer(spot: ParkingSpot, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Smc.Background.copy(alpha = 0.97f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BarTitle(text = stringResource(R.string.photo_viewer_title), modifier = Modifier.weight(1f))
            GhostButton(text = stringResource(R.string.action_close), onClick = onClose)
        }
        Spacer(Modifier.height(18.dp))
        val path = spot.photoPath
        if (path != null) {
            SpotPhoto(
                path = path,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(SmcCardShape),
            )
        } else {
            PhotoPlaceholder(
                caption = stringResource(R.string.photo_none),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        val caption = SpotText.garageLine(context, spot)
        if (caption.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(text = caption, color = Smc.Muted, fontSize = 14.5.sp)
        }
    }
}

/** Le panneau « Partager la place » qui monte du bas de l'écran. */
@Composable
fun ShareSheet(spot: ParkingSpot, onClose: () -> Unit) {
    val context = LocalContext.current
    val message = remember(spot) { Actions.shareText(context, spot) }
    var copied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Smc.Text.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Smc.Surface)
                .border(1.dp, Smc.Border, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Smc.Off),
            )
            Spacer(Modifier.height(18.dp))
            CondensedTitle(text = stringResource(R.string.share_title), size = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.share_body),
                color = Smc.Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )

            // Le message tel qu'il sera envoyé, avec le bouton « Copier ».
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmcShape)
                    .background(Smc.SurfaceHigh)
                    .border(1.dp, Smc.Border, SmcShape)
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    color = Smc.TextSoft,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("SpotMyCar", message))
                        copied = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Smc.Blue, contentColor = Smc.OnBlue),
                ) {
                    Text(
                        text = stringResource(if (copied) R.string.share_copied else R.string.share_copy),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareTarget(label = stringResource(R.string.share_whatsapp), modifier = Modifier.weight(1f)) {
                    if (!Actions.shareToWhatsApp(context, message)) {
                        Toast.makeText(context, R.string.share_whatsapp_missing, Toast.LENGTH_SHORT).show()
                    }
                }
                ShareTarget(label = stringResource(R.string.share_sms), modifier = Modifier.weight(1f)) {
                    Actions.shareBySms(context, message)
                }
                ShareTarget(label = stringResource(R.string.share_more), modifier = Modifier.weight(1f)) {
                    Actions.share(context, spot)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_close), color = Smc.Dim, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ShareTarget(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(SmcShape)
            .background(Smc.SurfaceHigh)
            .border(1.dp, Smc.Border, SmcShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Smc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Les trois onglets du bas : Ma voiture, Historique, Alertes. */
@Composable
fun BottomTabs(current: Screen, onSelect: (Screen) -> Unit) {
    Column(modifier = Modifier.background(Smc.Background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Smc.Border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Screen.HOME to R.string.tab_car,
                Screen.HISTORY to R.string.tab_history,
                Screen.ALERTS to R.string.tab_alerts,
            ).forEach { (target, label) ->
                val selected = target == current
                val color = if (selected) Smc.Blue else Smc.Faint
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SmcShape)
                        .clickable { onSelect(target) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(color),
                    )
                    Text(
                        text = stringResource(label),
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

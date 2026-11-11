package app.spotmycar.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.spotmycar.android.R
import app.spotmycar.android.data.LocationSource
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** En-dessous de cette marge d'erreur, on considère la position comme précise. */
private const val PRECISE_ACCURACY_METERS = 60f

/** Titre d'écran avec le bouton Réglages à droite. */
@Composable
fun ScreenHeader(title: String, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Une ligne qui dit, en clair, ce que vaut la position enregistrée. */
@Composable
fun LocationStatusLine(spot: ParkingSpot, locating: Boolean, modifier: Modifier = Modifier) {
    val accuracy = spot.accuracyMeters
    val good = MaterialTheme.colorScheme.tertiary
    val warn = MaterialTheme.colorScheme.secondary
    val neutral = MaterialTheme.colorScheme.outline

    val text: String
    val dot: Color
    when {
        !spot.hasCoordinates && locating -> {
            text = stringResource(R.string.gps_searching)
            dot = neutral
        }

        !spot.hasCoordinates -> {
            text = stringResource(R.string.gps_none)
            dot = neutral
        }

        spot.source == LocationSource.LAST_KNOWN -> {
            text = stringResource(R.string.gps_last_known)
            dot = warn
        }

        accuracy != null && accuracy <= PRECISE_ACCURACY_METERS -> {
            text = stringResource(R.string.gps_precise, accuracy.roundToInt())
            dot = good
        }

        accuracy != null -> {
            text = stringResource(R.string.gps_rough, accuracy.roundToInt())
            dot = warn
        }

        else -> {
            text = stringResource(R.string.gps_precise, 0)
            dot = good
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Affiche la photo de la place (chargée hors de l'écran pour ne pas le figer). */
@Composable
fun SpotPhoto(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { Photos.load(path, 1600)?.asImageBitmap() }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = stringResource(R.string.photo_description),
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

package app.spotmycar.android.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.util.Photos
import java.io.File

/** Étages proposés en un appui. On peut toujours écrire autre chose. */
private val QUICK_FLOORS = listOf("-3", "-2", "-1", "0", "1", "2", "3")

/**
 * Juste après s'être garé : la place est déjà notée. Ici, on peut ajouter
 * un repère (photo, étage, note), ou ne rien faire et toucher « Terminé ».
 */
@Composable
fun DetailsScreen(
    spot: ParkingSpot,
    locating: Boolean,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var floor by rememberSaveable(spot.savedAt) { mutableStateOf(spot.floor) }
    var note by rememberSaveable(spot.savedAt) { mutableStateOf(spot.note) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            if (saved) {
                ParkingStore.update(context) { it.copy(photoPath = path) }
            } else {
                File(path).delete()
            }
        }
    }

    val onTakePhoto: () -> Unit = {
        val file = Photos.newPhotoFile(context)
        pendingPhotoPath = file.absolutePath
        try {
            takePicture.launch(Photos.uriFor(context, file))
        } catch (e: ActivityNotFoundException) {
            pendingPhotoPath = null
            file.delete()
            Toast.makeText(context, R.string.error_no_camera, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.details_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.details_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LocationStatusLine(spot = spot, locating = locating)

        // Photo : le repère le plus fiable en sous-sol.
        val photoPath = spot.photoPath
        if (photoPath != null) {
            SpotPhoto(
                path = photoPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        FilledTonalButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(if (photoPath == null) R.string.photo_take else R.string.photo_retake),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (photoPath == null) {
            Text(
                text = stringResource(R.string.photo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Étage / zone.
        OutlinedTextField(
            value = floor,
            onValueChange = { value ->
                floor = value.take(40)
                ParkingStore.update(context) { it.copy(floor = floor.trim()) }
            },
            label = { Text(stringResource(R.string.floor_label)) },
            placeholder = { Text(stringResource(R.string.floor_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_FLOORS.forEach { quickFloor ->
                FilterChip(
                    selected = floor.trim() == quickFloor,
                    onClick = {
                        floor = quickFloor
                        ParkingStore.update(context) { it.copy(floor = quickFloor) }
                    },
                    label = { Text(quickFloor, style = MaterialTheme.typography.titleMedium) },
                )
            }
        }

        // Note libre.
        OutlinedTextField(
            value = note,
            onValueChange = { value ->
                note = value.take(200)
                ParkingStore.update(context) { it.copy(note = note.trim()) }
            },
            label = { Text(stringResource(R.string.note_label)) },
            placeholder = { Text(stringResource(R.string.note_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = stringResource(R.string.action_done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

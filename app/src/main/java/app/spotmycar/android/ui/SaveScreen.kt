package app.spotmycar.android.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.ui.theme.Outfit
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.Photos
import app.spotmycar.android.util.SpotText
import java.io.File

/** Niveaux proposés en un appui (du plus haut au plus bas). */
private val LEVELS = listOf("3", "2", "1", "0", "-1", "-2", "-3", "-4", "-5")

/** Zones proposées en un appui. */
private val ZONES = listOf("A", "B", "C", "D", "E", "F", "G", "H")

/**
 * « Nouvelle place » : niveau, zone, numéro, photo du panneau et repère.
 * La place est déjà notée ; ici on ajoute les repères (tout est facultatif).
 * Les changements ne comptent qu'au moment d'« Enregistrer ».
 */
@Composable
fun SaveScreen(
    spot: ParkingSpot,
    isNew: Boolean,
    locating: Boolean,
    startWithPhoto: Boolean,
    onCancel: (discardNewSpot: Boolean) -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var level by rememberSaveable(spot.savedAt) { mutableStateOf(spot.level) }
    var zone by rememberSaveable(spot.savedAt) { mutableStateOf(spot.zone) }
    var place by rememberSaveable(spot.savedAt) { mutableStateOf(spot.place) }
    var note by rememberSaveable(spot.savedAt) { mutableStateOf(spot.note) }
    var draftPhoto by rememberSaveable(spot.savedAt) { mutableStateOf(spot.photoPath) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    /** Supprime une photo prise ici et pas (encore) gardée. */
    fun dropDraftPhoto() {
        val draft = draftPhoto
        if (draft != null && draft != spot.photoPath) File(draft).delete()
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            if (saved) {
                dropDraftPhoto()
                draftPhoto = path
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

    // Arrivée par « Prendre une photo » (mode de secours) : l'appareil photo s'ouvre tout de suite.
    var photoLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startWithPhoto) {
        if (startWithPhoto && !photoLaunched) {
            photoLaunched = true
            onTakePhoto()
        }
    }

    // Retour système : on garde la place, on oublie seulement les changements.
    BackHandler {
        dropDraftPhoto()
        onCancel(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        TopBar(
            title = stringResource(if (isNew) R.string.save_title_new else R.string.save_title_edit),
            start = {
                TextButton(onClick = {
                    dropDraftPhoto()
                    onCancel(isNew)
                }) {
                    Text(text = stringResource(R.string.action_cancel), color = Smc.Accent, fontSize = 15.sp, maxLines = 1)
                }
            },
        )

        // Ce qui a déjà été noté tout seul.
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmcShape)
                .background(Smc.SurfaceHigh)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(color = Smc.Accent, size = 7.dp)
            Spacer(Modifier.width(10.dp))
            Text(text = bannerText(spot, locating), color = Smc.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        SectionLabel(text = stringResource(R.string.level_label), modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        ChoiceRow(options = LEVELS, selected = level, initialIndex = 2) { level = if (level == it) "" else it }

        SectionLabel(text = stringResource(R.string.zone_label) + " " + stringResource(R.string.optional), modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        ChoiceRow(options = ZONES, selected = zone, initialIndex = 0) { zone = if (zone == it) "" else it }

        SectionLabel(text = stringResource(R.string.save_place_number), modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        OutlinedTextField(
            value = place,
            onValueChange = { value -> place = value.take(8) },
            placeholder = { Text("148", color = Smc.Faint, fontFamily = Outfit, fontSize = 20.sp) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Smc.Text),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
            shape = SmcShape,
            colors = smcFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        // Photo du panneau le plus proche : le repère le plus sûr en sous-sol.
        Spacer(Modifier.height(22.dp))
        val photo = draftPhoto
        if (photo != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(SmcCardShape)
                    .clickable(onClick = onTakePhoto),
                contentAlignment = Alignment.BottomStart,
            ) {
                SpotPhoto(path = photo, modifier = Modifier.fillMaxSize())
                Text(
                    text = stringResource(R.string.save_photo_added),
                    color = Smc.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(SmcPillShape)
                        .background(Smc.Surface.copy(alpha = 0.9f))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
        } else {
            PhotoPlaceholder(
                caption = stringResource(R.string.save_photo_add),
                dashed = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable(onClick = onTakePhoto),
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { value -> note = value.take(200) },
            placeholder = { Text(stringResource(R.string.save_note_placeholder), color = Smc.Faint) },
            textStyle = TextStyle(fontFamily = Outfit, fontSize = 15.5.sp, color = Smc.Text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 3,
            shape = SmcShape,
            colors = smcFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.save_button),
            onClick = {
                val keptPhoto = draftPhoto
                ParkingStore.update(context) {
                    it.copy(
                        level = level,
                        zone = zone,
                        place = place.trim(),
                        note = note.trim(),
                        photoPath = keptPhoto,
                    )
                }
                onSaved()
            },
        )
    }
}

@Composable
private fun bannerText(spot: ParkingSpot, locating: Boolean): String {
    val context = LocalContext.current
    val clock = SpotText.clock(context, spot.savedAt)
    return when {
        spot.auto -> stringResource(R.string.save_banner_auto, clock)
        locating && !spot.hasCoordinates -> stringResource(R.string.save_banner_locating, clock)
        spot.hasCoordinates -> stringResource(R.string.save_banner_gps, clock)
        else -> stringResource(R.string.save_banner_no_gps, clock)
    }
}

/** Une rangée de gros boutons de choix, qui défile si elle déborde. */
@Composable
private fun ChoiceRow(options: List<String>, selected: String, initialIndex: Int, onPick: (String) -> Unit) {
    val startIndex = options.indexOf(selected).takeIf { it >= 0 }?.let { (it - 1).coerceAtLeast(0) } ?: initialIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            ChoiceButton(
                label = option,
                selected = option == selected,
                onClick = { onPick(option) },
                modifier = Modifier.width(60.dp),
            )
        }
    }
}

@Composable
fun smcFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Smc.Text,
    unfocusedTextColor = Smc.Text,
    focusedContainerColor = Smc.Surface,
    unfocusedContainerColor = Smc.Surface,
    focusedBorderColor = Smc.Accent,
    unfocusedBorderColor = Smc.Border,
    cursorColor = Smc.Accent,
)

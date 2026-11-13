package app.spotmycar.android.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.BuildConfig
import app.spotmycar.android.R
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.bluetooth.PairedDevice
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.notify.Notifier
import app.spotmycar.android.util.Actions

/**
 * Réglages : surtout l'enregistrement automatique par Bluetooth, présenté
 * comme une liste d'étapes à cocher, une seule fois.
 */
@Composable
fun SettingsScreen(resumeTick: Int, onBack: () -> Unit, onReplayIntro: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    // « refresh » augmente après chaque demande d'autorisation, « resumeTick » à chaque
    // retour dans l'app : les deux font revérifier l'état des autorisations.
    var refresh by remember { mutableIntStateOf(0) }
    var autoEnabled by remember { mutableStateOf(settings.autoSaveEnabled) }
    var carAddress by remember { mutableStateOf(settings.carAddress) }
    var showDisclosure by remember { mutableStateOf(false) }

    val bluetoothGranted = remember(resumeTick, refresh) { BluetoothHelper.hasConnectPermission(context) }
    val foregroundGranted = remember(resumeTick, refresh) { LocationFetcher.hasForegroundPermission(context) }
    val backgroundGranted = remember(resumeTick, refresh) { LocationFetcher.hasBackgroundPermission(context) }
    val notificationsGranted = remember(resumeTick, refresh) { Notifier.canNotify(context) }
    val devices = remember(resumeTick, refresh) { BluetoothHelper.pairedDevices(context) }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh += 1 }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh += 1 }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh += 1 }
    // Android impose deux temps : d'abord la position « pendant l'utilisation », puis « toujours ».
    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        refresh += 1
        if (result.values.any { it } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Smc.Muted,
                )
            }
            BarTitle(text = stringResource(R.string.settings_title))
        }

        // ── Enregistrement automatique ────────────────────────────────────────
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_bluetooth),
                    contentDescription = null,
                    tint = Smc.Accent,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.auto_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.auto_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Smc.Muted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.auto_switch),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                SmcSwitch(
                    checked = autoEnabled,
                    onCheckedChange = { enabled ->
                        autoEnabled = enabled
                        settings.autoSaveEnabled = enabled
                    },
                )
            }

            if (autoEnabled) {
                val ready = bluetoothGranted && carAddress != null && backgroundGranted
                Text(
                    text = stringResource(if (ready) R.string.auto_active else R.string.auto_incomplete),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ready) Smc.Success else Smc.Accent,
                )
                HorizontalDivider(color = Smc.Border)

                // Étape 1 : accès au Bluetooth.
                StepRow(
                    done = bluetoothGranted,
                    title = stringResource(R.string.step_bluetooth_title),
                    description = stringResource(R.string.step_bluetooth_desc),
                    actionLabel = stringResource(R.string.action_allow),
                    onAction = { bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                )

                // Étape 2 : quelle voiture ?
                StepRow(
                    done = carAddress != null,
                    title = stringResource(R.string.step_car_title),
                    description = stringResource(R.string.step_car_desc),
                )
                if (bluetoothGranted) {
                    if (devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.step_car_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Smc.Muted,
                        )
                        OutlinedButton(onClick = { Actions.openBluetoothSettings(context) }) {
                            Text(text = stringResource(R.string.step_car_open_bluetooth))
                        }
                    } else {
                        DeviceList(
                            devices = devices,
                            selectedAddress = carAddress,
                            onSelect = { device ->
                                carAddress = device.address
                                settings.carAddress = device.address
                                settings.carName = device.name
                            },
                        )
                    }
                }

                // Étape 3 : position « toujours », précédée d'une explication claire.
                StepRow(
                    done = backgroundGranted,
                    title = stringResource(R.string.step_location_title),
                    description = stringResource(R.string.step_location_desc),
                    actionLabel = stringResource(R.string.action_allow),
                    onAction = { showDisclosure = true },
                )

                // Étape 4 (facultative) : notification de confirmation.
                StepRow(
                    done = notificationsGranted,
                    title = stringResource(R.string.step_notifications_title),
                    description = stringResource(R.string.step_notifications_desc),
                    actionLabel = stringResource(R.string.action_allow),
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            Actions.openAppSettings(context)
                        }
                    },
                )

                TextButton(onClick = { Actions.openAppSettings(context) }) {
                    Text(text = stringResource(R.string.action_app_settings))
                }
            }
        }

        // ── Fiabilité sur les téléphones qui arrêtent les apps en arrière-plan ─
        SettingsCard {
            Text(
                text = stringResource(R.string.reliability_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.reliability_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Smc.Muted,
            )
            OutlinedButton(
                onClick = { Actions.openAutostartSettings(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.reliability_autostart))
            }
            OutlinedButton(
                onClick = { Actions.openBatterySettings(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.reliability_battery))
            }
        }

        // ── À propos ──────────────────────────────────────────────────────────
        SettingsCard {
            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.about_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = Smc.Muted,
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = Smc.Muted,
            )
            TextButton(onClick = { Actions.openUrl(context, Actions.PRIVACY_POLICY_URL) }) {
                Text(text = stringResource(R.string.about_privacy), color = Smc.Accent)
            }
            TextButton(onClick = onReplayIntro) {
                Text(text = stringResource(R.string.about_replay_intro), color = Smc.Accent)
            }
        }
    }

    // Explication obligatoire AVANT de demander la position en arrière-plan.
    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(text = stringResource(R.string.disclosure_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = stringResource(R.string.disclosure_body))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclosure = false
                        if (!foregroundGranted) {
                            foregroundLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.disclosure_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text(text = stringResource(R.string.disclosure_decline))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SmcCardShape,
        color = Smc.Surface,
        contentColor = Smc.Text,
        border = BorderStroke(1.dp, Smc.Border),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Une étape de la mise en route : pastille cochée ou bouton « Autoriser ». */
@Composable
private fun StepRow(
    done: Boolean,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (done) Smc.Success else Smc.SurfaceHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Smc.OnAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Smc.Muted,
            )
        }
        if (!done && actionLabel != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

/** Liste des appareils Bluetooth associés : on choisit celui de la voiture. */
@Composable
private fun DeviceList(
    devices: List<PairedDevice>,
    selectedAddress: String?,
    onSelect: (PairedDevice) -> Unit,
) {
    Column {
        devices.forEach { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(device) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = device.address == selectedAddress,
                    onClick = { onSelect(device) },
                )
                Text(text = device.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

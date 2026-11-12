package app.spotmycar.android.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.alerts.AlertScheduler
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.ParkingStore
import app.spotmycar.android.notify.Notifier
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.Actions
import app.spotmycar.android.util.SpotText

private val DURATION_CHOICES = listOf(1, 2, 3, 4, 6, 8, 12)

/** Onglet « Alertes » : quatre rappels simples, tous facultatifs. */
@Composable
fun AlertsScreen(spot: ParkingSpot?, resumeTick: Int, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var refresh by remember { mutableIntStateOf(0) }
    var closingOn by remember { mutableStateOf(settings.closingAlert) }
    var closingMinutes by remember { mutableIntStateOf(settings.closingMinutes) }
    var durationOn by remember { mutableStateOf(settings.durationAlert) }
    var maxHours by remember { mutableIntStateOf(settings.maxHours) }
    var freeOn by remember { mutableStateOf(settings.freeAlert) }
    var freeMinutes by remember { mutableIntStateOf(settings.freeMinutes) }
    var autoOn by remember(resumeTick) { mutableStateOf(settings.autoSaveEnabled) }
    var pickDuration by remember { mutableStateOf(false) }

    val autoReady = remember(resumeTick, refresh, autoOn) { BluetoothHelper.isAutoSaveReady(context) }
    val canNotify = remember(resumeTick, refresh) { Notifier.canNotify(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh += 1 }

    /** Une alerte ne sert à rien sans notifications : on les demande au bon moment. */
    fun ensureNotifications() {
        if (Notifier.canNotify(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Actions.openAppSettings(context)
        }
    }

    fun resync() = AlertScheduler.sync(context, ParkingStore.current(context))

    fun pickTime(initialMinutes: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initialMinutes / 60,
            initialMinutes % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        CondensedTitle(text = stringResource(R.string.alerts_title), size = 40.sp)

        // Aperçu de ce qui s'affichera sur l'écran verrouillé.
        Spacer(Modifier.height(20.dp))
        SmcCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SectionLabel(text = stringResource(R.string.alerts_preview))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Smc.Amber),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = previewTitle(closingOn, closingMinutes, durationOn, maxHours, freeOn, freeMinutes),
                            color = Smc.Text,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (spot != null) SpotText.reminder(context, spot) else stringResource(R.string.alerts_preview_sample),
                            color = Smc.Muted,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }

        if (!canNotify && (closingOn || durationOn || freeOn || autoOn)) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { ensureNotifications() }) {
                Text(text = stringResource(R.string.alerts_notifications_off), color = Smc.Amber, fontSize = 13.5.sp)
            }
        }

        SectionLabel(
            text = stringResource(R.string.alerts_section),
            modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
        )
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column {
                AlertRow(
                    label = stringResource(R.string.alert_closing_label),
                    hint = stringResource(R.string.alert_closing_hint, SpotText.clockOfDay(context, closingMinutes)),
                    checked = closingOn,
                    onCheckedChange = { on ->
                        closingOn = on
                        settings.closingAlert = on
                        if (on) ensureNotifications()
                        resync()
                    },
                    onEdit = {
                        pickTime(closingMinutes) { picked ->
                            closingMinutes = picked
                            settings.closingMinutes = picked
                            resync()
                        }
                    },
                )
                HorizontalDivider(color = Smc.SurfaceHigh)
                AlertRow(
                    label = stringResource(R.string.alert_duration_label),
                    hint = stringResource(R.string.alert_duration_hint, maxHours),
                    checked = durationOn,
                    onCheckedChange = { on ->
                        durationOn = on
                        settings.durationAlert = on
                        if (on) ensureNotifications()
                        resync()
                    },
                    onEdit = { pickDuration = true },
                )
                HorizontalDivider(color = Smc.SurfaceHigh)
                AlertRow(
                    label = stringResource(R.string.alert_bluetooth_label),
                    hint = stringResource(
                        when {
                            !autoOn -> R.string.alert_bluetooth_hint
                            autoReady -> R.string.alert_bluetooth_ready
                            else -> R.string.alert_bluetooth_setup
                        },
                    ),
                    checked = autoOn,
                    onCheckedChange = { on ->
                        autoOn = on
                        settings.autoSaveEnabled = on
                        // Première fois : il faut choisir la voiture et autoriser la position.
                        if (on && !BluetoothHelper.isAutoSaveReady(context)) onOpenSettings()
                    },
                    onEdit = onOpenSettings,
                )
                HorizontalDivider(color = Smc.SurfaceHigh)
                AlertRow(
                    label = stringResource(R.string.alert_free_label),
                    hint = stringResource(R.string.alert_free_hint, SpotText.clockOfDay(context, freeMinutes)),
                    checked = freeOn,
                    onCheckedChange = { on ->
                        freeOn = on
                        settings.freeAlert = on
                        if (on) ensureNotifications()
                        resync()
                    },
                    onEdit = {
                        pickTime(freeMinutes) { picked ->
                            freeMinutes = picked
                            settings.freeMinutes = picked
                            resync()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.alerts_privacy),
            color = Smc.Dim,
            fontSize = 12.5.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(20.dp))
        GhostButton(
            text = stringResource(R.string.settings_title),
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (pickDuration) {
        AlertDialog(
            onDismissRequest = { pickDuration = false },
            title = { Text(stringResource(R.string.alert_duration_pick)) },
            text = {
                Column {
                    DURATION_CHOICES.forEach { hours ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SmcShape)
                                .clickable {
                                    maxHours = hours
                                    settings.maxHours = hours
                                    resync()
                                    pickDuration = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = hours == maxHours,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Smc.Amber),
                            )
                            Text(
                                text = stringResource(R.string.duration_hours, hours),
                                color = Smc.Text,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickDuration = false }) {
                    Text(stringResource(R.string.action_close), color = Smc.Muted)
                }
            },
            containerColor = Smc.Surface,
            titleContentColor = Smc.Text,
        )
    }
}

@Composable
private fun previewTitle(
    closingOn: Boolean,
    closingMinutes: Int,
    durationOn: Boolean,
    maxHours: Int,
    freeOn: Boolean,
    freeMinutes: Int,
): String {
    val context = LocalContext.current
    return when {
        closingOn -> stringResource(R.string.alert_closing_title, SpotText.clockOfDay(context, closingMinutes))
        durationOn -> stringResource(R.string.alert_duration_title, SpotText.duration(context, maxHours * 3_600_000L))
        freeOn -> stringResource(R.string.alert_free_title, SpotText.clockOfDay(context, freeMinutes))
        else -> stringResource(R.string.notification_saved_title)
    }
}

/** Une ligne d'alerte : titre, explication, interrupteur et, si active, « Modifier ». */
@Composable
private fun AlertRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, color = Smc.Text, fontSize = 15.5.sp)
            Text(text = hint, color = Smc.Dim, fontSize = 12.5.sp, lineHeight = 18.sp)
            if (checked) {
                Text(
                    text = stringResource(R.string.action_edit),
                    color = Smc.Amber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(SmcShape)
                        .clickable(onClick = onEdit)
                        .padding(vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        SmcSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

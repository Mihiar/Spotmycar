package app.spotmycar.android.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.bluetooth.BluetoothHelper
import app.spotmycar.android.bluetooth.PairedDevice
import app.spotmycar.android.data.AppSettings
import app.spotmycar.android.location.LocationFetcher
import app.spotmycar.android.notify.Notifier
import app.spotmycar.android.ui.theme.Jakarta
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.Actions

/** Les étapes de la mise en route, dans l'ordre où elles sont proposées. */
enum class SetupStep { LOCATION, CAR, ALWAYS, NOTIFICATIONS }

object Setup {
    /** Vrai tant que l'étape a encore quelque chose à demander sur ce téléphone. */
    fun isPending(context: Context, settings: AppSettings, step: SetupStep): Boolean = when (step) {
        SetupStep.LOCATION -> !LocationFetcher.hasForegroundPermission(context)
        SetupStep.CAR -> settings.carAddress == null && BluetoothHelper.isSupported(context)
        // La position « toujours » ne sert qu'à l'enregistrement automatique : pas de voiture, pas de demande.
        SetupStep.ALWAYS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            settings.autoSaveEnabled && settings.carAddress != null &&
            !LocationFetcher.hasBackgroundPermission(context)
        SetupStep.NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notifier.canNotify(context)
    }

    fun firstPending(context: Context, settings: AppSettings, after: SetupStep? = null): SetupStep? =
        SetupStep.entries.firstOrNull { step ->
            (after == null || step.ordinal > after.ordinal) && isPending(context, settings, step)
        }
}

/**
 * Mise en route, juste après l'introduction : une autorisation par écran, avec sa raison
 * et un seul bouton. Chaque étape peut être remise à plus tard ; celles qui sont déjà
 * réglées ne s'affichent pas.
 */
@Composable
fun SetupScreen(resumeTick: Int, onFinished: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    // « refresh » augmente après chaque réponse à une demande, « resumeTick » à chaque
    // retour dans l'app (par exemple depuis les paramètres).
    var refresh by remember { mutableIntStateOf(0) }
    var step by rememberSaveable {
        mutableStateOf(Setup.firstPending(context, settings) ?: SetupStep.LOCATION)
    }
    var carAddress by remember { mutableStateOf(settings.carAddress) }

    val foregroundGranted = remember(resumeTick, refresh) { LocationFetcher.hasForegroundPermission(context) }
    val bluetoothGranted = remember(resumeTick, refresh) { BluetoothHelper.hasConnectPermission(context) }
    val devices = remember(resumeTick, refresh) { BluetoothHelper.pairedDevices(context) }

    val advance: () -> Unit = {
        val next = Setup.firstPending(context, settings, after = step)
        if (next == null) onFinished() else step = next
    }

    // Une autorisation accordée, ici ou dans les paramètres, fait passer à la suite.
    // (Le choix de la voiture attend, lui, un appui sur « Continuer ».)
    LaunchedEffect(resumeTick, refresh, step) {
        if (step != SetupStep.CAR && !Setup.isPending(context, settings, step)) advance()
    }

    val locationGate = rememberPermissionGate(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) { refresh += 1 }
    val bluetoothGate = rememberPermissionGate(Manifest.permission.BLUETOOTH_CONNECT) { refresh += 1 }
    val notificationGate = rememberPermissionGate(Manifest.permission.POST_NOTIFICATIONS) { refresh += 1 }
    val backgroundGate = rememberPermissionGate(Manifest.permission.ACCESS_BACKGROUND_LOCATION) { refresh += 1 }
    // Android impose deux temps : d'abord la position « pendant l'utilisation », puis « toujours ».
    val locationThenBackgroundGate = rememberPermissionGate(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) { granted ->
        refresh += 1
        if (granted) backgroundGate.request()
    }

    // Ce que montre et fait le bouton principal de l'étape en cours.
    val blocked: Boolean
    val actionLabel: String?
    val onAction: () -> Unit
    when (step) {
        SetupStep.LOCATION -> {
            blocked = locationGate.blocked
            actionLabel = stringResource(R.string.setup_location_action)
            onAction = locationGate.request
        }
        SetupStep.CAR -> when {
            !bluetoothGranted -> {
                blocked = bluetoothGate.blocked
                actionLabel = stringResource(R.string.setup_car_action)
                onAction = bluetoothGate.request
            }
            devices.isEmpty() -> {
                blocked = false
                actionLabel = stringResource(R.string.step_car_open_bluetooth)
                onAction = { Actions.openBluetoothSettings(context) }
            }
            else -> {
                blocked = false
                // Pas de bouton tant qu'aucune voiture n'est choisie.
                actionLabel = if (carAddress != null) stringResource(R.string.disclosure_accept) else null
                onAction = advance
            }
        }
        SetupStep.ALWAYS -> {
            val gate = if (foregroundGranted) backgroundGate else locationThenBackgroundGate
            blocked = gate.blocked
            actionLabel = stringResource(R.string.setup_always_action)
            onAction = gate.request
        }
        SetupStep.NOTIFICATIONS -> {
            blocked = notificationGate.blocked
            actionLabel = stringResource(R.string.setup_notifications_action)
            onAction = notificationGate.request
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(size = 19.sp, modifier = Modifier.weight(1f))
            SectionLabel(text = stringResource(R.string.setup_label))
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "setup",
        ) { shown ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(SmcBigCardShape)
                        .background(Smc.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Smc.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        StepIcon(shown)
                    }
                }
                Spacer(Modifier.height(24.dp))
                CondensedTitle(
                    text = stringResource(
                        when (shown) {
                            SetupStep.LOCATION -> R.string.setup_location_title
                            SetupStep.CAR -> R.string.setup_car_title
                            SetupStep.ALWAYS -> R.string.setup_always_title
                            SetupStep.NOTIFICATIONS -> R.string.setup_notifications_title
                        },
                    ),
                    size = 40.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        when (shown) {
                            SetupStep.LOCATION -> R.string.setup_location_body
                            SetupStep.CAR -> R.string.setup_car_body
                            // L'explication complète, obligatoire avant la position en arrière-plan.
                            SetupStep.ALWAYS -> R.string.disclosure_body
                            SetupStep.NOTIFICATIONS -> R.string.setup_notifications_body
                        },
                    ),
                    color = Smc.Muted,
                    fontFamily = Jakarta,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                )

                if (shown == SetupStep.CAR && bluetoothGranted) {
                    Spacer(Modifier.height(14.dp))
                    if (devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.step_car_none),
                            color = Smc.Dim,
                            fontFamily = Jakarta,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    } else {
                        CarChoices(
                            devices = devices,
                            selectedAddress = carAddress,
                            onSelect = { device ->
                                carAddress = device.address
                                settings.carAddress = device.address
                                settings.carName = device.name
                                // Choisir sa voiture, c'est demander l'enregistrement automatique.
                                settings.autoSaveEnabled = true
                            },
                        )
                    }
                }

                if (shown == step && blocked) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.permission_blocked_hint),
                        color = Smc.OnWarnSoft,
                        fontFamily = Jakarta,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SmcShape)
                            .background(Smc.WarnSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (actionLabel != null) {
            PrimaryButton(
                text = if (blocked) stringResource(R.string.action_open_settings) else actionLabel,
                onClick = onAction,
            )
        }
        TextButton(
            onClick = advance,
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(R.string.setup_later),
                color = Smc.Muted,
                fontFamily = Jakarta,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun StepIcon(step: SetupStep) {
    val modifier = Modifier.size(36.dp)
    when (step) {
        SetupStep.LOCATION ->
            Icon(painter = painterResource(R.drawable.ic_navigation), contentDescription = null, tint = Smc.Blue, modifier = modifier)
        SetupStep.CAR ->
            Icon(painter = painterResource(R.drawable.ic_bluetooth), contentDescription = null, tint = Smc.Blue, modifier = modifier)
        SetupStep.ALWAYS ->
            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = Smc.Blue, modifier = modifier)
        SetupStep.NOTIFICATIONS ->
            Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = Smc.Blue, modifier = modifier)
    }
}

/** Les appareils Bluetooth déjà associés au téléphone : on touche celui de la voiture. */
@Composable
private fun CarChoices(
    devices: List<PairedDevice>,
    selectedAddress: String?,
    onSelect: (PairedDevice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.forEach { device ->
            val selected = device.address == selectedAddress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmcShape)
                    .background(if (selected) Smc.SurfaceHigh else Smc.Surface)
                    .clickable(role = Role.RadioButton) { onSelect(device) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = { onSelect(device) },
                    colors = RadioButtonDefaults.colors(selectedColor = Smc.Blue, unselectedColor = Smc.Dim),
                )
                Text(
                    text = device.name,
                    color = Smc.Text,
                    fontFamily = Jakarta,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

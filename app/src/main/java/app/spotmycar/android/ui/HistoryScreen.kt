package app.spotmycar.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.HistoryStore
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.data.PastSpot
import app.spotmycar.android.ui.theme.BarlowCondensed
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.SpotText
import java.util.Calendar

/** Onglet « Historique » : les places précédentes et deux chiffres simples. */
@Composable
fun HistoryScreen(spot: ParkingSpot?, history: List<PastSpot>) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thisMonth = history.count { it.savedAt >= monthStart } + (if (spot != null && spot.savedAt >= monthStart) 1 else 0)
    val averageMillis = history
        .map { it.endedAt - it.savedAt }
        .filter { it > 0 }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        CondensedTitle(text = stringResource(R.string.history_title), size = 40.sp)

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = stringResource(R.string.history_month),
                value = thisMonth.toString(),
                caption = stringResource(R.string.history_month_caption),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.history_average),
                value = averageMillis?.let { SpotText.duration(context, it) } ?: "—",
                caption = stringResource(R.string.history_average_caption),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))
        if (history.isEmpty()) {
            SmcCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.history_empty), color = Smc.Muted, fontSize = 15.sp, lineHeight = 22.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.forEach { past -> HistoryRow(past = past, compact = false) }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = stringResource(R.string.history_clear), color = Smc.Faint, fontSize = 13.sp)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    HistoryStore.clear(context)
                    confirmClear = false
                }) { Text(stringResource(R.string.history_clear_confirm), color = Smc.Amber) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel), color = Smc.Muted)
                }
            },
            containerColor = Smc.Surface,
            titleContentColor = Smc.Text,
            textContentColor = Smc.Muted,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    SmcCard(modifier = modifier, padding = 14.dp) {
        Column {
            SectionLabel(text = label)
            Text(
                text = value,
                color = Smc.Text,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                maxLines = 1,
            )
            Text(text = caption, color = Smc.Dim, fontSize = 12.5.sp)
        }
    }
}

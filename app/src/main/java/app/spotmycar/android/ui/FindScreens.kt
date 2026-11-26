package app.spotmycar.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.ParkingSpot
import app.spotmycar.android.ui.theme.Jakarta
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.SpotText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Le parcours « Find my car » du prototype v2 :
 * Guidage → Trouvée ! (sans écran de recherche intermédiaire)
 */

private val CONFETTI_COLORS = listOf(
    Color(0xFF1F6BFF), Color(0xFF8B6CF6), Color(0xFF60A5FA), Color(0xFFC4B5FD), Color(0xFF1D4ED8),
)

/** « Trouvée ! » : la fin heureuse (règle du pic et de la fin), avec quelques confettis. */
@Composable
fun ArrivedScreen(spot: ParkingSpot, onDone: () -> Unit, onParkAnother: () -> Unit) {
    val context = LocalContext.current
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        burst.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    val pop = rememberPop()
    val garage = SpotText.garageLine(context, spot)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = burst.value
                val piece = Size(10.dp.toPx(), 5.dp.toPx())
                for (i in 0 until 16) {
                    val angle = i / 16f * 2f * PI.toFloat() + 0.2f
                    val distance = (60f + (52f + (i % 3) * 12f) * progress).dp.toPx()
                    val x = center.x + cos(angle) * distance
                    val y = center.y + sin(angle) * distance * 0.92f
                    rotate(degrees = angle * 57f + progress * 140f, pivot = Offset(x, y)) {
                        drawRoundRect(
                            color = CONFETTI_COLORS[i % CONFETTI_COLORS.size].copy(alpha = (progress * 4f).coerceIn(0f, 1f)),
                            topLeft = Offset(x - piece.width / 2f, y - piece.height / 2f),
                            size = piece,
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .scale(pop)
                    .shadow(16.dp, CircleShape, clip = false, ambientColor = Smc.Blue, spotColor = Smc.Blue)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Smc.BlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_car),
                    contentDescription = null,
                    tint = Smc.Blue,
                    modifier = Modifier.size(84.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.arrived_title),
            color = Smc.Text,
            fontFamily = Jakarta,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(R.string.arrived_body), color = Smc.Muted, fontSize = 16.sp)

        Spacer(Modifier.height(20.dp))
        SmcCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SummaryRow(
                    icon = painterResource(R.drawable.ic_parking),
                    text = spot.placeName.ifBlank {
                        stringResource(R.string.saved_at, SpotText.clock(context, spot.savedAt))
                    },
                    tile = false,
                )
                if (garage.isNotBlank()) {
                    SummaryRow(
                        icon = rememberVectorPainter(Icons.Filled.LocationOn),
                        text = garage,
                        tile = false,
                    )
                }
                if (spot.note.isNotBlank()) {
                    SummaryRow(
                        icon = painterResource(R.drawable.ic_layers),
                        text = spot.note,
                        tile = false,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = stringResource(R.string.action_done), onClick = onDone)
        TextButton(onClick = onParkAnother, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = stringResource(R.string.arrived_new),
                color = Smc.Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

package app.spotmycar.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.data.PastSpot
import app.spotmycar.android.location.Reliability
import app.spotmycar.android.ui.theme.Outfit
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.util.SpotText

/** Une ligne d'historique (aussi utilisée sur l'accueil, en version courte). */
@Composable
fun HistoryRow(past: PastSpot, compact: Boolean) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(SmcShape, 3.dp)
            .clip(SmcShape)
            .background(Smc.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 42.dp else 50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Smc.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = past.level.ifBlank { "P" },
                color = Smc.Accent,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = SpotText.historyTitle(context, past),
                color = Smc.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = SpotText.day(past.savedAt) + " · " + SpotText.duration(context, past.endedAt - past.savedAt),
                color = Smc.Dim,
                fontSize = 12.5.sp,
            )
            if (!compact && past.note.isNotBlank()) {
                Text(text = past.note, color = Smc.Faint, fontSize = 12.5.sp, maxLines = 2)
            }
        }
    }
}

/** Couleur associée à un niveau de fiabilité : vert, orange, gris. */
fun reliabilityColor(score: Int): Color = when (Reliability.grade(score)) {
    Reliability.Grade.HIGH -> Smc.Success
    Reliability.Grade.MEDIUM -> Smc.WarnBright
    Reliability.Grade.LOW -> Smc.Warn
}

/** « Fiabilité de la position … 92 % » et sa barre, qui se remplit en douceur. */
@Composable
fun ReliabilityBar(score: Int, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "reliability",
    )
    val color = reliabilityColor(score)
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.confidence_label),
                color = Smc.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.percent, (shown * 100f).toInt()),
                color = color,
                fontFamily = Outfit,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(SmcPillShape)
                .background(Smc.Border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown.coerceIn(0f, 1f))
                    .clip(SmcPillShape)
                    .background(color),
            )
        }
    }
}

/** Où en est un signal pendant la détection. */
enum class SignalState { PENDING, OK, WARN, NA }

/** Une ligne de signal : icône, nom, détail, et une pastille d'état à droite. */
@Composable
fun SignalRow(icon: Painter, label: String, detail: String, state: SignalState) {
    val warn = state == SignalState.WARN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .alpha(if (state == SignalState.PENDING) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(
            icon = icon,
            background = if (warn) Smc.WarnSoft else Smc.SurfaceHigh,
            tint = when (state) {
                SignalState.WARN -> Smc.Warn
                SignalState.NA -> Smc.Faint
                else -> Smc.Accent
            },
            size = 32.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Smc.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = detail, color = Smc.Dim, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when (state) {
                        SignalState.PENDING -> Smc.Border
                        SignalState.OK -> Smc.Accent
                        SignalState.WARN -> Smc.WarnBright
                        SignalState.NA -> Smc.Off
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                SignalState.OK -> Icon(Icons.Filled.Check, contentDescription = null, tint = Smc.OnAccent, modifier = Modifier.size(15.dp))
                SignalState.WARN -> Icon(Icons.Filled.Warning, contentDescription = null, tint = Smc.OnAccent, modifier = Modifier.size(13.dp))
                SignalState.PENDING -> Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Smc.Faint, modifier = Modifier.size(14.dp))
                SignalState.NA -> Box(
                    modifier = Modifier
                        .size(width = 9.dp, height = 2.dp)
                        .background(Smc.Surface),
                )
            }
        }
    }
}

/** Une ligne de récapitulatif : tuile d'icône et texte. */
@Composable
fun SummaryRow(icon: Painter, text: String, tile: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tile) {
            IconTile(icon = icon)
        } else {
            Icon(painter = icon, contentDescription = null, tint = Smc.Accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = text, color = Smc.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Valeur d'une petite animation « pop » : 0 → 1 avec un léger rebond, jouée une fois. */
@Composable
fun rememberPop(): Float {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }
    return pop.value
}

/** Pastille ronde qui « saute » à l'apparition, avec une icône au centre. */
@Composable
fun PopBadge(
    icon: Painter,
    size: Dp,
    brush: Brush,
    tint: Color,
    modifier: Modifier = Modifier,
    glow: Color? = null,
) {
    val pop = rememberPop()
    Box(
        modifier = modifier
            .scale(pop)
            .then(
                if (glow != null) {
                    Modifier.shadow(14.dp, CircleShape, clip = false, ambientColor = glow, spotColor = glow)
                } else {
                    Modifier
                },
            )
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.6f))
    }
}

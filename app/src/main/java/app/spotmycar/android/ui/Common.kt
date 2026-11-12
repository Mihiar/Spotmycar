package app.spotmycar.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.ui.theme.Barlow
import app.spotmycar.android.ui.theme.BarlowCondensed
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Les briques visuelles du style « sombre technique » : petits titres en
 * capitales espacées, gros chiffres en Barlow Condensed, boutons ambre.
 */

val SmcShape = RoundedCornerShape(10.dp)
val SmcCardShape = RoundedCornerShape(12.dp)

/** Petit titre de section : « NIVEAU », « PLACES PRÉCÉDENTES »… */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Smc.Dim) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = Barlow,
        fontSize = 11.5.sp,
        letterSpacing = tracking(0.16f),
        fontWeight = FontWeight.Medium,
    )
}

/** Gros titre condensé en capitales : « AUCUNE PLACE ENREGISTRÉE ». */
@Composable
fun CondensedTitle(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 40.sp,
    color: Color = Smc.Text,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = size,
        lineHeight = size * 1.04f,
        textAlign = textAlign,
    )
}

/** Titre de barre d'écran : « RETOUR À LA VOITURE », « NOUVELLE PLACE »… */
@Composable
fun BarTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = Smc.Text,
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = tracking(0.14f),
        maxLines = 1,
    )
}

/** Le bouton principal ambre (un seul par écran). */
@Composable
fun AmberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = SmcShape,
        colors = ButtonDefaults.buttonColors(containerColor = Smc.Amber, contentColor = Smc.OnAmber),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text.uppercase(),
            fontFamily = BarlowCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            letterSpacing = tracking(0.06f),
        )
    }
}

/** Bouton secondaire : contour gris, texte doux. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    textColor: Color = Smc.Muted,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = SmcShape,
        border = BorderStroke(1.dp, Smc.BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, fontFamily = Barlow, fontSize = 15.sp)
    }
}

/** Bouton de choix (niveau, zone) : ambre quand il est choisi. */
@Composable
fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = SmcShape,
        color = if (selected) Smc.Amber else Smc.SurfaceHigh,
        contentColor = if (selected) Smc.OnAmber else Smc.Muted,
        border = BorderStroke(1.dp, if (selected) Smc.Amber else Smc.Border),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontFamily = BarlowCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
        }
    }
}

/** Carte sombre bordée. */
@Composable
fun SmcCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(SmcCardShape)
            .background(Smc.Surface)
            .border(1.dp, Smc.Border, SmcCardShape)
            .padding(padding),
    ) { content() }
}

/** Une ligne « libellé ... valeur », séparée de la suivante par un trait fin. */
@Composable
fun InfoRow(label: String, value: String, divider: Boolean = true) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = label, color = Smc.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(text = value, color = Smc.Text, fontSize = 13.sp, textAlign = TextAlign.End)
        }
        if (divider) HorizontalDivider(color = Smc.Border, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Interrupteur ambre. */
@Composable
fun SmcSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Smc.Text,
            checkedTrackColor = Smc.Amber,
            checkedBorderColor = Smc.Amber,
            uncheckedThumbColor = Smc.Muted,
            uncheckedTrackColor = Smc.Off,
            uncheckedBorderColor = Smc.Off,
        ),
    )
}

/** Fond hachuré du prototype, utilisé quand il n'y a pas encore de photo. */
fun Modifier.hatched(stripe: Dp = 12.dp): Modifier = drawBehind {
    drawRect(Smc.Hatch1)
    val step = stripe.toPx()
    clipRect {
        var x = -size.height
        var odd = false
        while (x < size.width + size.height) {
            if (odd) {
                val path = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + step, 0f)
                    lineTo(x + step + size.height, size.height)
                    lineTo(x + size.height, size.height)
                    close()
                }
                drawPath(path, Smc.Hatch2)
            }
            odd = !odd
            x += step
        }
    }
}

/** Zone photo vide : hachures, icône appareil photo et une courte légende. */
@Composable
fun PhotoPlaceholder(caption: String, modifier: Modifier = Modifier, dashed: Boolean = false) {
    Column(
        modifier = modifier
            .clip(SmcCardShape)
            .hatched()
            .border(1.dp, if (dashed) Smc.BorderStrong else Smc.Border, SmcCardShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            tint = Smc.Faint,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(10.dp))
        SectionLabel(text = caption, color = Smc.Dim)
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
        Box(modifier = modifier.hatched())
    }
}

/** Pastille ronde ambre (ou éteinte). */
@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/** Barre du haut avec un bouton à gauche, un titre au centre et une place libre à droite. */
@Composable
fun TopBar(
    title: String,
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit = { Spacer(Modifier.width(56.dp)) },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.width(72.dp), verticalAlignment = Alignment.CenterVertically) { start() }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BarTitle(title) }
        Row(
            modifier = Modifier.width(72.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) { end() }
    }
}

/**
 * L'animation signature de SpotMyCar (« pin drop ») : des ondes ambre.
 * [inward] faux : elles partent du centre (on vient de se garer).
 * [inward] vrai : elles reviennent vers le centre (on cherche la voiture).
 */
@Composable
fun PinRings(
    inward: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Smc.Amber,
    center: @Composable () -> Unit = {},
) {
    val transition = rememberInfiniteTransition(label = "rings")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing), RepeatMode.Restart),
        label = "rings-progress",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRings(progress = progress, inward = inward, color = color)
        }
        center()
    }
}

private fun DrawScope.drawRings(progress: Float, inward: Boolean, color: Color) {
    val outer = size.minDimension / 2f
    val inner = outer * 0.22f
    for (index in 0 until 3) {
        val raw = (progress + index / 3f) % 1f
        val p = if (inward) 1f - raw else raw
        val alpha = if (inward) raw * (1f - raw) * 2.4f else (1f - p) * 0.75f
        drawCircle(
            color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = inner + (outer - inner) * p,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}

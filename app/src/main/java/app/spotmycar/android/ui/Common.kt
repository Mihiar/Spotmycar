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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R
import app.spotmycar.android.ui.theme.Outfit
import app.spotmycar.android.ui.theme.Smc
import app.spotmycar.android.ui.theme.tracking
import app.spotmycar.android.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Les briques visuelles du style « terracotta chaleureux » (Spotmy Car) :
 * cartes très arrondies (24–28 dp), boutons en pilule, accent terracotta, Outfit.
 */

val SmcShape = RoundedCornerShape(16.dp)
val SmcCardShape = RoundedCornerShape(24.dp)
val SmcBigCardShape = RoundedCornerShape(28.dp)
val SmcPillShape = RoundedCornerShape(999.dp)

/** Ombre douce et chaude des cartes. */
fun Modifier.softShadow(shape: androidx.compose.ui.graphics.Shape = SmcCardShape, elevation: Dp = 6.dp): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = Smc.Shadow, spotColor = Smc.Shadow)

/** Petit titre de section : « Étage », « Places précédentes »… */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Smc.Muted) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Outfit,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * Grand titre d'écran. Le nom et les tailles demandées viennent de l'ancien
 * style (police condensée) : Outfit est plus large, on réduit.
 */
@Composable
fun CondensedTitle(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 40.sp,
    color: Color = Smc.Text,
    textAlign: TextAlign? = null,
) {
    val shown = (size.value * 0.68f).coerceIn(20f, 30f).sp
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        letterSpacing = tracking(-0.01f),
        fontSize = shown,
        lineHeight = shown * 1.2f,
        textAlign = textAlign,
    )
}

/** Titre de barre d'écran : « Retour à la voiture », « Confirmez l'étage »… */
@Composable
fun BarTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Smc.Text,
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = tracking(-0.01f),
        maxLines = 1,
    )
}

/** Le logo en toutes lettres : SPOT en foncé, MY en terracotta, CAR en foncé (jamais inversé en arabe). */
@Composable
fun Wordmark(size: TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append("SPOT")
            withStyle(SpanStyle(color = Smc.Accent)) { append("MY") }
            append("CAR")
        },
        modifier = modifier,
        color = Smc.Text,
        fontFamily = Outfit,
        fontWeight = FontWeight.ExtraBold,
        fontSize = size,
        letterSpacing = tracking(0.04f),
        maxLines = 1,
    )
}

/** La spirale du logo (rampe de parking vue de dessus), dessinée et non en image. */
@Composable
fun Spiral(color: Color, modifier: Modifier = Modifier, strokeFraction: Float = 0.06f) {
    val path = remember { Path() }
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 100f
        val cx = size.width / 2f
        val cy = size.height / 2f
        path.reset()
        for (i in 0..220) {
            val f = i / 220f
            val angle = f * 2f * PI.toFloat() * 3.2f
            val radius = (4f + f * 40f) * unit
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = size.minDimension * strokeFraction, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Le bouton principal : pilule terracotta, texte brun foncé (un seul par écran). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .shadow(8.dp, SmcPillShape, clip = false, ambientColor = Smc.Accent, spotColor = Smc.Accent)
            .clip(SmcPillShape)
            .background(Smc.Accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = Smc.OnAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = Smc.OnAccent,
            fontFamily = Outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Bouton secondaire : pilule transparente, contour fin. */
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
        modifier = modifier.heightIn(min = 54.dp),
        shape = SmcPillShape,
        border = BorderStroke(1.5.dp, Smc.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = textColor),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** Bouton de choix (niveau, zone) : terracotta quand il est choisi, surface sinon. */
@Composable
fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Smc.Accent else Smc.Surface,
        contentColor = if (selected) Smc.OnAccent else Smc.Text,
        border = BorderStroke(1.dp, if (selected) Smc.Accent else Smc.Border),
        shadowElevation = if (selected) 6.dp else 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

/** Carte blanche arrondie, avec une ombre douce. */
@Composable
fun SmcCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .softShadow()
            .clip(SmcCardShape)
            .background(Smc.Surface)
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
            Text(text = value, color = Smc.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        if (divider) HorizontalDivider(color = Smc.Border, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Interrupteur terracotta. */
@Composable
fun SmcSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Smc.OnAccent,
            checkedTrackColor = Smc.Accent,
            checkedBorderColor = Smc.Accent,
            uncheckedThumbColor = Smc.Surface,
            uncheckedTrackColor = Smc.Off,
            uncheckedBorderColor = Smc.Off,
        ),
    )
}

/** Petite tuile carrée arrondie avec une icône (lignes de signaux, de récapitulatif). */
@Composable
fun IconTile(
    icon: Painter,
    modifier: Modifier = Modifier,
    background: Color = Smc.SurfaceHigh,
    tint: Color = Smc.Accent,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

/** Étiquette en pilule : « Voiture enregistrée », « Direction fiable »… */
@Composable
fun Pill(text: String, background: Color, color: Color, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(
        modifier = modifier
            .clip(SmcPillShape)
            .background(background)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Dot(color = dot, size = 7.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, color = color, fontFamily = Outfit, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
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

/** Pastille ronde de couleur. */
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
        Row(modifier = Modifier.width(80.dp), verticalAlignment = Alignment.CenterVertically) { start() }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { BarTitle(title) }
        Row(
            modifier = Modifier.width(80.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) { end() }
    }
}

/**
 * L'animation signature de SpotMyCar (« pin drop ») : des ondes terracotta (ou couleur encre pour « retrouver »).
 * [inward] faux : elles partent du centre (on vient de se garer).
 * [inward] vrai : elles reviennent vers le centre (on cherche la voiture).
 */
@Composable
fun PinRings(
    inward: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Smc.Accent,
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
    val inner = outer * 0.3f
    for (index in 0 until 4) {
        val raw = (progress + index / 4f) % 1f
        val p = if (inward) 1f - raw else raw
        val alpha = if (inward) raw * (1f - raw) * 2.4f else (1f - p) * 0.75f
        drawCircle(
            color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = inner + (outer - inner) * p,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

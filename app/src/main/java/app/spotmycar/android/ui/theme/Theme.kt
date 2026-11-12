package app.spotmycar.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.spotmycar.android.R

/*
 * Direction A « sombre technique » (prototype 3a de Claude Design) :
 * fond graphite, ambre #F5A524 pour tout ce qui compte, gris doux pour le reste.
 * Le thème est toujours sombre : lisible au fond d'un parking mal éclairé.
 */
object Smc {
    val Background = Color(0xFF131519)
    val Surface = Color(0xFF1B1E24)
    val SurfaceHigh = Color(0xFF22262E)
    val Border = Color(0xFF2A2E36)
    val BorderStrong = Color(0xFF363B45)
    val Hatch1 = Color(0xFF1F232A)
    val Hatch2 = Color(0xFF232830)
    val Text = Color(0xFFE8E6E1)
    val TextSoft = Color(0xFFCFD4DC)
    val Muted = Color(0xFF9AA1AC)
    val Dim = Color(0xFF7D838D)
    val Faint = Color(0xFF5E6570)
    val Amber = Color(0xFFF5A524)
    val AmberLight = Color(0xFFFFC45C)
    val OnAmber = Color(0xFF16181C)
    val Success = Color(0xFF5FD9A8)
    val Off = Color(0xFF2F343D)
}

/** Barlow pour le texte, Barlow Condensed pour les titres et les chiffres. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semi_bold, FontWeight.SemiBold),
)
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semi_bold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/**
 * Espacement des lettres des petits titres en capitales. En arabe, l'écriture
 * est liée : on n'écarte jamais les lettres.
 */
@Composable
@ReadOnlyComposable
fun tracking(value: Float): TextUnit =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else value.em

private val DarkColors = darkColorScheme(
    primary = Smc.Amber,
    onPrimary = Smc.OnAmber,
    primaryContainer = Smc.SurfaceHigh,
    onPrimaryContainer = Smc.Text,
    secondary = Smc.Amber,
    onSecondary = Smc.OnAmber,
    tertiary = Smc.Success,
    onTertiary = Smc.OnAmber,
    background = Smc.Background,
    onBackground = Smc.Text,
    surface = Smc.Surface,
    onSurface = Smc.Text,
    surfaceVariant = Smc.SurfaceHigh,
    onSurfaceVariant = Smc.Muted,
    surfaceContainerHigh = Smc.Surface,
    surfaceContainerHighest = Smc.SurfaceHigh,
    outline = Smc.BorderStrong,
    outlineVariant = Smc.Border,
)

private val SmcTypography = Typography().let { base ->
    fun TextStyle.barlow() = copy(fontFamily = Barlow)
    base.copy(
        displayLarge = base.displayLarge.barlow(),
        displayMedium = base.displayMedium.barlow(),
        displaySmall = base.displaySmall.barlow(),
        headlineLarge = base.headlineLarge.barlow(),
        headlineMedium = base.headlineMedium.barlow(),
        headlineSmall = base.headlineSmall.barlow(),
        titleLarge = base.titleLarge.barlow(),
        titleMedium = base.titleMedium.barlow(),
        titleSmall = base.titleSmall.barlow(),
        bodyLarge = base.bodyLarge.barlow().copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.barlow(),
        bodySmall = base.bodySmall.barlow(),
        labelLarge = base.labelLarge.barlow(),
        labelMedium = base.labelMedium.barlow(),
        labelSmall = base.labelSmall.barlow(),
    )
}

@Composable
fun SpotMyCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SmcTypography,
        content = content,
    )
}

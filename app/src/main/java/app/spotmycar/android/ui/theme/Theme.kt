package app.spotmycar.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
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
 * Direction « clair bleu » (SpotMyCar Prototype v2 de Claude Design) :
 * fond bleu très pâle, cartes blanches arrondies, bleu #1F6BFF pour « garer »,
 * violet #8B6CF6 pour « retrouver », vert pour la fiabilité, orange pour le
 * mode de secours.
 */
object Smc {
    val Background = Color(0xFFF4F7FD)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceHigh = Color(0xFFEAF1FF)
    val Border = Color(0xFFE8EDF6)
    val BorderStrong = Color(0xFFCFD8EA)
    val Hatch1 = Color(0xFFEEF2FA)
    val Hatch2 = Color(0xFFE3EAF6)
    val Text = Color(0xFF0F1A33)
    val TextSoft = Color(0xFF1E2A4A)
    val Muted = Color(0xFF4B5A7C)
    val Dim = Color(0xFF6B7896)
    val Faint = Color(0xFF8A96B3)
    val Blue = Color(0xFF1F6BFF)
    val BlueLight = Color(0xFF4A8BFF)
    val BlueDark = Color(0xFF1D5CF0)
    val BlueSoft = Color(0xFFE3EBFF)
    val OnBlue = Color(0xFFFFFFFF)
    val Violet = Color(0xFF8B6CF6)
    val VioletLight = Color(0xFF7C8CFF)
    val VioletSoft = Color(0xFFF1EDFF)
    val Success = Color(0xFF16A34A)
    val SuccessSoft = Color(0xFFDCFCE7)
    val OnSuccessSoft = Color(0xFF166534)
    val Warn = Color(0xFFD97706)
    val WarnBright = Color(0xFFF59E0B)
    val WarnSoft = Color(0xFFFFF4E0)
    val OnWarnSoft = Color(0xFF92400E)
    val Off = Color(0xFFCFD8EA)

    /** Dégradé du bouton principal et de la carte « Park my car ». */
    val BlueGradient: Brush get() = Brush.linearGradient(listOf(BlueLight, BlueDark))

    /** Dégradé de la carte « Find my car ». */
    val VioletGradient: Brush get() = Brush.linearGradient(listOf(VioletLight, Color(0xFF8B5CF6)))
}

/** Plus Jakarta Sans : une seule famille pour le texte, les titres et les chiffres. */
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semi_bold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extra_bold, FontWeight.ExtraBold),
)

/**
 * Espacement des lettres des petits titres. En arabe, l'écriture
 * est liée : on n'écarte jamais les lettres.
 */
@Composable
@ReadOnlyComposable
fun tracking(value: Float): TextUnit =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else value.em

private val LightColors = lightColorScheme(
    primary = Smc.Blue,
    onPrimary = Smc.OnBlue,
    primaryContainer = Smc.BlueSoft,
    onPrimaryContainer = Smc.Text,
    secondary = Smc.Violet,
    onSecondary = Smc.OnBlue,
    secondaryContainer = Smc.VioletSoft,
    onSecondaryContainer = Smc.Text,
    tertiary = Smc.Success,
    onTertiary = Smc.OnBlue,
    background = Smc.Background,
    onBackground = Smc.Text,
    surface = Smc.Surface,
    onSurface = Smc.Text,
    surfaceVariant = Smc.SurfaceHigh,
    onSurfaceVariant = Smc.Muted,
    surfaceContainerLowest = Smc.Surface,
    surfaceContainerLow = Smc.Surface,
    surfaceContainer = Smc.Surface,
    surfaceContainerHigh = Smc.Surface,
    surfaceContainerHighest = Smc.SurfaceHigh,
    outline = Smc.BorderStrong,
    outlineVariant = Smc.Border,
)

private val SmcTypography = Typography().let { base ->
    fun TextStyle.jakarta() = copy(fontFamily = Jakarta)
    base.copy(
        displayLarge = base.displayLarge.jakarta(),
        displayMedium = base.displayMedium.jakarta(),
        displaySmall = base.displaySmall.jakarta(),
        headlineLarge = base.headlineLarge.jakarta(),
        headlineMedium = base.headlineMedium.jakarta(),
        headlineSmall = base.headlineSmall.jakarta(),
        titleLarge = base.titleLarge.jakarta(),
        titleMedium = base.titleMedium.jakarta(),
        titleSmall = base.titleSmall.jakarta(),
        bodyLarge = base.bodyLarge.jakarta().copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.jakarta(),
        bodySmall = base.bodySmall.jakarta(),
        labelLarge = base.labelLarge.jakarta(),
        labelMedium = base.labelMedium.jakarta(),
        labelSmall = base.labelSmall.jakarta(),
    )
}

@Composable
fun SpotMyCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = SmcTypography,
        content = content,
    )
}

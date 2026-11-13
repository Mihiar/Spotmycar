package app.spotmycar.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * Direction « terracotta chaleureux » (Spotmy Car de Claude Design) :
 * fond crème, cartes arrondies, accent terracotta, Outfit, thèmes clair et sombre.
 * Les valeurs viennent des design tokens de la spec (section 4).
 */

/** Une palette complète : une claire, une sombre. */
@Immutable
class SmcPalette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val muted: Color,
    val dim: Color,
    val faint: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val track: Color,
    val danger: Color,
    val carGray: Color,
    val success: Color,
    val successSoft: Color,
    val onSuccessSoft: Color,
    val warn: Color,
    val warnBright: Color,
    val warnSoft: Color,
    val onWarnSoft: Color,
    val shadow: Color,
    val isDark: Boolean,
)

val LightPalette = SmcPalette(
    background = Color(0xFFF8F4EE),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0E9DF),
    line = Color(0xFFE6DDD1),
    lineStrong = Color(0xFFD6C9B9),
    ink = Color(0xFF2A2420),
    muted = Color(0xFF6E625A),
    dim = Color(0xFF7A6E64),
    faint = Color(0xFFA89B8F),
    accent = Color(0xFFE8794F),
    accentLight = Color(0xFFF08C64),
    accentDark = Color(0xFFDE6A40),
    accentDeep = Color(0xFFC45A33),
    accentSoft = Color(0xFFFBE4D9),
    track = Color(0xFFEFE7DC),
    danger = Color(0xFFD4483B),
    carGray = Color(0xFFDDD4C8),
    success = Color(0xFF2F8F57),
    successSoft = Color(0xFFE1F2E4),
    onSuccessSoft = Color(0xFF1E5E39),
    warn = Color(0xFFC9761B),
    warnBright = Color(0xFFE9A23B),
    warnSoft = Color(0xFFFBEFD9),
    onWarnSoft = Color(0xFF7A4510),
    shadow = Color(0x33281A0A),
    isDark = false,
)

val DarkPalette = SmcPalette(
    background = Color(0xFF1B1714),
    surface = Color(0xFF27211D),
    surface2 = Color(0xFF312A25),
    line = Color(0xFF3D342E),
    lineStrong = Color(0xFF4E433C),
    ink = Color(0xFFF5EEE6),
    muted = Color(0xFFAB9E92),
    dim = Color(0xFF9C8F83),
    faint = Color(0xFF7D7066),
    accent = Color(0xFFF08A62),
    accentLight = Color(0xFFF59873),
    accentDark = Color(0xFFE87A50),
    accentDeep = Color(0xFFF5A27F),
    accentSoft = Color(0xFF4A2C20),
    track = Color(0xFF3A312B),
    danger = Color(0xFFFF6F5F),
    carGray = Color(0xFF463C35),
    success = Color(0xFF5BC489),
    successSoft = Color(0xFF1F3427),
    onSuccessSoft = Color(0xFFA6E3BE),
    warn = Color(0xFFF0A24A),
    warnBright = Color(0xFFF5B85E),
    warnSoft = Color(0xFF3D2E18),
    onWarnSoft = Color(0xFFF7D59C),
    shadow = Color(0x66000000),
    isDark = true,
)

/**
 * Les couleurs de l'app. Elles suivent le thème du système (clair / sombre) :
 * [SpotMyCarTheme] choisit la palette, et chaque lecture de `Smc.X` (dans un
 * écran comme dans un dessin) se met à jour toute seule quand elle change.
 */
object Smc {
    var palette: SmcPalette by mutableStateOf(LightPalette)
        internal set

    val Background get() = palette.background
    val Surface get() = palette.surface
    val SurfaceHigh get() = palette.surface2
    val Border get() = palette.line
    val BorderStrong get() = palette.lineStrong
    val Hatch1 get() = palette.surface2
    val Hatch2 get() = palette.background
    val Text get() = palette.ink
    val TextSoft get() = palette.ink
    val Muted get() = palette.muted
    val Dim get() = palette.dim
    val Faint get() = palette.faint

    /** L'accent terracotta : « garer », boutons principaux, pas, voiture. */
    val Accent get() = palette.accent
    val AccentLight get() = palette.accentLight
    val AccentDark get() = palette.accentDark

    /** Terracotta plus foncé, pour du texte lisible sur fond clair (« P2 · Place 14 »). */
    val AccentDeep get() = palette.accentDeep
    val AccentSoft get() = palette.accentSoft

    /** Texte et icônes posés sur l'accent. */
    val OnAccent get() = Color(0xFF2A1A12)

    /** « Retrouver » : l'encre, comme le bouton « Find my car » du design. */
    val Find get() = palette.ink
    val FindSoft get() = palette.surface2
    val OnFind get() = palette.background

    val Track get() = palette.track
    val Danger get() = palette.danger
    val CarGray get() = palette.carGray

    /** Point bleu « vous êtes ici » et phares : identiques en clair et en sombre. */
    val User get() = Color(0xFF4A8FD8)
    val Headlight get() = Color(0xFFFFD98A)

    val Success get() = palette.success
    val SuccessSoft get() = palette.successSoft
    val OnSuccessSoft get() = palette.onSuccessSoft
    val Warn get() = palette.warn
    val WarnBright get() = palette.warnBright
    val WarnSoft get() = palette.warnSoft
    val OnWarnSoft get() = palette.onWarnSoft
    val Off get() = palette.line
    val Shadow get() = palette.shadow

    /** Bouton principal et carte « Garer » : un terracotta presque uni. */
    val AccentGradient: Brush get() = Brush.linearGradient(listOf(AccentLight, AccentDark))

    /** Carte « Retrouver » : l'encre unie. */
    val FindGradient: Brush get() = Brush.linearGradient(listOf(Find, Find))
}

/** Outfit : la police du texte, des titres et des chiffres. */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semi_bold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extra_bold, FontWeight.ExtraBold),
)

/** JetBrains Mono : petits libellés et numéros de place. */
val Mono = FontFamily(Font(R.font.jetbrains_mono_medium, FontWeight.Medium))

/** Chiffres à chasse fixe : le minuteur et les mètres ne « sautent » pas. */
const val TabularNums = "tnum"

/**
 * Espacement des lettres des petits titres. En arabe, l'écriture
 * est liée : on n'écarte jamais les lettres.
 */
@Composable
@ReadOnlyComposable
fun tracking(value: Float): TextUnit =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) 0.sp else value.em

private fun SmcPalette.colorScheme() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = Color(0xFF2A1A12),
        primaryContainer = accentSoft, onPrimaryContainer = ink,
        secondary = ink, onSecondary = background,
        secondaryContainer = surface2, onSecondaryContainer = ink,
        tertiary = success, onTertiary = background,
        background = background, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = surface2, onSurfaceVariant = muted,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surface,
        surfaceContainerHighest = surface2,
        outline = lineStrong, outlineVariant = line,
        error = danger,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = Color(0xFF2A1A12),
        primaryContainer = accentSoft, onPrimaryContainer = ink,
        secondary = ink, onSecondary = background,
        secondaryContainer = surface2, onSecondaryContainer = ink,
        tertiary = success, onTertiary = surface,
        background = background, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = surface2, onSurfaceVariant = muted,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surface,
        surfaceContainerHighest = surface2,
        outline = lineStrong, outlineVariant = line,
        error = danger,
    )
}

private val SmcTypography = Typography().let { base ->
    fun TextStyle.outfit() = copy(fontFamily = Outfit)
    base.copy(
        displayLarge = base.displayLarge.outfit(),
        displayMedium = base.displayMedium.outfit(),
        displaySmall = base.displaySmall.outfit(),
        headlineLarge = base.headlineLarge.outfit(),
        headlineMedium = base.headlineMedium.outfit(),
        headlineSmall = base.headlineSmall.outfit(),
        titleLarge = base.titleLarge.outfit(),
        titleMedium = base.titleMedium.outfit(),
        titleSmall = base.titleSmall.outfit(),
        bodyLarge = base.bodyLarge.outfit().copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.outfit().copy(fontSize = 15.sp),
        bodySmall = base.bodySmall.outfit().copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.outfit(),
        labelMedium = base.labelMedium.outfit(),
        labelSmall = base.labelSmall.outfit(),
    )
}

@Composable
fun SpotMyCarTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    // Écrit avant que les écrans ne lisent la palette : pas de retour en arrière dans la composition.
    if (Smc.palette !== palette) Smc.palette = palette
    MaterialTheme(
        colorScheme = palette.colorScheme(),
        typography = SmcTypography,
        content = content,
    )
}

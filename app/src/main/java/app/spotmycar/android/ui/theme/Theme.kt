package app.spotmycar.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette SpotMyCar : un bleu « panneau de parking », un ambre pour l'épingle,
// un vert pour « retrouvée ». Contrastes lisibles à tout âge.
val SignBlue = Color(0xFF1F4FD8)
val PinAmber = Color(0xFFF5A30B)
val FoundGreen = Color(0xFF0E9F6E)

private val LightColors = lightColorScheme(
    primary = SignBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0A2472),
    secondary = PinAmber,
    onSecondary = Color(0xFF2B1B00),
    secondaryContainer = Color(0xFFFFEBC2),
    onSecondaryContainer = Color(0xFF3D2800),
    tertiary = FoundGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF5E4),
    onTertiaryContainer = Color(0xFF053D29),
    background = Color(0xFFF3F5F9),
    onBackground = Color(0xFF142033),
    surface = Color.White,
    onSurface = Color(0xFF142033),
    surfaceVariant = Color(0xFFE4E8F0),
    onSurfaceVariant = Color(0xFF46536A),
    outline = Color(0xFF8A94A6),
    outlineVariant = Color(0xFFCBD2DE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB7FF),
    onPrimary = Color(0xFF05205F),
    primaryContainer = Color(0xFF173A9E),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFFFC14D),
    onSecondary = Color(0xFF3D2800),
    secondaryContainer = Color(0xFF5A3D00),
    onSecondaryContainer = Color(0xFFFFEBC2),
    tertiary = Color(0xFF5FD9A8),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = Color(0xFF07513A),
    onTertiaryContainer = Color(0xFFCFF5E4),
    background = Color(0xFF0E1522),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF172133),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF263247),
    onSurfaceVariant = Color(0xFFB4BED0),
    outline = Color(0xFF7D889C),
    outlineVariant = Color(0xFF3A465C),
)

@Composable
fun SpotMyCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

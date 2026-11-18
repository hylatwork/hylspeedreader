package eu.hylatwork.hylspeedreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Brand palette: the navy of the launcher mark, with a matching desaturated
 * secondary for container surfaces (e.g. the chapter drawer's selected pill).
 * Tertiary, error and the surface family stay on the Material 3 baseline tones,
 * which are already contrast-safe.
 *
 * The RSVP focal character is rendered in [ColorScheme.error] — red in both
 * palettes, the classic RSVP look — rather than a brand role, so it stays the
 * single highest-salience element on screen.
 */
private val NavyLightScheme = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FA),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF4F5F72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4FA),
    onSecondaryContainer = Color(0xFF0B1F33)
)

private val NavyDarkScheme = darkColorScheme(
    primary = Color(0xFFA4C8F0),
    onPrimary = Color(0xFF003257),
    primaryContainer = Color(0xFF17466E),
    onPrimaryContainer = Color(0xFFD3E4FA),
    secondary = Color(0xFFB6C8DC),
    onSecondary = Color(0xFF203243),
    secondaryContainer = Color(0xFF374A5D),
    onSecondaryContainer = Color(0xFFD3E4FA)
)

/**
 * Material 3 theme for HylSpeedReader.
 *
 * @param dynamicColor when true (default) and the platform supports it
 *   (Android 12+), the wallpaper-derived Material You scheme is used; otherwise
 *   the navy [NavyLightScheme]/[NavyDarkScheme] brand palette is used.
 */
@Composable
fun HylSpeedReaderTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val dynamic = if (dynamicColor) platformDynamicColorScheme(dark) else null
    val colors = remember(dark, dynamic) {
        dynamic ?: if (dark) NavyDarkScheme else NavyLightScheme
    }
    MaterialTheme(colorScheme = colors, content = content)
}

package eu.hylatwork.hylspeedreader.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Wallpaper-derived Material You scheme where the platform supports it
 * (Android 12+), or null when it does not (older Android, iOS) so the caller
 * falls back to the navy brand palette.
 */
@Composable
expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?

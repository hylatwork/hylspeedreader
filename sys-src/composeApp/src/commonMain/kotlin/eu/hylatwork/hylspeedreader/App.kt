package eu.hylatwork.hylspeedreader

import androidx.compose.runtime.Composable
import eu.hylatwork.hylspeedreader.ui.reader.ReaderScreen
import eu.hylatwork.hylspeedreader.ui.theme.HylSpeedReaderTheme

/**
 * Platform-neutral application root. Android hosts it from `MainActivity`, iOS
 * from `MainViewController`; both simply mount [App] inside the Compose runtime.
 */
@Composable
fun App() {
    HylSpeedReaderTheme {
        ReaderScreen()
    }
}

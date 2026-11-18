package eu.hylatwork.hylspeedreader.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.hylatwork.hylspeedreader.data.Platform
import eu.hylatwork.hylspeedreader.data.position.SettingsReadingPositionStore

/**
 * Composition root for the reader: wires the platform services into
 * [ReaderViewModel]. Lives outside `ui/reader` so the screen composables stay
 * free of `data` imports and the architecture guard can keep enforcing that
 * boundary (only this entry point may touch the data layer).
 */
@Composable
fun rememberReaderViewModel(): ReaderViewModel = viewModel {
    ReaderViewModel(Platform.epubEngine(), SettingsReadingPositionStore(Platform.settings()))
}

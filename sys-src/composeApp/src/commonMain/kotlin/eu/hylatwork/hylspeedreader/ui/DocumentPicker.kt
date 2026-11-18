package eu.hylatwork.hylspeedreader.ui

import androidx.compose.runtime.Composable
import eu.hylatwork.hylspeedreader.data.epub.DocumentHandle

/**
 * Returns a launcher that opens the platform's document picker and delivers the
 * chosen handle (or null when the user cancels). Android uses the Storage Access
 * Framework; iOS uses `UIDocumentPickerViewController`.
 */
@Composable
expect fun rememberEpubPicker(onResult: (DocumentHandle?) -> Unit): () -> Unit

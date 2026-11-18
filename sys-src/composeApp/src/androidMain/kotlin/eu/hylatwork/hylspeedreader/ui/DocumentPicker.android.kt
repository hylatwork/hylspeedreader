package eu.hylatwork.hylspeedreader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import eu.hylatwork.hylspeedreader.data.epub.AndroidDocumentHandle
import eu.hylatwork.hylspeedreader.data.epub.DocumentHandle

/**
 * Android picker backed by the Storage Access Framework (no storage
 * permission needed). `setContent` from a `ComponentActivity` provides the
 * activity-result registry this launcher registers against.
 */
@Composable
actual fun rememberEpubPicker(onResult: (DocumentHandle?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onResult(uri?.let(::AndroidDocumentHandle))
    }
    return { launcher.launch(arrayOf("application/epub+zip", "application/octet-stream")) }
}

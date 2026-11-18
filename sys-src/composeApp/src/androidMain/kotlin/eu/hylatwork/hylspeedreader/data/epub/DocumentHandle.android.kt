package eu.hylatwork.hylspeedreader.data.epub

import android.content.Intent
import android.net.Uri
import eu.hylatwork.hylspeedreader.data.Platform

/** Android handle: a SAF `content://` (or `file://`) [Uri]. */
class AndroidDocumentHandle(val uri: Uri) : DocumentHandle {

    override val persistedValue: String get() = uri.toString()

    override fun takePersistableAccess() {
        runCatching {
            Platform.appContext.contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

actual fun persistedStringToDocumentHandle(value: String): DocumentHandle? =
    runCatching { Uri.parse(value) }.getOrNull()
        ?.takeIf { it.toString().isNotBlank() }
        ?.let(::AndroidDocumentHandle)

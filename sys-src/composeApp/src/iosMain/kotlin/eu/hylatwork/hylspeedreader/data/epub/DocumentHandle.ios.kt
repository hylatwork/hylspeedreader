package eu.hylatwork.hylspeedreader.data.epub

import platform.Foundation.NSURL

/** iOS handle: a file `NSURL` (copied into the app sandbox). */
class IosDocumentHandle(val url: NSURL) : DocumentHandle {

    override val persistedValue: String get() = url.absoluteString ?: ""

    override fun takePersistableAccess() {
        // The picker is configured with `asCopy = true`, so the file is already
        // copied into the app's sandbox and stays readable without bookkeeping.
    }
}

actual fun persistedStringToDocumentHandle(value: String): DocumentHandle? =
    value.takeIf { it.isNotBlank() }
        ?.let { NSURL.URLWithString(it) }
        ?.let(::IosDocumentHandle)

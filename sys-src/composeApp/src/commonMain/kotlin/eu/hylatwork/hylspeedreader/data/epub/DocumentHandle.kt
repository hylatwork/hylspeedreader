package eu.hylatwork.hylspeedreader.data.epub

/**
 * Opaque handle for a document the user picked through the system file picker.
 * Each platform provides its own implementation that carries the underlying
 * handle (an Android `content://` **Uri**, an iOS **NSURL**), so the shared
 * playback code can pass it around and persist it without touching a platform
 * type.
 */
interface DocumentHandle {

    /** A stable string form suitable for persistence. */
    val persistedValue: String

    /**
     * Requests long-lived read access where the platform requires it (Android
     * SAF persistable permission); a no-op where access is already durable.
     */
    fun takePersistableAccess()
}

/** Rebuilds a handle from its persisted form, or null when it is unusable. */
expect fun persistedStringToDocumentHandle(value: String): DocumentHandle?

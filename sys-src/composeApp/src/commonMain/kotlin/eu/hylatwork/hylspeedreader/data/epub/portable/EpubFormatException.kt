package eu.hylatwork.hylspeedreader.data.epub.portable

/**
 * Raised when a portable archive/stream is malformed beyond recovery. A plain
 * common `Exception` so it can cross the `data` seam without a platform type
 * (the `ReaderViewModel` already maps any failure to a resource-backed message).
 */
internal class EpubFormatException(message: String) : Exception(message)

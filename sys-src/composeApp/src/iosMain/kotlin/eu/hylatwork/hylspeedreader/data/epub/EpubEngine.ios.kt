package eu.hylatwork.hylspeedreader.data.epub

import eu.hylatwork.hylspeedreader.data.Platform
import eu.hylatwork.hylspeedreader.data.epub.portable.PortableEpubParser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.posix.memcpy

actual fun createEpubEngine(): EpubEngine = IosEpubEngine()

/**
 * iOS EPUB codec: reads the sandboxed copy (the picker opens with `asCopy = true`)
 * into memory and parses it with the shared, dependency-free [PortableEpubParser]
 * (pure-Kotlin ZIP + DEFLATE + XML), so both platforms offer the same reader
 * experience (DC-4).
 */
private class IosEpubEngine : EpubEngine {

    override suspend fun open(handle: DocumentHandle): EpubBook {
        val url = (handle as IosDocumentHandle).url
        val bytes = withContext(Platform.ioDispatcher) { readBytes(url) }
        return withContext(Platform.ioDispatcher) { PortableEpubParser.parse(bytes) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readBytes(url: NSURL): ByteArray {
    val data = NSData.dataWithContentsOfURL(url)
        ?: throw IllegalStateException("The selected document could not be opened")
    val size = data.length.toInt()
    if (size == 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}

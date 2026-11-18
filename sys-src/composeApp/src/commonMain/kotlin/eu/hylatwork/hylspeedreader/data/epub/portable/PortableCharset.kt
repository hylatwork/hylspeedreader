package eu.hylatwork.hylspeedreader.data.epub.portable

/**
 * Decodes raw EPUB entry bytes on platforms without `java.nio.charset`
 * (notably Kotlin/Native): a UTF byte-order mark wins, otherwise a declared
 * `encoding="…"` in the prolog is honoured for the common cases, and everything
 * else falls back to UTF-8 (lossy but non-fatal). Mirrors the Android
 * `EpubCharset` behaviour for the encodings EPUBs actually use (FR-2.4 / NFR-2.2).
 */
internal object PortableCharset {

    private val DECLARED_ENCODING = Regex("encoding\\s*=\\s*[\"']([A-Za-z0-9._:-]+)[\"']")

    private const val SNIFF_BYTES = 1024

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        when {
            bytes.startsWith(0xEF, 0xBB, 0xBF) -> return utf8(bytes, 3)
            bytes.startsWith(0xFF, 0xFE) -> return utf16(bytes, 2, littleEndian = true)
            bytes.startsWith(0xFE, 0xFF) -> return utf16(bytes, 2, littleEndian = false)
        }

        val prolog = latin1(bytes, 0, minOf(bytes.size, SNIFF_BYTES)).lowercase()
        val declared = DECLARED_ENCODING.find(prolog)?.groupValues?.get(1)
        return when {
            declared == null -> utf8(bytes, 0)
            "utf-16" in declared || "utf16" in declared -> utf16(bytes, 0, littleEndian = true)
            "iso-8859-1" in declared || "latin1" in declared ||
                "windows-1252" in declared || "cp1252" in declared -> latin1(bytes, 0, bytes.size)
            else -> utf8(bytes, 0)
        }
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if ((this[i].toInt() and 0xFF) != prefix[i]) return false
        return true
    }

    private fun utf8(bytes: ByteArray, from: Int): String =
        bytes.copyOfRange(from, bytes.size).decodeToString()

    private fun latin1(bytes: ByteArray, from: Int, to: Int): String {
        val builder = StringBuilder(to - from)
        for (i in from until to) builder.append((bytes[i].toInt() and 0xFF).toChar())
        return builder.toString()
    }

    private fun utf16(bytes: ByteArray, from: Int, littleEndian: Boolean): String {
        val builder = StringBuilder((bytes.size - from) / 2)
        var i = from
        while (i + 1 < bytes.size) {
            val a = bytes[i].toInt() and 0xFF
            val b = bytes[i + 1].toInt() and 0xFF
            val unit = if (littleEndian) (a or (b shl 8)) else ((a shl 8) or b)
            builder.append(unit.toChar())
            i += 2
        }
        return builder.toString()
    }
}

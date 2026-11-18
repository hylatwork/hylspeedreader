package eu.hylatwork.hylspeedreader.data.epub

import java.nio.charset.Charset

/**
 * Decodes raw EPUB entry bytes, honouring a UTF byte-order mark or the encoding
 * declared in an XML/HTML prolog, so chapters that are not UTF-8 do not turn
 * into replacement-character mojibake (FR-2.4 / NFR-2.2).
 */
internal object EpubCharset {

    /** `encoding="..."` / `encoding='...'` in an XML declaration or meta tag. */
    private val DECLARED_ENCODING = Regex("encoding\\s*=\\s*[\"']([A-Za-z0-9._:-]+)[\"']")

    /** How many leading bytes to inspect for a declared encoding. */
    private const val SNIFF_BYTES = 1024

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        bom(bytes)?.let { (charset, offset) ->
            return String(bytes, offset, bytes.size - offset, charset)
        }
        val prolog = String(bytes, 0, minOf(bytes.size, SNIFF_BYTES), Charsets.ISO_8859_1)
        val declared = DECLARED_ENCODING.find(prolog)?.groupValues?.get(1)
        val charset = declared
            ?.let { runCatching { Charset.forName(it.trim()) }.getOrNull() }
            ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    private fun bom(bytes: ByteArray): Pair<Charset, Int>? = when {
        bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            Charsets.UTF_8 to 3

        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            Charsets.UTF_16LE to 2

        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            Charsets.UTF_16BE to 2

        else -> null
    }
}

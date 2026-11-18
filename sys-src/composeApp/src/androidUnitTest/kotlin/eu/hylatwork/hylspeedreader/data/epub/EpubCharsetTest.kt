package eu.hylatwork.hylspeedreader.data.epub

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for encoding-aware entry decoding (FR-2.4 / NFR-2.2).
 */
class EpubCharsetTest {

    @Test
    fun decodesUtf8ByDefault() {
        val text = "<p>caf\u00e9 \u2014 \u201cquoted\u201d</p>"
        assertEquals(text, EpubCharset.decode(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun stripsUtf8ByteOrderMark() {
        val text = "<p>hello</p>"
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray(Charsets.UTF_8)
        assertEquals(text, EpubCharset.decode(bytes))
    }

    @Test
    fun decodesUtf16LittleEndianWithBom() {
        val text = "<p>h\u00e9llo</p>"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)
        assertEquals(text, EpubCharset.decode(bytes))
    }

    @Test
    fun honoursDeclaredSingleByteEncoding() {
        val text = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><p>caf\u00e9</p>"
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val decoded = EpubCharset.decode(bytes)
        assertEquals(text, decoded)
    }

    @Test
    fun fallsBackToUtf8OnUnknownDeclaredEncoding() {
        val text = "<?xml version=\"1.0\" encoding=\"not-a-charset\"?><p>plain</p>"
        assertEquals(text, EpubCharset.decode(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun emptyInputDecodesToEmptyString() {
        assertEquals("", EpubCharset.decode(ByteArray(0)))
    }
}

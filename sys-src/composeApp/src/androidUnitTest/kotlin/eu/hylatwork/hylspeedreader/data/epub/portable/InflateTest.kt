package eu.hylatwork.hylspeedreader.data.epub.portable

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

/**
 * Verifies the pure-Kotlin DEFLATE decoder against streams produced by the JDK
 * `Deflater` (raw, `nowrap = true`), across every level so stored, fixed-Huffman
 * and dynamic-Huffman blocks are all exercised.
 */
class InflateTest {

    private fun deflate(data: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level, true)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val written = deflater.deflate(buffer)
            out.write(buffer, 0, written)
        }
        deflater.end()
        return out.toByteArray()
    }

    @Test
    fun `round trips text at every compression level`() {
        val data = "HylSpeedReader reads local EPUB books one word at a time.".repeat(60)
            .toByteArray(Charsets.UTF_8)
        for (level in intArrayOf(
            Deflater.NO_COMPRESSION,
            Deflater.BEST_SPEED,
            Deflater.DEFAULT_COMPRESSION,
            Deflater.BEST_COMPRESSION
        )) {
            assertArrayEquals("level $level", data, Inflate.inflateRaw(deflate(data, level)))
        }
    }

    @Test
    fun `round trips Unicode punctuation and accented letters`() {
        val data = "don’t — “quoted” … naïve café 你好 1,234".repeat(40).toByteArray(Charsets.UTF_8)
        assertArrayEquals(data, Inflate.inflateRaw(deflate(data, Deflater.DEFAULT_COMPRESSION)))
    }

    @Test
    fun `round trips binary data that forces dynamic Huffman blocks`() {
        var seed = 0x123456789ABCDEFL
        val data = ByteArray(20_000) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            (seed ushr 33).toByte()
        }
        assertArrayEquals(data, Inflate.inflateRaw(deflate(data, Deflater.BEST_COMPRESSION)))
    }

    @Test
    fun `round trips empty and highly repetitive payloads`() {
        assertArrayEquals(ByteArray(0), Inflate.inflateRaw(deflate(ByteArray(0), Deflater.DEFAULT_COMPRESSION)))
        val repeated = ByteArray(10_000) { 7 }
        assertArrayEquals(repeated, Inflate.inflateRaw(deflate(repeated, Deflater.BEST_COMPRESSION)))
    }

    @Test
    fun `round trips an uncompressed stored stream`() {
        val data = ByteArray(1_000) { (it % 251).toByte() }
        assertArrayEquals(data, Inflate.inflateRaw(deflate(data, Deflater.NO_COMPRESSION)))
    }
}

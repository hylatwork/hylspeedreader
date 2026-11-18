package eu.hylatwork.hylspeedreader.data.epub.portable

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Verifies the portable ZIP reader against archives written by `java.util.zip`. */
class PortableZipTest {

    private fun zipOf(build: ZipOutputStream.() -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { it.build() }
        return out.toByteArray()
    }

    @Test
    fun `reads deflated entries`() {
        val payload = "chapter text ".repeat(200).toByteArray()
        val bytes = zipOf {
            putNextEntry(ZipEntry("META-INF/container.xml"))
            write("<container/>".toByteArray())
            closeEntry()
            putNextEntry(ZipEntry("OEBPS/ch.xhtml"))
            write(payload)
            closeEntry()
        }
        val zip = PortableZip(bytes)
        assertArrayEquals("<container/>".toByteArray(), zip.read("META-INF/container.xml"))
        assertArrayEquals(payload, zip.read("OEBPS/ch.xhtml"))
    }

    @Test
    fun `reads stored entries`() {
        val payload = "stored bytes, no compression here".toByteArray()
        val crc = CRC32().apply { update(payload) }.value
        val bytes = zipOf {
            putNextEntry(
                ZipEntry("plain.txt").apply {
                    method = ZipEntry.STORED
                    size = payload.size.toLong()
                    compressedSize = payload.size.toLong()
                    this.crc = crc
                }
            )
            write(payload)
            closeEntry()
        }
        assertArrayEquals(payload, PortableZip(bytes).read("plain.txt"))
    }

    @Test
    fun `looks entries up case-insensitively and returns null when absent`() {
        val bytes = zipOf {
            putNextEntry(ZipEntry("OEBPS/Content.opf"))
            write("opf".toByteArray())
            closeEntry()
        }
        val zip = PortableZip(bytes)
        assertArrayEquals("opf".toByteArray(), zip.read("oebps/content.opf"))
        assertNull(zip.read("missing.xhtml"))
        assertEquals(1, zip.entry("OEBPS/Content.opf")?.name?.let { 1 })
    }
}

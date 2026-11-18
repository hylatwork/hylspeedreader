package eu.hylatwork.hylspeedreader.data.epub.portable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * End-to-end verification of the portable EPUB codec (the iOS path, DC-4):
 * a real EPUB archive, built on the JVM, parsed and tokenized entirely by the
 * shared pure-Kotlin code.
 */
class PortableEpubTest {

    private fun zipOf(entries: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun sampleEpub(): ByteArray {
        val container = """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
        """.trimIndent()

        val opf = """
            <?xml version="1.0" encoding="utf-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Test Book</dc:title>
                <dc:creator>Ada Lovelace</dc:creator>
              </metadata>
              <manifest>
                <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                <item id="c1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/>
                <item id="c2" href="text/ch2.xhtml" media-type="application/xhtml+xml"/>
                <item id="css" href="style.css" media-type="text/css"/>
              </manifest>
              <spine>
                <itemref idref="nav"/>
                <itemref idref="c1"/>
                <itemref idref="c2"/>
                <itemref idref="css"/>
              </spine>
            </package>
        """.trimIndent()

        val ch1 = """
            <html><head><title>Hidden title</title></head><body>
              <h1>Chapter One</h1>
              <p>The quick brown fox.</p>
              <p>Second paragraph!</p>
              <script>var x = 1;</script>
            </body></html>
        """.trimIndent()

        val ch2 = "<html><body><h1>Chapter Two</h1><p>End.</p></body></html>"
        val nav = "<html><body><nav epub:type=\"toc\"><ol><li>One</li></ol></nav></body></html>"

        return zipOf(
            mapOf(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to opf,
                "OEBPS/nav.xhtml" to nav,
                "OEBPS/text/ch1.xhtml" to ch1,
                "OEBPS/text/ch2.xhtml" to ch2,
                "OEBPS/style.css" to "p { color: black; }"
            )
        )
    }

    @Test
    fun `parses metadata and the linear spine only`() {
        val book = PortableEpubParser.parse(sampleEpub())
        assertEquals("Test Book", book.title)
        assertEquals("Ada Lovelace", book.author)
        assertEquals(2, book.chapters.size) // nav document and CSS are excluded
        assertEquals("Chapter One", book.chapters[0].title)
        assertEquals("Chapter Two", book.chapters[1].title)
        book.close()
    }

    @Test
    fun `tokenizes chapter text and strips scripts`() {
        val book = PortableEpubParser.parse(sampleEpub())
        val words = book.loadChapter(0).map { it.raw }
        assertTrue(words.contains("quick"))
        assertFalse(words.any { it.contains("var") })
        assertTrue(words.contains("paragraph!"))
        assertTrue(book.loadChapter(0).last().endsParagraph)
    }

    @Test
    fun `second chapter loads independently`() {
        val book = PortableEpubParser.parse(sampleEpub())
        val words = book.loadChapter(1).map { it.raw }
        assertTrue(words.contains("End."))
    }
}

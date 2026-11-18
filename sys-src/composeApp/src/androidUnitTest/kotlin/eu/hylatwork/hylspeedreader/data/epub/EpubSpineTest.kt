package eu.hylatwork.hylspeedreader.data.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for EPUB spine resolution (FR-2.1 / FR-2.2) — document order,
 * `linear="no"`, the EPUB 3 navigation document, media-type filtering and
 * relative href resolution.
 */
class EpubSpineTest {

    private val opf = """
        <?xml version="1.0" encoding="utf-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
          <metadata><dc:title>Title</dc:title></metadata>
          <manifest>
            <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
            <item id="c1" href="text/ch1.xhtml" media-type="application/xhtml+xml"/>
            <item id="c2" href="../ch2.xhtml" media-type="text/html"/>
            <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml" properties="cover-image"/>
            <item id="plain" href="plain.xhtml" media-type="application/octet-stream"/>
            <item id="css" href="style.css" media-type="text/css"/>
            <item id="img" href="cover.jpg" media-type="image/jpeg"/>
          </manifest>
          <spine>
            <itemref idref="c1"/>
            <itemref idref="nav"/>
            <itemref idref="c2"/>
            <itemref idref="cover"/>
            <itemref idref="css"/>
            <itemref idref="img"/>
            <itemref idref="plain"/>
            <itemref idref="missing"/>
          </spine>
        </package>
    """.trimIndent()

    @Test
    fun keepsSpineOrderResolvesPathsAndFiltersNonDocuments() {
        assertEquals(
            listOf("OEBPS/text/ch1.xhtml", "ch2.xhtml", "OEBPS/cover.xhtml"),
            EpubSpine.resolve(opf, "OEBPS")
        )
    }

    @Test
    fun dropsLinearNoEntriesCaseInsensitively() {
        val linearNo = opf.replace("<itemref idref=\"c1\"/>", "<itemref idref=\"c1\" linear=\"NO\"/>")
        assertEquals(
            listOf("ch2.xhtml", "OEBPS/cover.xhtml"),
            EpubSpine.resolve(linearNo, "OEBPS")
        )
    }

    @Test
    fun navIsMatchedByWholeTokenOnly() {
        val notNav = opf.replace("properties=\"nav\"", "properties=\"navigation\"")
        assertTrue(EpubSpine.resolve(notNav, "OEBPS").contains("OEBPS/nav.xhtml"))
    }

    @Test
    fun returnsEmptyWhenSpineHasNoReadableDocuments() {
        val onlyCss = """
            <package>
              <manifest><item id="css" href="s.css" media-type="text/css"/></manifest>
              <spine><itemref idref="css"/></spine>
            </package>
        """.trimIndent()
        assertEquals(emptyList<String>(), EpubSpine.resolve(onlyCss, ""))
    }

    @Test
    fun resolvePathHandlesParentSegmentsAndPercentDecoding() {
        assertEquals("OEBPS/a.xhtml", EpubSpine.resolvePath("OEBPS", "a.xhtml"))
        assertEquals("OEBPS/images/a.xhtml", EpubSpine.resolvePath("OEBPS/text", "../images/a.xhtml"))
        assertEquals("OEBPS/a b.xhtml", EpubSpine.resolvePath("OEBPS", "a%20b.xhtml"))
        assertEquals("OEBPS/text/x/y.xhtml", EpubSpine.resolvePath("OEBPS/text", "./x/./y.xhtml"))
        assertEquals("a.xhtml", EpubSpine.resolvePath("", "a.xhtml"))
        assertEquals("x/y.xhtml", EpubSpine.resolvePath("OEBPS", "/x/y.xhtml"))
    }
}

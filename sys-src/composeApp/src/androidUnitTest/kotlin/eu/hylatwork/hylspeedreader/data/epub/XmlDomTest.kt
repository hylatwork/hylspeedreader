package eu.hylatwork.hylspeedreader.data.epub

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for the dependency-free XML layer that replaced jsoup:
 * DOCTYPE tolerance, named/numeric entity decoding, unknown-entity safety and
 * XXE containment.
 */
class XmlDomTest {

    private fun textOf(xml: String, tag: String): String =
        XmlDom.parse(xml).elements(tag).firstOrNull()?.text().orEmpty()

    @Test
    fun `a public doctype is tolerated`() {
        val xml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" " +
            "\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
            "<html><body><p>ok</p></body></html>"
        assertEquals("ok", textOf(xml, "p"))
    }

    @Test
    fun `named entities are decoded`() {
        assertEquals("Caf\u00e9 \u2014 a\u00a0b", textOf("<r><t>Caf&eacute; &mdash; a&nbsp;b</t></r>", "t"))
    }

    @Test
    fun `numeric references are decoded`() {
        assertEquals("Tom & Jerry \u2014", textOf("<r><t>Tom &amp; Jerry &#8212;</t></r>", "t"))
    }

    @Test
    fun `an unknown named entity becomes literal text, not an error`() {
        assertEquals("x &foo; y", textOf("<r><t>x &foo; y</t></r>", "t"))
    }

    @Test
    fun `a bare ampersand in text does not break parsing`() {
        assertEquals("Tom & Jerry", textOf("<r><t>Tom & Jerry</t></r>", "t"))
    }

    @Test
    fun `an external entity is stripped with the doctype and never resolved`() {
        val xml = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
            "<root><a>&xxe;</a></root>"
        // No exception, no file access: the DOCTYPE (and its entity) is gone.
        assertEquals("&xxe;", textOf(xml, "a"))
    }

    @Test
    fun `tag and attribute names are matched exactly`() {
        val doc = XmlDom.parse(
            "<package><metadata><dc:title>Der Titel</dc:title></metadata></package>"
        )
        assertEquals("Der Titel", doc.elements("dc:title").single().text())
        assertEquals("", doc.elements("title").firstOrNull()?.text().orEmpty())
    }
}

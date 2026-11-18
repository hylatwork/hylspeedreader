package eu.hylatwork.hylspeedreader.data.epub.portable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies the tolerant portable XML tree (FR-2.4 / NFR-2.2). */
class PortableXmlTest {

    @Test
    fun `decodes entities, attributes, CDATA and namespaced names`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
              <body>
                <p id="a" class='lead'>Caf&eacute; &amp; cr&#232;me <em>now</em></p>
                <nav epub:type="toc"><p>skip me</p></nav>
                <pre><![CDATA[raw <tag> & stuff]]></pre>
              </body>
            </html>
        """.trimIndent()

        val root = PortableXml.parse(xml)
        val body = root.descendant("body")!!
        val paragraph = body.descendant("p")!!
        assertEquals("a", paragraph.attr("id"))
        assertEquals("lead", paragraph.attr("class"))
        assertEquals("Café & crème now", paragraph.textContent())
        assertEquals("toc", body.descendant("nav")!!.attr("epub:type"))
        assertEquals("raw <tag> & stuff", body.descendant("pre")!!.textContent())
    }

    @Test
    fun `keeps unknown named entities as literal text`() {
        val root = PortableXml.parse("<p>&bogus; &nbsp; ok</p>")
        assertEquals("&bogus; \u00A0 ok", root.descendant("p")!!.textContent())
    }

    @Test
    fun `parses self-closing elements without consuming their siblings`() {
        val root = PortableXml.parse("<r a='1'/><r b='2'/><r/>")
        assertEquals(3, root.descendants("r").size)
        assertEquals("2", root.descendants("r")[1].attr("b"))
    }

    @Test
    fun `recovers from malformed markup instead of failing`() {
        val root = PortableXml.parse("<p>unclosed <b>bold")
        assertEquals("unclosed bold", root.descendant("p")!!.textContent())
        assertNull(root.descendant("missing"))
    }
}

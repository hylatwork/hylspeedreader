package eu.hylatwork.hylspeedreader.data.epub

import eu.hylatwork.hylspeedreader.domain.logic.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization ("golden") tests for chapter extraction: they pin the
 * observable behaviour that must be identical before and after replacing the
 * `jsoup`-based parser with the `javax.xml` DOM implementation. They were run
 * against the jsoup build first, then re-run unchanged after the migration.
 */
class EpubTextExtractionGoldenTest {

    @Test
    fun `a br inside a paragraph becomes a separating space`() {
        assertEquals(
            listOf("Hello World"),
            EpubTextExtractor.extractParagraphs("<html><body><p>Hello<br/>World</p></body></html>")
        )
    }

    @Test
    fun `mixed case block tags are recognised`() {
        assertEquals(
            listOf("Alpha", "Beta"),
            EpubTextExtractor.extractParagraphs("<HTML><BODY><P>Alpha</P><H2>Beta</H2></BODY></HTML>")
        )
    }

    @Test
    fun `a non block wrapper is descended into`() {
        assertEquals(
            listOf("Quoted"),
            EpubTextExtractor.extractParagraphs("<html><body><blockquote><p>Quoted</p></blockquote></body></html>")
        )
    }

    @Test
    fun `a non breaking space keeps the two words separated`() {
        // jsoup collapses &nbsp; to a plain space, a DOM parser keeps U+00A0;
        // both must still tokenize to two words, which is what the reader needs.
        val paragraph = EpubTextExtractor.extractParagraphs("<html><body><p>a&nbsp;b</p></body></html>").single()
        assertEquals(listOf("a", "b"), Tokenizer.tokenize(listOf(paragraph)).map { it.raw })
    }

    @Test
    fun `named and numeric entities are decoded`() {
        assertEquals(
            listOf("Tom & Jerry \u2014 ok"),
            EpubTextExtractor.extractParagraphs("<html><body><p>Tom &amp; Jerry &#8212; ok</p></body></html>")
        )
    }

    @Test
    fun `a doctype and typographic entities in xhtml are tolerated`() {
        val html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" " +
            "\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
            "<html><body><p>Caf&eacute; &mdash; fine.</p></body></html>"
        assertEquals(listOf("Caf\u00e9 \u2014 fine."), EpubTextExtractor.extractParagraphs(html))
    }

    @Test
    fun `malformed markup falls back to readable plain text`() {
        val paragraph = EpubTextExtractor.extractParagraphs("<html><body><p>unclosed <b>bold text</body>")
            .single()
        assertTrue("expected both words in '$paragraph'", paragraph.contains("unclosed"))
        assertTrue("expected both words in '$paragraph'", paragraph.contains("bold"))
    }
}

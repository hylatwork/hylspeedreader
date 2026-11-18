package eu.hylatwork.hylspeedreader.data.epub

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for the DOM block-element extraction (FR-2.2 / FR-2.3).
 * Run with: ./gradlew :app:testDebugUnitTest
 */
class EpubTextExtractorTest {

    @Test
    fun extractsBlockParagraphsInReadingOrder() {
        val html = """
            <html><body>
              <h1>Title</h1>
              <p>First paragraph.</p>
              <p>Second <em>paragraph</em>.</p>
            </body></html>
        """.trimIndent()

        assertEquals(
            listOf("Title", "First paragraph.", "Second paragraph."),
            EpubTextExtractor.extractParagraphs(html)
        )
    }

    @Test
    fun stripsScriptsStylesAndTableOfContentsNav() {
        val html = """
            <html><body>
              <nav epub:type="toc"><a href="#c1">Contents</a></nav>
              <script>var x = 1;</script>
              <style>p { color: red; }</style>
              <p>Real text.</p>
            </body></html>
        """.trimIndent()

        assertEquals(listOf("Real text."), EpubTextExtractor.extractParagraphs(html))
    }

    @Test
    fun nestedBlocksAreNotDuplicated() {
        val html = "<html><body><li><p>Item text</p></li></body></html>"
        assertEquals(listOf("Item text"), EpubTextExtractor.extractParagraphs(html))
    }

    @Test
    fun fallsBackToWholeBodyTextWhenNoBlocks() {
        val html = "<html><body><div>Just a div</div></body></html>"
        assertEquals(listOf("Just a div"), EpubTextExtractor.extractParagraphs(html))
    }

    @Test
    fun extractTitlePrefersFirstHeadingOverDocumentTitle() {
        val html = "<html><head><title>Doc title</title></head><body><h2>Heading</h2></body></html>"
        assertEquals("Heading", EpubTextExtractor.extractTitle(html, "Fallback"))
    }

    @Test
    fun extractTitleFallsBackWhenNoHeading() {
        val html = "<html><body><p>Body only</p></body></html>"
        assertEquals("Fallback", EpubTextExtractor.extractTitle(html, "Fallback"))
    }
}

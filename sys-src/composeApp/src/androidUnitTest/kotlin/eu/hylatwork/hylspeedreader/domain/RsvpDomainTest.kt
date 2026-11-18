package eu.hylatwork.hylspeedreader.domain

import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.domain.logic.Tokenizer
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM unit tests for the focus calculation, the timing heuristics and the
 * tokenizer. Run with: ./gradlew :app:testDebugUnitTest
 */
class RsvpDomainTest {

    // ---------------------------------------------------------------- focus

    @Test
    fun `focus index follows standard length buckets`() {
        assertEquals(0, RsvpWord.focusIndexFor(1))
        assertEquals(1, RsvpWord.focusIndexFor(2))
        assertEquals(1, RsvpWord.focusIndexFor(5))
        assertEquals(2, RsvpWord.focusIndexFor(6))
        assertEquals(2, RsvpWord.focusIndexFor(9))
        assertEquals(3, RsvpWord.focusIndexFor(10))
        assertEquals(3, RsvpWord.focusIndexFor(13))
        assertEquals(4, RsvpWord.focusIndexFor(14))
    }

    @Test
    fun `word is split at focus index`() {
        val w = RsvpWord.of("reading")!!
        assertEquals(2, w.focusIndex)
        assertEquals("re", w.prefix)
        assertEquals('a', w.focusChar)
        assertEquals("ding", w.suffix)
    }

    // ------------------------------------------------------------- timing

    @Test
    fun `base delay matches wpm`() {
        val w = RsvpWord.of("at")!!
        assertEquals(200L, RsvpTiming.delayMs(w, 300)) // 60_000 / 300
    }

    @Test
    fun `sentence end doubles dwell`() {
        val plain = RsvpWord.of("word")!!
        val ended = RsvpWord.of("word.")!!
        val base = RsvpTiming.delayMs(plain, 400)
        assertEquals(base * 2, RsvpTiming.delayMs(ended, 400))
    }

    @Test
    fun `clause break adds fifty percent`() {
        val plain = RsvpWord.of("word")!!
        val clause = RsvpWord.of("word,")!!
        val base = RsvpTiming.delayMs(plain, 400)
        assertEquals((base * 1.5).toLong(), RsvpTiming.delayMs(clause, 400))
    }

    @Test
    fun `wpm is clamped to supported range`() {
        val w = RsvpWord.of("hi")!!
        assertEquals(RsvpTiming.delayMs(w, 200), RsvpTiming.delayMs(w, 10))
        assertEquals(RsvpTiming.delayMs(w, 1000), RsvpTiming.delayMs(w, 5000))
    }

    @Test
    fun `long words get a surcharge`() {
        val short = RsvpWord.of("word")!!
        val long = RsvpWord.of("extraordinarily")!!
        val base = RsvpTiming.delayMs(short, 400)
        val expected = base + (long.raw.length - 9) * 25L
        assertEquals(expected, RsvpTiming.delayMs(long, 400))
    }

    // ----------------------------------------------------------- tokenizer

    @Test
    fun `tokenizer splits paragraphs and flags ends`() {
        val words = Tokenizer.tokenize(listOf("Hello brave new world.", "Second paragraph here"))
        assertEquals(7, words.size)
        assertEquals(false, words[0].endsParagraph)
        assertEquals(true, words[3].endsParagraph)
        assertEquals(true, words[6].endsParagraph)
    }

    @Test
    fun `em dash splits into pause tokens`() {
        val words = Tokenizer.tokenize(listOf("left—right"))
        assertEquals(listOf("left", "—", "right"), words.map { it.raw })
    }

    @Test
    fun `edge quotes are trimmed`() {
        val words = Tokenizer.tokenize(listOf("“quoted” word"))
        assertEquals("quoted", words[0].raw)
    }

    @Test
    fun `curly single quotes are trimmed`() {
        val words = Tokenizer.tokenize(listOf("‘quoted’ word"))
        assertEquals("quoted", words[0].raw)
    }

    @Test
    fun `non breaking space splits tokens`() {
        val words = Tokenizer.tokenize(listOf("foo\u00A0bar"))
        assertEquals(listOf("foo", "bar"), words.map { it.raw })
    }

    @Test
    fun `soft hyphen is stripped and does not shift the focus index`() {
        val withShy = Tokenizer.tokenize(listOf("co\u00ADoperate"))[0]
        val plain = Tokenizer.tokenize(listOf("cooperate"))[0]
        assertEquals("cooperate", withShy.raw)
        assertEquals(plain.focusIndex, withShy.focusIndex)
        assertEquals(plain.focusChar, withShy.focusChar)
    }

    // -------------------------------------------------- smart punctuation

    @Test
    fun `fullwidth full stop is preserved in the token`() {
        assertEquals("word\u3002", Tokenizer.tokenize(listOf("word\u3002")).single().raw)
    }

    @Test
    fun `curly quote marks are trimmed from token edges`() {
        assertEquals("word", Tokenizer.tokenize(listOf("said \u201cword\u201d")).last().raw)
    }

    @Test
    fun `interior guillemets are preserved, not rewritten`() {
        val token = Tokenizer.tokenize(listOf("a\u00bbb")).single()
        assertEquals("a\u00bbb", token.raw)
        assertEquals(3, token.raw.length)
    }

    @Test
    fun `a curly apostrophe survives mid-word`() {
        assertEquals("don\u2019t", Tokenizer.tokenize(listOf("don\u2019t")).single().raw)
    }

    @Test
    fun `smart punctuation is paced like its ascii equivalent`() {
        // Use a non-final token so the paragraph-pause surcharge is not applied.
        val smart = Tokenizer.tokenize(listOf("word\u3002 next")).first()
        val ascii = RsvpWord.of("word.")!!
        assertEquals(RsvpTiming.delayMs(ascii, 400), RsvpTiming.delayMs(smart, 400))
    }
}

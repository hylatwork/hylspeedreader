package eu.hylatwork.hylspeedreader.domain

import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Punctuation-pacing matrix for [RsvpTiming] (FR-5.1). The table is canonical
 * ASCII — the [eu.hylatwork.hylspeedreader.domain.logic.Tokenizer] normalizes smart
 * punctuation onto it — so each case below pins one canonical character.
 *
 * At 400 WPM the base dwell is 60_000 / 400 = 150 ms.
 */
class RsvpTimingTest {

    private fun delay(raw: String, wpm: Int = 400): Long =
        RsvpTiming.delayMs(RsvpWord.of(raw)!!, wpm)

    private val base = 150L

    @Test
    fun bareWordUsesTheBaseDwell() {
        assertEquals(base, delay("word"))
    }

    @Test
    fun sentenceEndersDoubleTheDwell() {
        for (mark in listOf(".", "!", "?", "\u2026")) {
            assertEquals("mark '$mark'", base * 2, delay("word$mark"))
        }
    }

    @Test
    fun clauseBreaksAddFiftyPercent() {
        for (mark in listOf(",", ";", ":", "\u2014", "\u2013", ")", "]", "}", "\"", "'")) {
            assertEquals("mark '$mark'", (base * 1.5).toLong(), delay("word$mark"))
        }
    }

    @Test
    fun runsOfPunctuationAreCappedAtTwoAndAHalfTimes() {
        // '!' caps at 1.0 then '?' pushes the punctuation total to the 1.5 cap.
        assertEquals((base * 2.5).toLong(), delay("word?!"))
    }

    @Test
    fun anEmDashOnItsOwnIsAClauseBreak() {
        assertEquals((base * 1.5).toLong(), delay("\u2014"))
    }

    @Test
    fun aSingleQuotedWordKeepsItsClosingQuotePause() {
        // Interior/kept closing quotes remain clause-paced (edge quotes are
        // neutralized by the tokenizer before they reach the timer).
        assertTrue(delay("word\"") > base)
        assertEquals((base * 1.5).toLong(), delay("word\""))
    }

    @Test
    fun smartPunctuationVariantsArePacedLikeAscii() {
        assertEquals(delay("word."), delay("word\u3002"))       // 。 fullwidth full stop
        assertEquals(delay("word!"), delay("word\uff01"))       // ！ fullwidth exclamation
        assertEquals(delay("word,"), delay("word\uff0c"))       // ， fullwidth comma
        assertEquals(delay("word,"), delay("word\u3001"))       // 、 ideographic comma
        assertEquals(delay("word\""), delay("word\u201d"))      // ” curly closing quote
        assertEquals(delay("word'"), delay("word\u2019"))       // ’ curly closing quote
    }
}

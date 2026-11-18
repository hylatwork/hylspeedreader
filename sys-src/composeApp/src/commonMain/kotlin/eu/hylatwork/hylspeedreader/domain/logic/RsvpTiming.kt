package eu.hylatwork.hylspeedreader.domain.logic

import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import kotlin.math.roundToLong

/**
 * Timing heuristics for the RSVP stream.
 *
 * The base dwell time for one word is `60_000 / WPM` milliseconds, modulated by:
 *  - trailing punctuation (sentence ends get a much longer dwell than clause breaks)
 *  - numerals           (digits are slower to recognize than letters)
 *  - word length        (a fixed surcharge per character above a threshold)
 *  - paragraph boundary (an extra breath at the end of a paragraph)
 *
 * Absolute surcharges are added *on top of* the base so they keep their effect
 * even at extreme speeds, while punctuation scales multiplicatively.
 *
 * The punctuation table is canonical ASCII; each character is mapped through
 * [Typography.canonical] as it is scanned, so smart quotes, guillemets and
 * CJK/fullwidth marks are paced identically without being rewritten on screen.
 */
object RsvpTiming {

    const val MIN_WPM = 200
    const val MAX_WPM = 1000
    const val DEFAULT_WPM = 350

    private const val SENTENCE_END_FACTOR = 1.0      // '.','!','?','…' -> 2x base dwell
    private const val CLAUSE_BREAK_FACTOR = 0.5      // ',',';',':','—',')',']','}'… -> 1.5x
    private const val MAX_PUNCTUATION_FACTOR = 1.5   // cap for runs like '?!'
    private const val NUMERAL_FACTOR = 0.25
    private const val LONG_WORD_THRESHOLD = 9
    private const val LONG_WORD_MS_PER_CHAR = 25.0
    private const val PARAGRAPH_BREAK_FACTOR = 0.5
    private const val MIN_DELAY_MS = 16L

    /** Returns how long [word] should stay on screen when reading at [wpm]. */
    fun delayMs(word: RsvpWord, wpm: Int): Long {
        val baseMs = 60_000.0 / wpm.coerceIn(MIN_WPM, MAX_WPM)
        var factor = 1.0

        // 1) Trailing punctuation. Scan from the end so constructs like 'word.' or
        //    'word).' accumulate their pause without unbounded growth.
        var punctuationFactor = 0.0
        for (i in word.raw.length - 1 downTo 0) {
            val extra = when (Typography.canonical(word.raw[i])) {
                '.', '!', '?', '…' -> SENTENCE_END_FACTOR
                ',', ';', ':', '—', '–', ')', ']', '}', '"', '\'' -> CLAUSE_BREAK_FACTOR
                else -> break
            }
            punctuationFactor += extra
            if (punctuationFactor >= MAX_PUNCTUATION_FACTOR) {
                punctuationFactor = MAX_PUNCTUATION_FACTOR
                break
            }
        }
        factor += punctuationFactor

        // 2) Numerals take measurably longer to recognize.
        if (word.raw.any { it.isDigit() }) factor += NUMERAL_FACTOR

        // 3) Long words: cognitive surcharge per character beyond the threshold.
        val longWordSurcharge =
            (word.raw.length - LONG_WORD_THRESHOLD).coerceAtLeast(0) * LONG_WORD_MS_PER_CHAR

        // 4) Paragraph boundary: an extra half-base breath, even if the paragraph
        //    ends without punctuation.
        val paragraphSurcharge = if (word.endsParagraph) baseMs * PARAGRAPH_BREAK_FACTOR else 0.0

        return (baseMs * factor + longWordSurcharge + paragraphSurcharge)
            .roundToLong()
            .coerceAtLeast(MIN_DELAY_MS)
    }
}

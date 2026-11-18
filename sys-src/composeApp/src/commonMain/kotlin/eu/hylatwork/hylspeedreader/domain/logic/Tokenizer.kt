package eu.hylatwork.hylspeedreader.domain.logic

import eu.hylatwork.hylspeedreader.domain.model.RsvpWord

/**
 * Turns clean paragraph text into an ordered list of RSVP-ready tokens:
 *  - splits on any whitespace, including Unicode spaces such as the
 *    non-breaking space (U+00A0), which Java's `\s` does not match
 *  - strips invisible soft hyphens (U+00AD) so they cannot inflate a word's
 *    length and shift its focus index
 *  - trims a *leading/trailing* quotation mark (straight or smart) from the token
 *    edges — a boundary marker, not a pause: it would widen the word and bias
 *    the focus alignment. Interior glyphs, including apostrophes in `don’t`, are
 *    left untouched so the book's typography is preserved on screen.
 *  - normalizes typewriter double-dashes to em dashes
 *  - splits words glued by em/en dashes into separate tokens, keeping the dash
 *    as its own "breathing pause" token
 *  - flags the final token of every paragraph so the timing engine adds a pause
 *
 * Punctuation *recognition* is not done here: [RsvpTiming] consults
 * [Typography.canonical] so smart variants are paced without being rewritten.
 */
object Tokenizer {

    /** ASCII whitespace plus the Unicode space separators and BOM. */
    private val whitespace = Regex("[\\s\\p{Zs}\\uFEFF]+")
    private val softHyphen = "\u00AD"

    /** Canonical forms considered quotation marks for edge trimming. */
    private val edgeQuoteCanonical = setOf('"', '\'')

    fun tokenize(paragraphs: List<String>): List<RsvpWord> {
        val words = ArrayList<RsvpWord>(paragraphs.sumOf { it.length / 6 + 4 })
        for (paragraph in paragraphs) {
            val pieces = ArrayList<String>()
            for (chunk in whitespace.split(paragraph)) {
                val token = trimEdgeQuotes(chunk.replace(softHyphen, "")).replace("--", "—")
                if (token.isEmpty()) continue
                pieces += splitOnClauseDashes(token)
            }
            if (pieces.isEmpty()) continue
            pieces.forEachIndexed { index, piece ->
                RsvpWord.of(piece, endsParagraph = index == pieces.lastIndex)?.let(words::add)
            }
        }
        return words
    }

    /** Trims quotation marks from both ends, treating smart variants as quotes. */
    private fun trimEdgeQuotes(text: String): String {
        var start = 0
        var end = text.length
        while (start < end && isEdgeQuote(text[start])) start++
        while (end > start && isEdgeQuote(text[end - 1])) end--
        return if (start == 0 && end == text.length) text else text.substring(start, end)
    }

    private fun isEdgeQuote(c: Char): Boolean = Typography.canonical(c) in edgeQuoteCanonical

    private fun splitOnClauseDashes(token: String): List<String> {
        if (!token.contains('—') && !token.contains('–')) return listOf(token)
        val pieces = ArrayList<String>(3)
        val current = StringBuilder()
        for (c in token) {
            if (c == '—' || c == '–') {
                if (current.isNotEmpty()) {
                    pieces += current.toString()
                    current.setLength(0)
                }
                pieces += c.toString()
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) pieces += current.toString()
        return pieces
    }
}

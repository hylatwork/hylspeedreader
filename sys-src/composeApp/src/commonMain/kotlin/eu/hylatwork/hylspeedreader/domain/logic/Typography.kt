package eu.hylatwork.hylspeedreader.domain.logic

/**
 * Typographic ("smart") punctuation, decoupled from rendering.
 *
 * Tokens keep the book's original glyphs on screen — `don’t` stays `don’t` and a
 * CJK `。` stays `。` — while the pacing heuristic consults [canonical] so every
 * variant behaves exactly like its ASCII counterpart. Every mapping is a single
 * BMP character to a single BMP character, so anything that only *reads*
 * characters (the focus index, display widths) is unaffected.
 */
object Typography {

    private val CANONICAL: Map<Char, Char> = mapOf(
        // Quotation marks -> straight quotes.
        '“' to '"', '”' to '"', '„' to '"', '‟' to '"', '«' to '"', '»' to '"', '＂' to '"',
        '‘' to '\'', '’' to '\'', '‚' to '\'', '‛' to '\'', '‹' to '\'', '›' to '\'', '＇' to '\'',
        // Sentence-ending marks.
        '。' to '.', '．' to '.', '！' to '!', '？' to '?', '﹒' to '.', '﹗' to '!', '﹖' to '?',
        '‥' to '…', '⋯' to '…',
        // Clause-separating marks.
        '，' to ',', '、' to ',', '；' to ';', '：' to ':', '﹐' to ',', '﹑' to ',', '﹔' to ';', '﹕' to ':'
    )

    /** The ASCII canonical form of [c], or [c] itself when it is already canonical. */
    fun canonical(c: Char): Char = CANONICAL[c] ?: c
}

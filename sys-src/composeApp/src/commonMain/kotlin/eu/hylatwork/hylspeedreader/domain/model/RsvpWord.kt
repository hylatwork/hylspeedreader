package eu.hylatwork.hylspeedreader.domain.model

/**
 * A single token prepared for Rapid Serial Visual Presentation (RSVP).
 *
 * The word is pre-split into its three render parts so the UI never has to
 * recompute the split per frame:
 *
 *  - [prefix]  – characters before the focus
 *  - [focusChar] – the focus character (highlighted + centered)
 *  - [suffix]  – characters after the focus
 *
 * Example: raw = "reading" -> focusIndex = 2 -> prefix "re", focusChar 'a', suffix "ding".
 */
data class RsvpWord(
    val raw: String,
    val focusIndex: Int,
    val prefix: String,
    val focusChar: Char,
    val suffix: String,
    val endsParagraph: Boolean
) {
    companion object {

        /**
         * Standard focus heuristic: the focus sits at roughly 30-35% of the word
         * length, rounded into fixed buckets. Longer words are capped so the
         * fixation point never drifts into the second half of the word.
         *
         *  length  1       -> char 0
         *  length  2..5    -> char 1
         *  length  6..9    -> char 2
         *  length 10..13   -> char 3
         *  length 14+      -> char 4
         */
        fun focusIndexFor(length: Int): Int = when {
            length <= 1 -> 0
            length <= 5 -> 1
            length <= 9 -> 2
            length <= 13 -> 3
            else -> 4
        }

        /** Builds a display-ready word, or null if [raw] is empty. */
        fun of(raw: String, endsParagraph: Boolean = false): RsvpWord? {
            if (raw.isEmpty()) return null
            val focus = focusIndexFor(raw.length).coerceIn(0, raw.lastIndex)
            return RsvpWord(
                raw = raw,
                focusIndex = focus,
                prefix = raw.substring(0, focus),
                focusChar = raw[focus],
                suffix = raw.substring(focus + 1),
                endsParagraph = endsParagraph
            )
        }
    }
}

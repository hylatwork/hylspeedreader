package eu.hylatwork.hylspeedreader.domain.model

/**
 * A restorable reading position: which book, where inside it, and the speed the
 * reader was using. Stored as plain values so it survives process death.
 *
 * The book is identified by the platform document URI string; re-opening it
 * after a restart relies on the persistable read permission taken at import
 * time, and degrades to "no book" if that permission is gone.
 */
data class ReadingPosition(
    val bookUri: String,
    val chapterIndex: Int,
    val wordIndex: Int,
    val wpm: Int
) {
    /**
     * Guards against a corrupt or stale store: a blank URI or negative indices
     * are dropped rather than restored.
     */
    fun validOrNull(): ReadingPosition? =
        if (bookUri.isBlank()) null
        else copy(
            chapterIndex = chapterIndex.coerceAtLeast(0),
            wordIndex = wordIndex.coerceAtLeast(0),
            wpm = wpm.coerceAtLeast(0)
        )
}

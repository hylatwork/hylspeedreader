package eu.hylatwork.hylspeedreader.data.epub

import eu.hylatwork.hylspeedreader.domain.model.Chapter
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord

/**
 * A parsed, still-open EPUB, expressed without any platform types so the UI and
 * playback layers can consume it on every target. Chapter token lists are
 * materialized lazily by the implementation (see the Android `EpubDocument`).
 */
interface EpubBook {

    val title: String

    val author: String

    val chapters: List<Chapter>

    /** Extracts, strips and tokenizes the chapter at [index]. */
    fun loadChapter(index: Int): List<RsvpWord>

    /** Releases the underlying archive and any temporary file. */
    fun close()
}

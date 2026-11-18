package eu.hylatwork.hylspeedreader.data.epub

import eu.hylatwork.hylspeedreader.domain.logic.Tokenizer
import eu.hylatwork.hylspeedreader.domain.model.Chapter
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * A parsed, still-open EPUB. Chapter token lists are materialized lazily, one
 * chapter at a time, and kept in a tiny LRU cache so hopping between chapters
 * is instant without ever holding a whole book in memory.
 */
class EpubDocument internal constructor(
    override val title: String,
    override val author: String,
    override val chapters: List<Chapter>,
    private val entryPaths: List<String>,
    private val zipFile: ZipFile,
    private val tempFile: File
) : EpubBook {

    private val chapterCache = LinkedHashMap<Int, List<RsvpWord>>()

    /** Extracts, strips and tokenizes the chapter at [index]. Thread-safe. */
    override fun loadChapter(index: Int): List<RsvpWord> {
        synchronized(chapterCache) {
            chapterCache.remove(index)?.let { cached ->
                chapterCache[index] = cached
                return cached
            }
        }

        val path = entryPaths.getOrNull(index) ?: throw IOException("Chapter $index has no entry")
        val html = zipFile.getEntry(path)?.let { entry ->
            zipFile.getInputStream(entry).use { EpubCharset.decode(it.readBytes()) }
        } ?: throw IOException("Missing EPUB entry '$path'")

        val words = Tokenizer.tokenize(EpubTextExtractor.extractParagraphs(html))
        synchronized(chapterCache) {
            chapterCache[index] = words
            while (chapterCache.size > MAX_CACHED_CHAPTERS) {
                chapterCache.remove(chapterCache.keys.first())
            }
        }
        return words
    }

    override fun close() {
        runCatching { zipFile.close() }
        tempFile.delete()
    }

    private companion object {
        const val MAX_CACHED_CHAPTERS = 3
    }
}

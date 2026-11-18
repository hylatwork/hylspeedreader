package eu.hylatwork.hylspeedreader.data.epub

import eu.hylatwork.hylspeedreader.domain.model.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Pure-JVM tests for the lazy chapter loader and its bounded LRU cache
 * (FR-2.1 / FR-2.3 / NFR-2.2).
 */
class EpubDocumentTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun writeZip(name: String, entries: Map<String, String>): File {
        val file = folder.newFile(name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return file
    }

    private fun document(file: File, entryPaths: List<String>): EpubDocument =
        EpubDocument(
            title = "Title",
            author = "Author",
            chapters = entryPaths.mapIndexed { index, _ -> Chapter(index, "Chapter ${index + 1}") },
            entryPaths = entryPaths,
            zipFile = ZipFile(file),
            tempFile = file
        )

    @Test
    fun loadsAndTokenizesChapterContent() {
        val file = writeZip(
            "book.epub",
            mapOf("ch1.xhtml" to "<html><body><p>Hello world.</p></body></html>")
        )
        val doc = document(file, listOf("ch1.xhtml"))
        try {
            assertEquals(listOf("Hello", "world."), doc.loadChapter(0).map { it.raw })
        } finally {
            doc.close()
        }
    }

    @Test
    fun repeatedLoadReturnsIdenticalTokens() {
        val file = writeZip(
            "repeat.epub",
            mapOf("ch1.xhtml" to "<p>One two three</p>")
        )
        val doc = document(file, listOf("ch1.xhtml"))
        try {
            val first = doc.loadChapter(0).map { it.raw }
            val second = doc.loadChapter(0).map { it.raw }
            assertEquals(first, second)
            assertEquals(listOf("One", "two", "three"), first)
        } finally {
            doc.close()
        }
    }

    @Test
    fun loadsMoreChaptersThanTheCacheHolds() {
        val entryPaths = (1..5).map { "ch$it.xhtml" }
        val entries = entryPaths.mapIndexed { i, path -> path to "<p>Word${i + 1}</p>" }.toMap()
        val file = writeZip("many.epub", entries)
        val doc = document(file, entryPaths)
        try {
            // Loading four distinct chapters evicts entries beyond MAX_CACHED_CHAPTERS
            // (3); every chapter must still resolve to its own text.
            for (index in entryPaths.indices) {
                assertEquals(listOf("Word${index + 1}"), doc.loadChapter(index).map { it.raw })
            }
            assertEquals(listOf("Word1"), doc.loadChapter(0).map { it.raw })
        } finally {
            doc.close()
        }
    }

    @Test
    fun missingEntryThrowsIoException() {
        val file = writeZip("sparse.epub", mapOf("present.xhtml" to "<p>x</p>"))
        val doc = document(file, listOf("absent.xhtml"))
        try {
            assertThrows(IOException::class.java) { doc.loadChapter(0) }
        } finally {
            doc.close()
        }
    }

    @Test
    fun closeDeletesTheTempFile() {
        val file = writeZip("gone.epub", mapOf("ch1.xhtml" to "<p>text</p>"))
        val doc = document(file, listOf("ch1.xhtml"))
        assertTrue(file.exists())
        doc.close()
        assertFalse(file.exists())
    }
}

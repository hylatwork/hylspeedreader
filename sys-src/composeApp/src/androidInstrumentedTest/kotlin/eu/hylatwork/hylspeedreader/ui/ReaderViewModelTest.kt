package eu.hylatwork.hylspeedreader.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.hylatwork.hylspeedreader.data.Platform
import eu.hylatwork.hylspeedreader.data.epub.AndroidDocumentHandle
import eu.hylatwork.hylspeedreader.data.position.SettingsReadingPositionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Instrumented end-to-end checks for the ViewModel state machine (FR-1, FR-2,
 * FR-4, FR-7): a valid EPUB imports and loads its first chapter, a chapter
 * switch loads the new chapter, a saved position is restored on construction,
 * and a non-EPUB fails with a user-visible error while always leaving
 * `isLoading` behind.
 */
class ReaderViewModelTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun clearSavedPosition() {
        Platform.init(app)
        SettingsReadingPositionStore(Platform.settings()).clear()
    }

    private fun newViewModel(): ReaderViewModel =
        ReaderViewModel(Platform.epubEngine(), SettingsReadingPositionStore(Platform.settings()))

    private fun writeEpub(name: String, entries: Map<String, String>): AndroidDocumentHandle {
        val file = File(app.cacheDir, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((path, content) in entries) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return AndroidDocumentHandle(Uri.fromFile(file))
    }

    private fun epub(name: String, vararg chapters: Pair<String, String>): AndroidDocumentHandle {
        val manifest = StringBuilder()
        val spine = StringBuilder()
        val entries = linkedMapOf<String, String>()
        entries["META-INF/container.xml"] =
            """<?xml version="1.0"?><container><rootfiles>""" +
                """<rootfile full-path="OEBPS/content.opf"/></rootfiles></container>"""
        chapters.forEachIndexed { index, (text, href) ->
            val id = "c${index + 1}"
            manifest.append("""<item id="$id" href="$href" media-type="application/xhtml+xml"/>""")
            spine.append("""<itemref idref="$id"/>""")
            entries["OEBPS/$href"] = "<html><body><p>$text</p></body></html>"
        }
        entries["OEBPS/content.opf"] =
            """<?xml version="1.0"?><package><metadata>""" +
                """<dc:title>Title</dc:title><dc:creator>Author</dc:creator></metadata>""" +
                """<manifest>$manifest</manifest><spine>$spine</spine></package>"""
        return writeEpub(name, entries)
    }

    @Test
    fun importsEpubAndLoadsFirstChapter() = runBlocking {
        val vm = newViewModel()
        vm.openEpub(epub("vm-one-${System.nanoTime()}.epub", "Hello world." to "ch1.xhtml"))

        val state = withTimeout(10_000) {
            vm.uiState.first { !it.isLoading && (it.hasBook || it.error != null) }
        }

        assertNull(state.error)
        assertEquals("Title", state.bookTitle)
        assertEquals("Author", state.author)
        assertTrue(state.hasBook)
        assertEquals(1, state.chapters.size)
        assertEquals(2, state.chapterWordCount)
        assertEquals("Hello", state.currentWord?.raw)
        assertEquals(PlaybackStatus.Paused, state.status)
    }

    @Test
    fun selectingAChapterLoadsIt() = runBlocking {
        val vm = newViewModel()
        vm.openEpub(
            epub(
                "vm-two-${System.nanoTime()}.epub",
                "First" to "ch1.xhtml",
                "Second" to "ch2.xhtml"
            )
        )
        withTimeout(10_000) { vm.uiState.first { it.hasBook && !it.isLoading } }

        vm.selectChapter(1)
        val state = withTimeout(10_000) {
            vm.uiState.first { !it.isLoading && it.currentChapterIndex == 1 }
        }
        assertEquals("Second", state.currentWord?.raw)
    }

    @Test
    fun reportsErrorForNonEpubFile() = runBlocking {
        val file = File(app.cacheDir, "not-an-epub-${System.nanoTime()}.txt")
        file.writeText("this is definitely not a zip archive")

        val vm = newViewModel()
        vm.openEpub(AndroidDocumentHandle(Uri.fromFile(file)))

        val state = withTimeout(10_000) { vm.uiState.first { !it.isLoading && it.error != null } }
        assertNotNull(state.error)
        assertFalse(state.hasBook)
    }

    @Test
    fun restoresSavedPositionOnConstruction() = runBlocking {
        val handle = epub(
            "vm-restore-${System.nanoTime()}.epub",
            "First" to "ch1.xhtml",
            "Second" to "ch2.xhtml"
        )

        val first = newViewModel()
        first.openEpub(handle)
        withTimeout(10_000) { first.uiState.first { it.hasBook && !it.isLoading } }
        first.selectChapter(1)
        withTimeout(10_000) { first.uiState.first { it.currentChapterIndex == 1 && !it.isLoading } }
        first.pause() // makes sure the position is flushed

        // A fresh instance restores the saved chapter/word from the store.
        val second = newViewModel()
        val restored = withTimeout(10_000) { second.uiState.first { it.hasBook && !it.isLoading } }
        assertEquals(1, restored.currentChapterIndex)
        assertEquals("Second", restored.currentWord?.raw)
    }
}

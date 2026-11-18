package eu.hylatwork.hylspeedreader.data.epub

import android.content.Context
import android.net.Uri
import eu.hylatwork.hylspeedreader.domain.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Lightweight EPUB 2/3 reader built on java.util.zip + the platform XML DOM —
 * no external
 * EPUB library required:
 *
 *   SAF Uri -> cached file copy -> ZipFile
 *     -> META-INF/container.xml -> rootfile (content.opf)
 *     -> <manifest> + <spine>   -> ordered XHTML chapters
 *
 * The SAF stream is copied to cache first because ZipFile needs a seekable
 * file; SAF descriptors only expose a forward stream.
 */
class EpubParser(context: Context) {

    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver

    /**
     * Copies the picked document into the app cache and parses its structure (IO).
     *
     * Runs [NonCancellable]: the blocking archive read cannot be interrupted
     * mid-way, and the returned [EpubDocument] owns an open [ZipFile] plus a temp
     * file. If `withContext` were cancellable it could discard that result and
     * leak both, so a superseded import instead completes and discards its
     * document via `EpubDocument.close()` in the caller.
     */
    suspend fun parse(uri: Uri): EpubDocument = withContext(NonCancellable + Dispatchers.IO) {
        val tempFile = copyToCache(uri)
        try {
            val zip = ZipFile(tempFile)
            try {
                val opfPath = findOpfPath(zip)
                val opfXml = readEntry(zip, opfPath)
                    ?: throw IOException("OPF document not found at '$opfPath'")
                val opf = XmlDom.parse(opfXml)

                val title = opf.elements("dc:title").firstOrNull()?.text().orEmpty()
                val author = opf.elements("dc:creator").firstOrNull()?.text().orEmpty()
                val opfDir = opfPath.substringBeforeLast('/', "")

                val entryPaths = EpubSpine.resolve(opfXml, opfDir)
                if (entryPaths.isEmpty()) throw IOException("EPUB spine contains no readable documents")

                val chapters = entryPaths.mapIndexed { index, path ->
                    val fallback = "Chapter ${index + 1}"
                    val html = readEntry(zip, path)
                    Chapter(
                        index = index,
                        title = html
                            ?.let { runCatching { EpubTextExtractor.extractTitle(it, fallback) }.getOrNull() }
                            ?: fallback
                    )
                }

                EpubDocument(
                    title = title.ifEmpty { "Untitled" },
                    author = author,
                    chapters = chapters,
                    entryPaths = entryPaths,
                    zipFile = zip,
                    tempFile = tempFile
                )
            } catch (t: Throwable) {
                runCatching { zip.close() }
                tempFile.delete()
                throw t
            }
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }

    private fun copyToCache(uri: Uri): File {
        val dir = File(appContext.cacheDir, "epubs").apply { mkdirs() }
        pruneStaleCopies(dir)
        val file = File(dir, "book-${System.currentTimeMillis()}.zip")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("The selected document could not be opened")
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        return file
    }

    /**
     * Deletes leftover book copies from an earlier session. [EpubDocument.close]
     * already removes its own temp file, so anything still here is the residue of
     * a process kill or crash and would otherwise accumulate in the cache.
     */
    private fun pruneStaleCopies(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("book-") && file.name.endsWith(".zip")) {
                runCatching { file.delete() }
            }
        }
    }

    private fun findOpfPath(zip: ZipFile): String {
        val container = readEntry(zip, "META-INF/container.xml")
            ?: throw IOException("META-INF/container.xml missing — not a valid EPUB")
        val doc = XmlDom.parse(container)
        return doc.elements("rootfile").firstOrNull()
            ?.attrOrEmpty("full-path")
            ?.takeIf { it.isNotEmpty() }
            ?.removePrefix("/")
            ?: throw IOException("Malformed EPUB: container has no <rootfile full-path=…>")
    }

    private fun readEntry(zip: ZipFile, name: String): String? {
        val entry = zip.getEntry(name) ?: findEntryIgnoreCase(zip, name) ?: return null
        return zip.getInputStream(entry).use { EpubCharset.decode(it.readBytes()) }
    }

    /** Some producers write entry names with unexpected casing; tolerate that. */
    private fun findEntryIgnoreCase(zip: ZipFile, name: String): ZipEntry? {
        val target = name.lowercase()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.lowercase() == target) return entry
        }
        return null
    }
}

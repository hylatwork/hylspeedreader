package eu.hylatwork.hylspeedreader.data.epub.portable

import eu.hylatwork.hylspeedreader.data.epub.EpubBook
import eu.hylatwork.hylspeedreader.domain.logic.Tokenizer
import eu.hylatwork.hylspeedreader.domain.model.Chapter
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord

/**
 * A parsed, in-memory EPUB. Chapter token lists are materialized lazily and kept
 * in a tiny LRU cache, mirroring the Android `EpubDocument` so hopping between
 * chapters is instant without holding a whole book.
 */
internal class PortableEpub(
    override val title: String,
    override val author: String,
    override val chapters: List<Chapter>,
    private val entryPaths: List<String>,
    private val zip: PortableZip
) : EpubBook {

    private val chapterCache = LinkedHashMap<Int, List<RsvpWord>>()

    override fun loadChapter(index: Int): List<RsvpWord> {
        synchronized(chapterCache) {
            chapterCache.remove(index)?.let { cached ->
                chapterCache[index] = cached
                return cached
            }
        }

        val path = entryPaths.getOrNull(index)
            ?: throw EpubFormatException("Chapter $index has no entry")
        val html = zip.read(path)?.let(PortableCharset::decode)
            ?: throw EpubFormatException("Missing EPUB entry '$path'")
        val words = Tokenizer.tokenize(PortableTextExtractor.extractParagraphs(html))

        synchronized(chapterCache) {
            chapterCache[index] = words
            while (chapterCache.size > MAX_CACHED_CHAPTERS) {
                chapterCache.remove(chapterCache.keys.first())
            }
        }
        return words
    }

    override fun close() {
        // Everything lives in memory; the caller-owned byte array is reused.
    }

    private companion object {
        const val MAX_CACHED_CHAPTERS = 3
    }
}

/**
 * Parses an EPUB byte array into a [PortableEpub]: `META-INF/container.xml` →
 * root OPF package → ordered spine entries, delegating spine rules to
 * [resolveSpine] (the portable twin of the Android `EpubSpine`).
 */
internal object PortableEpubParser {

    private val DOCUMENT_TYPES = setOf("application/xhtml+xml", "text/html")

    fun parse(bytes: ByteArray): EpubBook {
        val zip = PortableZip(bytes)

        val container = zip.read("META-INF/container.xml")?.let(PortableCharset::decode)
            ?: throw EpubFormatException("META-INF/container.xml missing — not a valid EPUB")
        val opfPath = PortableXml.parse(container).descendant("rootfile")
            ?.attr("full-path")
            ?.removePrefix("/")
            ?.takeIf { it.isNotEmpty() }
            ?: throw EpubFormatException("Malformed EPUB: container has no <rootfile full-path>")

        val opfXml = zip.read(opfPath)?.let(PortableCharset::decode)
            ?: throw EpubFormatException("OPF document not found at '$opfPath'")
        val opf = PortableXml.parse(opfXml)

        val title = opf.descendant("dc:title")?.textContent().orEmpty().ifEmpty { "Untitled" }
        val author = opf.descendant("dc:creator")?.textContent().orEmpty()
        val opfDir = opfPath.substringBeforeLast('/', "")

        val entryPaths = resolveSpine(opf, opfDir)
        if (entryPaths.isEmpty()) throw EpubFormatException("EPUB spine contains no readable documents")

        val chapters = entryPaths.mapIndexed { index, path ->
            val fallback = "Chapter ${index + 1}"
            val html = zip.read(path)?.let(PortableCharset::decode)
            Chapter(
                index = index,
                title = html
                    ?.let { runCatching { PortableTextExtractor.extractTitle(it, fallback) }.getOrDefault(fallback) }
                    ?: fallback
            )
        }

        return PortableEpub(title, author, chapters, entryPaths, zip)
    }

    private fun resolveSpine(opf: XmlNode, opfDir: String): List<String> {
        val manifest = HashMap<String, Pair<String, String>>()
        val properties = HashMap<String, String>()
        for (item in opf.descendants("item")) {
            val id = item.attr("id")
            val href = item.attr("href")
            if (id.isEmpty() || href.isEmpty()) continue
            manifest[id] = href to item.attr("media-type")
            properties[id] = item.attr("properties")
        }

        val result = ArrayList<String>()
        for (itemref in opf.descendants("itemref")) {
            if (itemref.attr("linear").equals("no", ignoreCase = true)) continue
            val idref = itemref.attr("idref")
            if (idref.isEmpty()) continue
            val (href, mediaType) = manifest[idref] ?: continue
            if (mediaType !in DOCUMENT_TYPES) continue
            if (isNavigation(properties[idref].orEmpty())) continue
            result += resolvePath(opfDir, href)
        }
        return result
    }

    private fun isNavigation(properties: String): Boolean =
        properties.split(' ', '\t', '\r', '\n').any { it == "nav" }

    /** Resolves a manifest href relative to the OPF directory ('../' aware, URI-decoded). */
    private fun resolvePath(baseDir: String, href: String): String {
        val decoded = percentDecode(stripQueryAndFragment(href))
        val combined = if (decoded.startsWith("/")) decoded else "$baseDir/$decoded"
        val segments = ArrayList<String>()
        for (part in combined.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments.add(part)
            }
        }
        return segments.joinToString("/")
    }

    private fun stripQueryAndFragment(href: String): String {
        val query = href.indexOf('?')
        val hash = href.indexOf('#')
        val cut = listOf(query, hash).filter { it >= 0 }.minOrNull() ?: return href
        return href.substring(0, cut)
    }

    private fun percentDecode(value: String): String {
        if ('%' !in value) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 < value.length) {
                val code = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (code != null) {
                    out.append(code.toChar())
                    i += 3
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}

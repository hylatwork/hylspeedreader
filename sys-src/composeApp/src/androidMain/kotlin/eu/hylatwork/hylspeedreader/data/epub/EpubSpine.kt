package eu.hylatwork.hylspeedreader.data.epub

import java.net.URI

/**
 * Pure (Android-free) resolution of an EPUB package document into the ordered
 * list of readable spine entry paths. Keeping it separate from [EpubParser]
 * makes the spine rules — `linear="no"`, the EPUB 3 navigation document, media
 * types, and relative `../` hrefs — unit-testable on the JVM.
 */
internal object EpubSpine {

    private val DOCUMENT_TYPES = setOf("application/xhtml+xml", "text/html")

    fun resolve(opfXml: String, opfDir: String): List<String> {
        val opf = XmlDom.parse(opfXml)

        val manifest = opf.elements("item").mapNotNull { el ->
            val id = el.attrOrEmpty("id")
            val href = el.attrOrEmpty("href")
            if (id.isEmpty() || href.isEmpty()) null
            else id to ManifestItem(href, el.attrOrEmpty("media-type"), el.attrOrEmpty("properties"))
        }.toMap()

        return opf.elements("itemref")
            .asSequence()
            .filter { !it.attrOrEmpty("linear").equals("no", ignoreCase = true) }
            .map { it.attrOrEmpty("idref") }
            .filter { it.isNotEmpty() }
            .mapNotNull { manifest[it] }
            .filter { it.mediaType in DOCUMENT_TYPES }
            .filterNot { it.isNavigation }
            .map { resolvePath(opfDir, it.href) }
            .toList()
    }

    /** Resolves a manifest href relative to the OPF directory ('../' aware, URI-decoded). */
    fun resolvePath(baseDir: String, href: String): String {
        val decoded = decode(href)
        val combined = if (decoded.startsWith("/")) decoded else "$baseDir/$decoded"
        val segments = ArrayDeque<String>()
        for (part in combined.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeLast()
                else -> segments.addLast(part)
            }
        }
        return segments.joinToString("/")
    }

    /**
     * Percent-decodes [href] without treating `+` as a space (unlike
     * `URLDecoder`), falling back to the raw value for strings [URI] cannot
     * parse (e.g. a bare space in a href).
     */
    private fun decode(href: String): String =
        runCatching { URI(href).path }.getOrNull()?.takeIf { it.isNotEmpty() } ?: href

    private data class ManifestItem(val href: String, val mediaType: String, val properties: String) {
        /** True for the EPUB 3 navigation document (`properties="nav"`). */
        val isNavigation: Boolean
            get() = properties.split(' ', '\t', '\r', '\n').any { it == "nav" }
    }
}

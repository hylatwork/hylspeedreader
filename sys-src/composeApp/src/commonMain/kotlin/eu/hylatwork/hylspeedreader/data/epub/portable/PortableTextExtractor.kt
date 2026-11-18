package eu.hylatwork.hylspeedreader.data.epub.portable

/**
 * Strips EPUB XHTML down to ordered plain-text paragraphs using [PortableXml].
 *
 * Mirrors the Android `EpubTextExtractor`: it collects the outermost
 * block-level content elements (so a `<p>` inside an `<li>` is not emitted
 * twice), skips script/style/nav subtrees while gathering text, falls back to
 * the whole body text for documents built from bare `<div>`s, and finally to a
 * tolerant tag-stripper so malformed content degrades to plain text rather than
 * failing (FR-2.3 / NFR-2.2).
 */
internal object PortableTextExtractor {

    private val NOISE = setOf("script", "style", "iframe", "svg", "image", "audio", "video")
    private val BLOCKS =
        setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "dd", "dt", "td", "figcaption")

    private val WHITESPACE = Regex("\\s+")
    private val TAG = Regex("(?s)<[^>]+>")
    private val NOISE_ELEMENT = Regex("(?is)<(script|style)[^>]*>.*?</\\1\\s*>")

    /** Returns the readable paragraphs of one chapter in reading order. */
    fun extractParagraphs(html: String): List<String> {
        val root = runCatching { bodyOf(html) }.getOrNull() ?: return stripTagsFallback(html)

        val blocks = ArrayList<XmlNode>()
        collectOutermostBlocks(root, blocks)
        val paragraphs = blocks.map(::textOf).filter { it.isNotEmpty() }
        if (paragraphs.isNotEmpty()) return paragraphs

        val whole = textOf(root)
        return if (whole.isEmpty()) emptyList() else listOf(whole)
    }

    /** Best-effort chapter title: first heading, then document title, then fallback. */
    fun extractTitle(html: String, fallback: String): String {
        val root = runCatching { PortableXml.parse(html) }.getOrNull() ?: return fallback
        for (tag in listOf("h1", "h2", "h3")) {
            val text = textOf(root.descendant(tag))
            if (text.isNotEmpty()) return text.take(80)
        }
        return textOf(root.descendant("title")).takeIf { it.isNotEmpty() } ?: fallback
    }

    private fun bodyOf(html: String): XmlNode {
        val root = PortableXml.parse(html)
        return root.descendant("body")
            ?: root.descendant("html")
            ?: root.children.firstOrNull { !it.isText }
            ?: root
    }

    private fun collectOutermostBlocks(node: XmlNode, out: MutableList<XmlNode>) {
        for (child in node.children) {
            if (child.isText) continue
            when {
                child.name.lowercase() in NOISE || isTableOfContents(child) -> Unit
                child.name.lowercase() in BLOCKS -> out += child
                else -> collectOutermostBlocks(child, out)
            }
        }
    }

    private fun isTableOfContents(element: XmlNode): Boolean {
        if (element.name.lowercase() != "nav") return false
        val type = element.attr("epub:type") + " " + element.attr("type")
        return type.contains("toc", ignoreCase = true) || type.contains("landmarks", ignoreCase = true)
    }

    /** Whitespace-normalised text; noise skipped, `<br>` and block edges add a space. */
    private fun textOf(element: XmlNode?): String {
        if (element == null) return ""
        val text = StringBuilder()
        fun walk(node: XmlNode) {
            for (child in node.children) {
                if (child.isText) {
                    text.append(child.text)
                    continue
                }
                val name = child.name.lowercase()
                if (name in NOISE || isTableOfContents(child)) continue
                if (name == "br" || name in BLOCKS) text.append(' ')
                walk(child)
                if (name in BLOCKS) text.append(' ')
            }
        }
        walk(element)
        return text.toString().replace(WHITESPACE, " ").trim()
    }

    private fun stripTagsFallback(html: String): List<String> =
        html.replace(NOISE_ELEMENT, " ")
            .replace(TAG, " ")
            .replace(WHITESPACE, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let(::listOf)
            ?: emptyList()
}

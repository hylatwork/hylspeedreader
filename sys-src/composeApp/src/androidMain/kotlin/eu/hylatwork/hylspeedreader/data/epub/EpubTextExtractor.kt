package eu.hylatwork.hylspeedreader.data.epub

import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Strips EPUB XHTML down to ordered plain-text paragraphs using only the
 * platform XML DOM (no jsoup).
 *
 * Strategy: walk the body, collect the outermost block-level content elements
 * (so a `<p>` inside an `<li>` is not emitted twice), skip script/style/nav
 * subtrees while gathering text, and fall back to the whole body text for
 * documents that use bare `<div>`s. Anything the strict XML parser rejects is
 * handled by a tolerant tag-stripper, so malformed content degrades to plain
 * text instead of failing (NFR-2.2).
 */
internal object EpubTextExtractor {

    private val NOISE = setOf("script", "style", "iframe", "svg", "image", "audio", "video")
    private val BLOCKS =
        setOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "pre", "dd", "dt", "td", "figcaption")

    private val WHITESPACE = Regex("\\s+")
    private val TAG = Regex("(?s)<[^>]+>")
    private val NOISE_ELEMENT = Regex("(?is)<(script|style)[^>]*>.*?</\\1\\s*>")

    /** Returns the readable paragraphs of one chapter in reading order. */
    fun extractParagraphs(html: String): List<String> {
        val root = runCatching {
            val document = XmlDom.parse(html)
            document.elements("body").firstOrNull() ?: document.documentElement
        }.getOrNull() ?: return stripTagsFallback(html)

        val blocks = ArrayList<Element>()
        collectOutermostBlocks(root, blocks)
        val paragraphs = blocks.map(::textOf).filter { it.isNotEmpty() }
        if (paragraphs.isNotEmpty()) return paragraphs

        val whole = textOf(root)
        return if (whole.isEmpty()) emptyList() else listOf(whole)
    }

    /** Best-effort chapter title: first heading, then document title, then fallback. */
    fun extractTitle(html: String, fallback: String): String {
        val document = runCatching { XmlDom.parse(html) }.getOrNull() ?: return fallback
        for (tag in listOf("h1", "h2", "h3")) {
            val text = textOf(document.elements(tag).firstOrNull())
            if (text.isNotEmpty()) return text.take(80)
        }
        return textOf(document.elements("title").firstOrNull())
            .takeIf { it.isNotEmpty() }
            ?: fallback
    }

    private fun collectOutermostBlocks(node: Node, out: MutableList<Element>) {
        for (child in node.childNodes.asIterable()) {
            if (child !is Element) continue
            val name = child.nodeName.lowercase()
            when {
                name in NOISE || isTableOfContents(child) -> Unit // skip subtree
                name in BLOCKS -> out += child                    // outermost only
                else -> collectOutermostBlocks(child, out)
            }
        }
    }

    private fun isTableOfContents(element: Element): Boolean {
        if (element.nodeName.lowercase() != "nav") return false
        val type = element.attrOrEmpty("epub:type") + " " + element.attrOrEmpty("type")
        return type.contains("toc", ignoreCase = true) || type.contains("landmarks", ignoreCase = true)
    }

    /** Whitespace-normalised text; noise skipped, `<br>` and block edges add a space. */
    private fun textOf(element: Element?): String {
        if (element == null) return ""
        val text = StringBuilder()
        fun walk(node: Node) {
            for (child in node.childNodes.asIterable()) {
                when (child.nodeType) {
                    Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(child.nodeValue)
                    Node.ENTITY_REFERENCE_NODE -> text.append(child.nodeValue ?: "")
                    Node.ELEMENT_NODE -> {
                        val element = child as Element
                        val name = element.nodeName.lowercase()
                        if (name in NOISE || isTableOfContents(element)) continue
                        if (name == "br" || name in BLOCKS) text.append(' ')
                        walk(element)
                        if (name in BLOCKS) text.append(' ')
                    }
                }
            }
        }
        walk(element)
        return text.toString().replace(WHITESPACE, " ").trim()
    }

    /** Fallback for markup the strict XML parser rejects: keep the words, drop the tags. */
    private fun stripTagsFallback(html: String): List<String> =
        html.replace(NOISE_ELEMENT, " ")
            .replace(TAG, " ")
            .replace(WHITESPACE, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let(::listOf)
            ?: emptyList()
}

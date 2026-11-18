package eu.hylatwork.hylspeedreader.data.epub.portable

import eu.hylatwork.hylspeedreader.data.epub.XmlEntities

/**
 * Tiny, tolerant, namespace-agnostic XML tree used by the portable EPUB codec.
 *
 * It mirrors the behaviour of the Android `XmlDom` closely enough for EPUB
 * work — the DOCTYPE/declaration is dropped (so no external DTD is ever needed
 * and there is no XXE surface), named HTML entities are decoded through the
 * shared [XmlEntities] table, numeric and predefined references are resolved,
 * CDATA is kept as text, and tags are matched literally (so `dc:title`,
 * `itemref`, `epub:type` all work). Anything that cannot be well-formed is
 * recovered on a best-effort basis rather than failing the import (NFR-2.2).
 */
internal class XmlNode(val name: String) {

    val attributes = LinkedHashMap<String, String>()
    val children = ArrayList<XmlNode>()

    /** Text content for `#text` / `#cdata` nodes; empty for elements. */
    var text: String = ""

    val isText: Boolean get() = name == TEXT_NODE || name == CDATA_NODE

    fun attr(name: String): String = attributes[name] ?: ""

    /** Whitespace-normalised concatenated text of this subtree (element `textContent`). */
    fun textContent(): String {
        val builder = StringBuilder()
        fun walk(node: XmlNode) {
            for (child in node.children) {
                if (child.isText) builder.append(child.text) else walk(child)
            }
        }
        walk(this)
        return builder.toString().replace(WHITESPACE, " ").trim()
    }

    /** Direct children with the given literal tag name, in document order. */
    fun elements(tag: String): List<XmlNode> = children.filter { it.name == tag }

    /** First descendant (depth-first, document order) with the given tag name. */
    fun descendant(tag: String): XmlNode? {
        for (child in children) {
            if (child.isText) continue
            if (child.name == tag) return child
            child.descendant(tag)?.let { return it }
        }
        return null
    }

    /** All descendants (depth-first, document order) with the given tag name. */
    fun descendants(tag: String): List<XmlNode> {
        val out = ArrayList<XmlNode>()
        fun walk(node: XmlNode) {
            for (child in node.children) {
                if (child.isText) continue
                if (child.name == tag) out += child
                walk(child)
            }
        }
        walk(this)
        return out
    }

    companion object {
        const val TEXT_NODE = "#text"
        const val CDATA_NODE = "#cdata"

        private val WHITESPACE = Regex("\\s+")
    }
}

internal object PortableXml {

    fun parse(markup: String): XmlNode {
        val prepared = XmlEntities.decode(stripProlog(markup))
        val root = XmlNode("#root")
        val stack = ArrayDeque<XmlNode>()
        stack.addLast(root)

        var i = 0
        val length = prepared.length
        while (i < length) {
            if (prepared[i] != '<') {
                var j = i
                while (j < length && prepared[j] != '<') j++
                appendText(stack.last(), prepared.substring(i, j))
                i = j
                continue
            }

            when {
                prepared.startsWith("<!--", i) -> {
                    val end = prepared.indexOf("-->", i)
                    i = if (end < 0) length else end + 3
                }

                prepared.startsWith("<![CDATA[", i) -> {
                    val contentStart = i + 9
                    val end = prepared.indexOf("]]>", contentStart)
                    val stop = if (end < 0) length else end
                    appendCdata(stack.last(), prepared.substring(contentStart, stop))
                    i = if (end < 0) length else end + 3
                }

                prepared.startsWith("<?", i) -> {
                    val end = prepared.indexOf("?>", i)
                    i = if (end < 0) length else end + 2
                }

                prepared.startsWith("<!", i) -> {
                    val end = prepared.indexOf('>', i)
                    i = if (end < 0) length else end + 1
                }

                i + 1 < length && prepared[i + 1] == '/' -> {
                    val end = prepared.indexOf('>', i)
                    if (end < 0) break
                    if (stack.size > 1) stack.removeLast()
                    i = end + 1
                }

                else -> {
                    val tag = findTagEnd(prepared, i + 1)
                    val (name, attrs) = parseTag(prepared.substring(i + 1, tag.start))
                    if (name.isNotEmpty()) {
                        val node = XmlNode(name)
                        node.attributes.putAll(attrs)
                        stack.last().children.add(node)
                        if (!tag.selfClosing) stack.addLast(node)
                    }
                    i = tag.end
                }
            }
        }
        return root
    }

    private fun appendText(parent: XmlNode, raw: String) {
        val decoded = decodeEntities(raw)
        if (decoded.isEmpty()) return
        val node = XmlNode(XmlNode.TEXT_NODE)
        node.text = decoded
        parent.children.add(node)
    }

    private fun appendCdata(parent: XmlNode, raw: String) {
        val node = XmlNode(XmlNode.CDATA_NODE)
        node.text = raw
        parent.children.add(node)
    }

    /** Drops the XML declaration, processing instructions, comments and DOCTYPE. */
    private fun stripProlog(markup: String): String {
        val start = if (markup.startsWith("\uFEFF")) 1 else 0
        val builder = StringBuilder(markup.length)
        var i = start
        while (i < markup.length) {
            when {
                markup.startsWith("<?", i) -> {
                    val end = markup.indexOf("?>", i)
                    i = if (end < 0) markup.length else end + 2
                }
                markup.startsWith("<!DOCTYPE", i, ignoreCase = true) -> i = skipDoctype(markup, i)
                markup.startsWith("<!--", i) -> {
                    val end = markup.indexOf("-->", i)
                    i = if (end < 0) markup.length else end + 3
                }
                else -> {
                    builder.append(markup[i])
                    i++
                }
            }
        }
        return builder.toString()
    }

    /** Skips `<!DOCTYPE …>` including an optional `[ … ]` internal subset. */
    private fun skipDoctype(markup: String, from: Int): Int {
        var depth = 0
        var i = from + 9
        while (i < markup.length) {
            when (markup[i]) {
                '[' -> depth++
                ']' -> depth--
                '>' -> if (depth <= 0) return i + 1
            }
            i++
        }
        return markup.length
    }

    private class TagEnd(val start: Int, val end: Int, val selfClosing: Boolean)

    private fun findTagEnd(markup: String, from: Int): TagEnd {
        var quote = '\u0000'
        var i = from
        while (i < markup.length) {
            val c = markup[i]
            if (quote != '\u0000') {
                if (c == quote) quote = '\u0000'
            } else {
                when (c) {
                    '"', '\'' -> quote = c
                    '>' -> return TagEnd(i, i + 1, markup.getOrNull(i - 1) == '/')
                }
            }
            i++
        }
        return TagEnd(markup.length, markup.length, selfClosing = true)
    }

    private fun parseTag(rawContent: String): Pair<String, Map<String, String>> {
        // The self-closing slash sits before '>', so it is not part of the name.
        val content = if (rawContent.endsWith("/")) rawContent.dropLast(1) else rawContent
        val attributes = LinkedHashMap<String, String>()
        var i = 0
        while (i < content.length && !content[i].isWhitespace()) i++
        val name = content.substring(0, i)

        while (i < content.length) {
            while (i < content.length && content[i].isWhitespace()) i++
            if (i >= content.length) break
            val nameStart = i
            while (i < content.length && content[i] != '=' && !content[i].isWhitespace()) i++
            val attributeName = content.substring(nameStart, i)
            while (i < content.length && content[i].isWhitespace()) i++
            if (i < content.length && content[i] == '=') {
                i++
                while (i < content.length && content[i].isWhitespace()) i++
                if (i < content.length && (content[i] == '"' || content[i] == '\'')) {
                    val quote = content[i]
                    val valueStart = ++i
                    while (i < content.length && content[i] != quote) i++
                    attributes[attributeName] = decodeEntities(content.substring(valueStart, i))
                    if (i < content.length) i++
                } else {
                    val valueStart = i
                    while (i < content.length && !content[i].isWhitespace()) i++
                    attributes[attributeName] = decodeEntities(content.substring(valueStart, i))
                }
            } else if (attributeName.isNotEmpty()) {
                attributes[attributeName] = ""
            }
        }
        return name to attributes
    }

    /** Resolves numeric and the five predefined XML references; unknown kept literal. */
    private fun decodeEntities(raw: String): String {
        if (raw.indexOf('&') < 0) return raw
        val builder = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '&') {
                val semicolon = raw.indexOf(';', i + 1)
                if (semicolon in (i + 1)..minOf(raw.length - 1, i + 10)) {
                    val decoded = decodeEntity(raw.substring(i + 1, semicolon))
                    if (decoded != null) {
                        builder.append(decoded)
                        i = semicolon + 1
                        continue
                    }
                }
            }
            builder.append(c)
            i++
        }
        return builder.toString()
    }

    private fun decodeEntity(entity: String): String? = when {
        entity == "amp" -> "&"
        entity == "lt" -> "<"
        entity == "gt" -> ">"
        entity == "quot" -> "\""
        entity == "apos" -> "'"
        entity.startsWith("#x") || entity.startsWith("#X") ->
            entity.substring(2).toIntOrNull(16)?.let(::codePointToString)
        entity.startsWith("#") ->
            entity.substring(1).toIntOrNull()?.let(::codePointToString)
        else -> null
    }

    private fun codePointToString(codePoint: Int): String? = when {
        codePoint <= 0 || codePoint > 0x10FFFF -> null
        codePoint < 0x10000 -> codePoint.toChar().toString()
        else -> {
            val offset = codePoint - 0x10000
            val high = (0xD800 + (offset shr 10)).toChar()
            val low = (0xDC00 + (offset and 0x3FF)).toChar()
            "$high$low"
        }
    }
}

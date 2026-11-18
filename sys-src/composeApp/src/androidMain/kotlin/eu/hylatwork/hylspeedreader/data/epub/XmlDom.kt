package eu.hylatwork.hylspeedreader.data.epub

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal, hardened XML access built only on `javax.xml`/`org.w3c.dom`.
 *
 * These are JDK *and* Android platform classes, so this keeps the parser
 * dependency-free while remaining JVM-testable (unlike `android.util.Xml`),
 * which is what lets the extraction tests run as plain unit tests.
 *
 * Two pre-processing steps make a strict XML parser behave like the lenient
 * HTML parser it replaces:
 *  - the document type declaration is dropped (XHTML chapters carry a DOCTYPE
 *    whose external DTD must never be fetched — that is also the XXE guard);
 *  - HTML named entities are decoded and unknown ones escaped, so common XHTML
 *    content cannot become a fatal "undeclared entity" error.
 */
internal object XmlDom {

    fun parse(xml: String): Document {
        val prepared = escapeBareAmpersands(XmlEntities.decode(stripDoctype(xml)))
        val factory = DocumentBuilderFactory.newInstance().apply {
            // false keeps prefixes part of the tag name, matching the way the
            // code looks up `dc:title`, `itemref`, … .
            isNamespaceAware = false
            isXIncludeAware = false
            isValidating = false
            isCoalescing = true
            feature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            feature("http://xml.org/sax/features/external-general-entities", false)
            feature("http://xml.org/sax/features/external-parameter-entities", false)
            feature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(ACCESS_EXTERNAL_SCHEMA, "") }
        }
        val builder = factory.newDocumentBuilder()
        // Belt and braces: even if a feature flag is unsupported (Harmony), an
        // external entity can never be dereferenced.
        builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
        // Reader-based input: the already-decoded String wins over any encoding
        // declaration, so EpubCharset stays the single source of truth.
        return builder.parse(InputSource(StringReader(prepared)))
    }

    private fun DocumentBuilderFactory.feature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    /** Removes `<!DOCTYPE …>` including an optional `[ … ]` internal subset. */
    private fun stripDoctype(xml: String): String {
        val start = xml.indexOf("<!DOCTYPE", ignoreCase = true)
        if (start < 0) return xml
        var depth = 0
        var i = start + 9
        while (i < xml.length) {
            when (xml[i]) {
                '[' -> depth++
                ']' -> depth--
                '>' -> if (depth <= 0) return xml.substring(0, start) + xml.substring(i + 1)
            }
            i++
        }
        return xml.substring(0, start)
    }

    /** Escapes an ampersand that is not part of a (numeric or named) reference. */
    private fun escapeBareAmpersands(xml: String): String =
        BARE_AMP.replace(xml, "&amp;")

    private val BARE_AMP = Regex("&(?!(?:[A-Za-z][A-Za-z0-9]*|#[0-9]+|#[xX][0-9A-Fa-f]+);)")

    // Android's javax.xml.XMLConstants omits these two attribute names, so the
    // JAXP-specified URIs are spelled out (they are applied via runCatching).
    private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
}

/** All elements with the given (case-sensitive) tag name, in document order. */
internal fun Document.elements(tag: String): List<Element> {
    val nodes = getElementsByTagName(tag)
    return ArrayList<Element>(nodes.length).apply {
        for (i in 0 until nodes.length) (nodes.item(i) as? Element)?.let(::add)
    }
}

/** Attribute value, or the empty string when absent (same as jsoup `attr`). */
internal fun Element.attrOrEmpty(name: String): String = getAttribute(name)

/** Whitespace-normalised text content (equivalent of jsoup `Element.text()`). */
internal fun Element.text(): String = (textContent ?: "").replace(WHITESPACE, " ").trim()

private val WHITESPACE = Regex("\\s+")

internal fun NodeList.asIterable(): Iterable<Node> = Iterable {
    object : Iterator<Node> {
        private var index = 0
        override fun hasNext(): Boolean = index < length
        override fun next(): Node = item(index++)
    }
}

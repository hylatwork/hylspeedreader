package eu.hylatwork.hylspeedreader.data.epub

/**
 * Named-character decoding for the XHTML entity references that EPUB chapters
 * commonly use. This is the one piece of "HTML knowledge" a full HTML parser
 * would provide for free; it is a finite, well-defined table (the HTML 4
 * Latin-1 set plus the usual typographic/symbol names), so it is embedded here.
 *
 * Shared by both codecs: the Android `XmlDom` (over `javax.xml`) and the
 * portable `PortableXml` (used by the iOS engine). The five XML-predefined
 * entities (`amp`, `lt`, `gt`, `quot`, `apos`) and all numeric references
 * (`&#233;`, `&#xE9;`) are deliberately left for the XML parser to resolve.
 */
internal object XmlEntities {

    private val PREDEFINED = setOf("amp", "lt", "gt", "quot", "apos")

    private val NAMED = Regex("&([A-Za-z][A-Za-z0-9]*);")

    private val TABLE: Map<String, Char> = """
        nbsp 00A0 iexcl 00A1 cent 00A2 pound 00A3 curren 00A4 yen 00A5 brvbar 00A6 sect 00A7
        uml 00A8 copy 00A9 ordf 00AA laquo 00AB not 00AC shy 00AD reg 00AE macr 00AF
        deg 00B0 plusmn 00B1 sup2 00B2 sup3 00B3 acute 00B4 micro 00B5 para 00B6 middot 00B7
        cedil 00B8 sup1 00B9 ordm 00BA raquo 00BB frac14 00BC frac12 00BD frac34 00BE iquest 00BF
        Agrave 00C0 Aacute 00C1 Acirc 00C2 Atilde 00C3 Auml 00C4 Aring 00C5 AElig 00C6 Ccedil 00C7
        Egrave 00C8 Eacute 00C9 Ecirc 00CA Euml 00CB Igrave 00CC Iacute 00CD Icirc 00CE Iuml 00CF
        ETH 00D0 Ntilde 00D1 Ograve 00D2 Oacute 00D3 Ocirc 00D4 Otilde 00D5 Ouml 00D6 times 00D7
        Oslash 00D8 Ugrave 00D9 Uacute 00DA Ucirc 00DB Uuml 00DC Yacute 00DD THORN 00DE szlig 00DF
        agrave 00E0 aacute 00E1 acirc 00E2 atilde 00E3 auml 00E4 aring 00E5 aelig 00E6 ccedil 00E7
        egrave 00E8 eacute 00E9 ecirc 00EA euml 00EB igrave 00EC iacute 00ED icirc 00EE iuml 00EF
        eth 00F0 ntilde 00F1 ograve 00F2 oacute 00F3 ocirc 00F4 otilde 00F5 ouml 00F6 divide 00F7
        oslash 00F8 ugrave 00F9 uacute 00FA ucirc 00FB uuml 00FC yacute 00FD thorn 00FE yuml 00FF
        OElig 0152 oelig 0153 Scaron 0160 scaron 0161 Yuml 0178 fnof 0192 circ 02C6 tilde 02DC
        ensp 2002 emsp 2003 thinsp 2009 zwnj 200C zwj 200D lrm 200E rlm 200F
        ndash 2013 mdash 2014 lsquo 2018 rsquo 2019 sbquo 201A ldquo 201C rdquo 201D bdquo 201E
        dagger 2020 Dagger 2021 bull 2022 hellip 2026 permil 2030 prime 2032 Prime 2033
        lsaquo 2039 rsaquo 203A oline 203E frasl 2044 euro 20AC trade 2122 minus 2212 lowast 2217
        radic 221A infin 221E ne 2260 le 2264 ge 2265 larr 2190 uarr 2191 rarr 2192 darr 2193 harr 2194
    """.trimIndent()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .chunked(2)
        .associate { (name, hex) -> name to hex.toInt(16).toChar() }

    /** Replaces known named references with their characters; escapes the unknown. */
    fun decode(xml: String): String = NAMED.replace(xml) { match ->
        val name = match.groupValues[1]
        val decoded = TABLE[name]
        when {
            decoded != null -> decoded.toString()
            name in PREDEFINED -> match.value // let the XML parser resolve it
            else -> "&amp;$name;"             // keep it as literal text, never fatal
        }
    }
}

package eu.hylatwork.hylspeedreader.domain.model

/** Metadata for one spine document of an EPUB (content is loaded lazily). */
data class Chapter(
    val index: Int,
    val title: String
)

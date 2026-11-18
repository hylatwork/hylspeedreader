package eu.hylatwork.hylspeedreader.data.epub

/** Turns a picked document into a parsed, still-open [EpubBook]. */
interface EpubEngine {
    suspend fun open(handle: DocumentHandle): EpubBook
}

/** Platform factory for the [EpubEngine]. */
expect fun createEpubEngine(): EpubEngine

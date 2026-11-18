package eu.hylatwork.hylspeedreader.data.epub

import android.content.Context
import eu.hylatwork.hylspeedreader.data.Platform

actual fun createEpubEngine(): EpubEngine = AndroidEpubEngine(Platform.appContext)

/** Android EPUB codec: SAF stream -> cached copy -> `java.util.zip` + platform XML DOM. */
private class AndroidEpubEngine(context: Context) : EpubEngine {

    private val parser = EpubParser(context)

    override suspend fun open(handle: DocumentHandle): EpubBook =
        parser.parse((handle as AndroidDocumentHandle).uri)
}

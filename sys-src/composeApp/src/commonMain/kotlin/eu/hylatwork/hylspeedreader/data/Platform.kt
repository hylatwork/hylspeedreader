package eu.hylatwork.hylspeedreader.data

import com.russhwolf.settings.Settings
import eu.hylatwork.hylspeedreader.data.epub.EpubEngine
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The small set of platform services the shared code needs. Everything that
 * cannot live in `commonMain` (an Android `Context`, `SharedPreferences`,
 * Logcat, the IO dispatcher) is funnelled through this single seam so the
 * domain and UI layers stay platform-agnostic.
 */
expect object Platform {

    /** The EPUB reader for this platform. */
    fun epubEngine(): EpubEngine

    /** Durable key/value storage for the last reading position. */
    fun settings(): Settings

    /** Platform log sink (Logcat on Android, `NSLog` on iOS). */
    fun logW(tag: String, message: String, throwable: Throwable?)

    /** Dispatcher for blocking archive reads. */
    val ioDispatcher: CoroutineDispatcher
}

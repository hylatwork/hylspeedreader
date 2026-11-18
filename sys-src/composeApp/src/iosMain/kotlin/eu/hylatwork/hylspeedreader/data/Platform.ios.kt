package eu.hylatwork.hylspeedreader.data

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import eu.hylatwork.hylspeedreader.data.epub.EpubEngine
import eu.hylatwork.hylspeedreader.data.epub.createEpubEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSLog
import platform.Foundation.NSUserDefaults

actual object Platform {

    actual fun epubEngine(): EpubEngine = createEpubEngine()

    actual fun settings(): Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

    actual fun logW(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] $message${throwable?.let { ": ${it.message}" } ?: ""}")
    }

    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
}

package eu.hylatwork.hylspeedreader.data

import android.content.Context
import android.util.Log
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import eu.hylatwork.hylspeedreader.data.epub.EpubEngine
import eu.hylatwork.hylspeedreader.data.epub.createEpubEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object Platform {

    private const val PREFS_NAME = "hylspeedreader.settings"

    internal lateinit var appContext: Context
        private set

    /** Called once from `MainActivity` before any platform service is used. */
    internal fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun epubEngine(): EpubEngine = createEpubEngine()

    actual fun settings(): Settings =
        SharedPreferencesSettings(appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    actual fun logW(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
}

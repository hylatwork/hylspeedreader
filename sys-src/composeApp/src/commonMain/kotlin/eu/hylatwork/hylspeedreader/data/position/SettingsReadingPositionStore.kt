package eu.hylatwork.hylspeedreader.data.position

import com.russhwolf.settings.Settings
import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.domain.model.ReadingPosition

/**
 * Multiplatform [ReadingPositionStore] over a `multiplatform-settings`
 * [Settings] instance (SharedPreferences on Android, NSUserDefaults on iOS).
 *
 * Writes go through the settings backend, which batches/commits them for us, so
 * the playback loop can snapshot a position while ticking.
 */
class SettingsReadingPositionStore(private val settings: Settings) : ReadingPositionStore {

    override fun load(): ReadingPosition? {
        val uri = settings.getStringOrNull(KEY_URI) ?: return null
        return ReadingPosition(
            bookUri = uri,
            chapterIndex = settings.getInt(KEY_CHAPTER, 0),
            wordIndex = settings.getInt(KEY_WORD, 0),
            wpm = settings.getInt(KEY_WPM, RsvpTiming.DEFAULT_WPM)
        ).validOrNull()
    }

    override fun save(position: ReadingPosition) {
        settings.putString(KEY_URI, position.bookUri)
        settings.putInt(KEY_CHAPTER, position.chapterIndex)
        settings.putInt(KEY_WORD, position.wordIndex)
        settings.putInt(KEY_WPM, position.wpm)
    }

    override fun clear() {
        settings.clear()
    }

    private companion object {
        const val KEY_URI = "book_uri"
        const val KEY_CHAPTER = "chapter_index"
        const val KEY_WORD = "word_index"
        const val KEY_WPM = "wpm"
    }
}

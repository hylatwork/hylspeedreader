package eu.hylatwork.hylspeedreader.ui

import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.domain.model.Chapter
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord

/** Lifecycle of the RSVP stream. */
sealed interface PlaybackStatus {
    /** No book opened yet. */
    data object Idle : PlaybackStatus

    /** Words are currently being streamed. */
    data object Playing : PlaybackStatus

    /** Stream interrupted; position retained. */
    data object Paused : PlaybackStatus

    /** The last word of the chapter was shown. */
    data object ChapterEnd : PlaybackStatus
}

/**
 * User-facing failures, expressed as keys rather than strings so the shared
 * state stays free of platform resources; the UI maps them to `Res.string.*`.
 */
enum class ReaderError {
    OPEN_EPUB,
    LOAD_CHAPTER
}

/**
 * Single source of truth rendered by the UI. Immutable snapshot – every state
 * change is an atomic copy, so Compose recomposition is trivially diffable.
 */
data class ReaderUiState(
    val isLoading: Boolean = false,
    val bookTitle: String = "",
    val author: String = "",
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int = -1,
    val chapterTitle: String = "",
    val chapterWordCount: Int = 0,
    val currentWordIndex: Int = 0,
    val currentWord: RsvpWord? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val wpm: Int = RsvpTiming.DEFAULT_WPM,
    val error: ReaderError? = null
) {
    val hasBook: Boolean get() = currentChapterIndex >= 0

    /** Chapter progress in 0..1 for the seek slider. */
    val progress: Float
        get() = if (chapterWordCount > 1) currentWordIndex / (chapterWordCount - 1).toFloat() else 0f
}

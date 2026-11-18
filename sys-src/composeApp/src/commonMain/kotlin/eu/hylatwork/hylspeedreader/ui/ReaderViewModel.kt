package eu.hylatwork.hylspeedreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.hylatwork.hylspeedreader.data.Platform
import eu.hylatwork.hylspeedreader.data.epub.DocumentHandle
import eu.hylatwork.hylspeedreader.data.epub.EpubBook
import eu.hylatwork.hylspeedreader.data.epub.EpubEngine
import eu.hylatwork.hylspeedreader.data.epub.persistedStringToDocumentHandle
import eu.hylatwork.hylspeedreader.data.position.ReadingPositionStore
import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.domain.logic.RsvpTicker
import eu.hylatwork.hylspeedreader.domain.model.ReadingPosition
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * MVVM player: owns all playback state in a single [StateFlow] and drives the
 * RSVP tick loop with the pure-domain [RsvpTicker]. The tick loop runs on
 * `Dispatchers.Default`; chapter archive reads run on the platform IO
 * dispatcher; the UI thread only ever renders immutable state snapshots.
 *
 * Every asynchronous unit of work is tracked (import, chapter load, tick) and
 * guarded by an invalidation token so a slower, superseded task can never
 * overwrite the state of a newer one. While work is in flight `isLoading` is
 * true and the transport controls are disabled, so no action ever operates on
 * stale chapter data.
 *
 * Platform services are injected ([engine], [positionStore]) so the same class
 * runs on Android and iOS.
 */
class ReaderViewModel(
    private val engine: EpubEngine,
    private val positionStore: ReadingPositionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var document: EpubBook? = null

    @Volatile
    private var chapterWords: List<RsvpWord> = emptyList()

    /** Persisted handle of the open book; null when nothing is open. */
    private var bookHandle: String? = null

    /** True while the initial persisted position is being re-opened. */
    private var restoring = false

    private var openJob: Job? = null
    private var chapterJob: Job? = null
    private var tickerJob: Job? = null

    // Invalidate in-flight async work so a superseded task cannot mutate state.
    private var openToken = 0
    private var chapterLoadToken = 0

    init {
        restoreLastPosition()
    }

    // ------------------------------------------------------------ ingestion

    private fun restoreLastPosition() {
        val saved = positionStore.load() ?: return
        val handle = persistedStringToDocumentHandle(saved.bookUri)
        if (handle == null) {
            positionStore.clear()
            return
        }
        restoring = true
        _uiState.value = _uiState.value.copy(wpm = saved.wpm)
        openEpub(handle, startChapter = saved.chapterIndex, startWord = saved.wordIndex)
    }

    fun openEpub(handle: DocumentHandle, startChapter: Int = 0, startWord: Int = 0) {
        // A user-initiated open during a silent restore should surface errors.
        val silentRestore = restoring
        restoring = false

        stopTicker()
        stopChapterLoad()
        stopOpen()
        val token = openToken
        document?.let { runCatching(it::close) }
        document = null
        chapterWords = emptyList()
        bookHandle = null
        val keepWpm = _uiState.value.wpm
        _uiState.value = ReaderUiState(isLoading = true, wpm = keepWpm)

        openJob = viewModelScope.launch {
            try {
                val parsed = engine.open(handle) // suspends on the platform IO dispatcher
                if (token != openToken) {
                    runCatching(parsed::close)
                    return@launch
                }
                document = parsed
                bookHandle = handle.persistedValue
                handle.takePersistableAccess()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookTitle = parsed.title,
                        author = parsed.author,
                        chapters = parsed.chapters
                    )
                }
                selectChapter(startChapter, startWordIndex = startWord)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token != openToken) return@launch
                if (silentRestore) {
                    // The saved book is gone or its permission was revoked:
                    // clear the stale position and start from the empty state.
                    Platform.logW(TAG, "Could not restore the last book; starting fresh", e)
                    positionStore.clear()
                    _uiState.value = ReaderUiState(wpm = keepWpm)
                } else {
                    fail(ReaderError.OPEN_EPUB, "Failed to open the selected EPUB", e)
                }
            }
        }
    }

    fun selectChapter(index: Int, autoPlay: Boolean = false, startWordIndex: Int = 0) {
        val doc = document ?: return
        if (index !in doc.chapters.indices) return
        val resumePlayback = autoPlay || _uiState.value.status == PlaybackStatus.Playing
        stopTicker()
        stopChapterLoad()
        val token = chapterLoadToken
        _uiState.update { it.copy(isLoading = true, status = PlaybackStatus.Paused) }

        chapterJob = viewModelScope.launch(Platform.ioDispatcher) {
            try {
                val words = doc.loadChapter(index)
                if (token != chapterLoadToken) return@launch
                chapterWords = words
                val wordIndex = startWordIndex.coerceIn(0, words.lastIndex.coerceAtLeast(0))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentChapterIndex = index,
                        chapterTitle = doc.chapters[index].title,
                        chapterWordCount = words.size,
                        currentWordIndex = wordIndex,
                        currentWord = words.getOrNull(wordIndex),
                        status = PlaybackStatus.Paused
                    )
                }
                persist()
                if (resumePlayback) play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (token != chapterLoadToken) return@launch
                fail(ReaderError.LOAD_CHAPTER, "Failed to load chapter $index", e)
            }
        }
    }

    // ------------------------------------------------------------- playback

    fun playPause() {
        if (_uiState.value.status == PlaybackStatus.Playing) pause() else play()
    }

    fun play() {
        if (_uiState.value.isLoading) return
        val words = chapterWords
        if (words.isEmpty()) return
        val state = _uiState.value
        val start =
            if (state.status == PlaybackStatus.ChapterEnd && state.currentWordIndex >= words.lastIndex) 0
            else state.currentWordIndex.coerceIn(0, words.lastIndex)
        stopTicker()

        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(status = PlaybackStatus.Playing) }
            RsvpTicker.indices(words, start) { _uiState.value.wpm }.collect { index ->
                _uiState.update {
                    it.copy(
                        status = PlaybackStatus.Playing,
                        currentWordIndex = index,
                        currentWord = words[index]
                    )
                }
                if (index % POSITION_SAVE_EVERY_WORDS == 0) persist()
            }
            // The flow only completes when the last word has been shown.
            _uiState.update { it.copy(status = PlaybackStatus.ChapterEnd) }
            persist()
        }
    }

    fun pause() {
        stopTicker()
        _uiState.update { state ->
            if (state.status == PlaybackStatus.Playing) state.copy(status = PlaybackStatus.Paused) else state
        }
        persist()
    }

    fun seekToFraction(fraction: Float) {
        if (_uiState.value.isLoading) return
        val words = chapterWords
        if (words.isEmpty()) return
        seekToWord((fraction.coerceIn(0f, 1f) * (words.size - 1)).roundToInt())
    }

    fun skipWords(delta: Int) {
        seekToWord(_uiState.value.currentWordIndex + delta)
    }

    fun changeWpm(wpm: Int) {
        _uiState.update { it.copy(wpm = wpm.coerceIn(RsvpTiming.MIN_WPM, RsvpTiming.MAX_WPM)) }
    }

    fun nextChapter() {
        val state = _uiState.value
        if (state.hasBook && state.currentChapterIndex < state.chapters.lastIndex) {
            selectChapter(state.currentChapterIndex + 1, autoPlay = state.status == PlaybackStatus.Playing)
        }
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state.hasBook && state.currentChapterIndex > 0) {
            selectChapter(state.currentChapterIndex - 1, autoPlay = state.status == PlaybackStatus.Playing)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        persist()
        stopTicker()
        stopChapterLoad()
        stopOpen()
        document?.let { runCatching(it::close) }
        document = null
    }

    // ------------------------------------------------------------- internals

    private fun seekToWord(target: Int) {
        if (_uiState.value.isLoading) return
        val words = chapterWords
        if (words.isEmpty()) return
        val index = target.coerceIn(0, words.lastIndex)
        val wasPlaying = _uiState.value.status == PlaybackStatus.Playing
        stopTicker()
        _uiState.update {
            it.copy(
                currentWordIndex = index,
                currentWord = words[index],
                status = if (wasPlaying) PlaybackStatus.Playing else PlaybackStatus.Paused
            )
        }
        persist()
        if (wasPlaying) play()
    }

    /** Saves a snapshot of the current position so it survives process death. */
    private fun persist() {
        val uri = bookHandle ?: return
        val state = _uiState.value
        if (!state.hasBook) return
        positionStore.save(
            ReadingPosition(
                bookUri = uri,
                chapterIndex = state.currentChapterIndex,
                wordIndex = state.currentWordIndex,
                wpm = state.wpm
            )
        )
    }

    /**
     * Reports a failure without leaking the exception message to the UI: the
     * detail goes to the platform log, the user sees a resource-backed key.
     */
    private fun fail(error: ReaderError, logMessage: String, cause: Throwable) {
        Platform.logW(TAG, logMessage, cause)
        _uiState.update { it.copy(isLoading = false, error = error) }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** Cancels the in-flight chapter load and invalidates its completion. */
    private fun stopChapterLoad() {
        chapterLoadToken++
        chapterJob?.cancel()
        chapterJob = null
    }

    /** Cancels the in-flight import and invalidates its completion. */
    private fun stopOpen() {
        openToken++
        openJob?.cancel()
        openJob = null
    }

    private companion object {
        const val TAG = "ReaderViewModel"

        /** Persist roughly every this many words while playing. */
        const val POSITION_SAVE_EVERY_WORDS = 25
    }
}

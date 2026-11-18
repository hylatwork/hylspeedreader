package eu.hylatwork.hylspeedreader.domain.logic

import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The RSVP playback cadence, isolated from Android so it can be tested with
 * virtual time.
 *
 * Emits the index of the word to display, then suspends for that word's dwell
 * time ([RsvpTiming.delayMs]). Cancellation is immediate because `delay` is
 * cancellable, and [wpm] is evaluated for every word, so a speed change applies
 * from the very next tick without restarting the stream.
 */
object RsvpTicker {

    fun indices(words: List<RsvpWord>, startIndex: Int, wpm: () -> Int): Flow<Int> = flow {
        var index = startIndex.coerceIn(0, words.size)
        while (index < words.size) {
            emit(index)
            delay(RsvpTiming.delayMs(words[index], wpm()))
            index++
        }
    }
}

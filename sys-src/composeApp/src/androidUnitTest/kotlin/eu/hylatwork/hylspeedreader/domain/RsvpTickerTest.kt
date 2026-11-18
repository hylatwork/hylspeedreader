package eu.hylatwork.hylspeedreader.domain

import eu.hylatwork.hylspeedreader.domain.logic.RsvpTicker
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Virtual-time tests for the RSVP cadence (FR-4 / FR-5). `runTest` advances the
 * clock instantly, so the full 60 ms..1 s dwell range is exercised in-process.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RsvpTickerTest {

    private val plainWords = listOf("at", "be", "of", "to").map { RsvpWord.of(it)!! }

    @Test
    fun emitsEveryIndexFromStartToEnd() = runTest {
        assertEquals(listOf(0, 1, 2, 3), RsvpTicker.indices(plainWords, 0) { 350 }.toList())
    }

    @Test
    fun resumesFromTheGivenStartIndex() = runTest {
        assertEquals(listOf(2, 3), RsvpTicker.indices(plainWords, 2) { 350 }.toList())
    }

    @Test
    fun emptyWordListCompletesImmediately() = runTest {
        assertEquals(emptyList<Int>(), RsvpTicker.indices(emptyList(), 0) { 350 }.toList())
    }

    @Test
    fun dwellTimeIsSixtyThousandOverWpm() = runTest {
        val start = testScheduler.currentTime
        RsvpTicker.indices(plainWords, 0) { 300 }.toList()
        // 4 words * (60_000 / 300) ms = 800 ms of virtual time.
        assertEquals(800L, testScheduler.currentTime - start)
    }

    @Test
    fun wpmIsReadForEveryWordSoSpeedChangesApplyImmediately() = runTest {
        var wpm = 300
        val marks = mutableListOf<Long>()
        RsvpTicker.indices(plainWords, 0) { wpm }
            .onEach { if (it == 1) wpm = 600 }
            .collect { marks += testScheduler.currentTime }

        // 0@0, 1@200 (300 wpm), then 2@300 and 3@400 (600 wpm).
        assertEquals(listOf(0L, 200L, 300L, 400L), marks)
    }

    @Test
    fun startIndexBeyondTheEndIsClamped() = runTest {
        assertEquals(emptyList<Int>(), RsvpTicker.indices(plainWords, 99) { 350 }.toList())
    }
}

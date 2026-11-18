package eu.hylatwork.hylspeedreader.ui.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.hylatwork.hylspeedreader.ui.PlaybackStatus
import eu.hylatwork.hylspeedreader.ui.ReaderUiState
import eu.hylatwork.hylspeedreader.ui.theme.HylSpeedReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented checks for the transport bar and its accessibility labels. The
 * user-facing strings now live in shared Compose resources, so the expected
 * English values are asserted as literals rather than through Android `R`.
 */
class PlayerControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = ReaderUiState(
        currentChapterIndex = 0,
        chapterTitle = "Chapter One",
        chapterWordCount = 10,
        status = PlaybackStatus.Paused
    )

    private fun showControls(onPlayPause: () -> Unit = {}) {
        composeRule.setContent {
            HylSpeedReaderTheme {
                PlayerControls(
                    state = state,
                    onPlayPause = onPlayPause,
                    onSeek = {},
                    onSkipWords = {},
                    onWpmChange = {},
                    onPreviousChapter = {},
                    onNextChapter = {}
                )
            }
        }
    }

    @Test
    fun playButtonInvokesPlayPause() {
        var playPause = false
        showControls(onPlayPause = { playPause = true })
        composeRule.onNodeWithContentDescription("Play").performClick()
        assertTrue(playPause)
    }

    @Test
    fun scrubbingAndSpeedSlidersExposeLabels() {
        showControls()
        composeRule.onNodeWithContentDescription("Reading position").assertExists()
        composeRule.onNodeWithContentDescription("Reading speed").assertExists()
    }

    @Test
    fun showsWordProgress() {
        showControls()
        composeRule.onNodeWithText("1 / 10 words").assertExists()
    }
}

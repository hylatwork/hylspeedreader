package eu.hylatwork.hylspeedreader.ui.rsvp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import eu.hylatwork.hylspeedreader.ui.theme.HylSpeedReaderTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented checks for the focus-aligned RSVP canvas. Shared Compose
 * resources replace the Android `R` strings, so English values are asserted as
 * literals.
 */
class RsvpWordViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wordIsRenderedButKeptOutOfAccessibilityTree() {
        composeRule.setContent {
            HylSpeedReaderTheme { RsvpWordView(word = RsvpWord.of("reading")) }
        }
        // The flipping word is intentionally non-semantic (TalkBack would
        // otherwise announce every tick); the labelled controls carry the UI.
        composeRule.onNodeWithText("reading").assertDoesNotExist()
    }

    @Test
    fun rendersDefaultPlaceholderWhenNoWord() {
        composeRule.setContent {
            HylSpeedReaderTheme { RsvpWordView(word = null) }
        }
        composeRule.onNodeWithText("Ready").assertExists()
    }

    @Test
    fun rendersExplicitPlaceholder() {
        val placeholder = "This chapter contains no extractable text"
        composeRule.setContent {
            HylSpeedReaderTheme { RsvpWordView(word = null, placeholder = placeholder) }
        }
        composeRule.onNodeWithText(placeholder).assertExists()
    }
}

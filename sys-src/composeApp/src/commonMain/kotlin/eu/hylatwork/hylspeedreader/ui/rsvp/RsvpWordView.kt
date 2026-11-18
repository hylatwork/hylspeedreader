package eu.hylatwork.hylspeedreader.ui.rsvp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.hylatwork.hylspeedreader.domain.model.RsvpWord
import eu.hylatwork.hylspeedreader.resources.Res
import eu.hylatwork.hylspeedreader.resources.reader_placeholder_ready
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

private val GuideTickLength = 12.dp

/**
 * Minimalist RSVP canvas:
 *  - fixed center axis with top/bottom focus ticks and a faint guide between them
 *  - the focus character is highlighted and aligned to the axis with pixel precision.
 *
 * Alignment strategy: the word is rendered as ONE text line (an AnnotatedString
 * with a colored span for the focus char, so kerning stays intact). A
 * `TextMeasurer` measures `prefix` and `prefix + focusChar`
 * with the exact same style; the delta is the focus advance *in context*, so a
 * custom [Layout] can shift the whole line so the focus center lands exactly on
 * the box's vertical axis — regardless of font or letter widths.
 */
@Composable
fun RsvpWordView(
    word: RsvpWord?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    boxHeight: Dp = 148.dp,
    highlightColor: Color = Color.Unspecified,
    guideColor: Color = Color.Unspecified,
    placeholder: String? = null,
) {
    val highlight = if (highlightColor == Color.Unspecified) MaterialTheme.colorScheme.error else highlightColor
    val guide = if (guideColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else guideColor
    val placeholderText = placeholder ?: stringResource(Res.string.reader_placeholder_ready)

    val textMeasurer = rememberTextMeasurer()
    val wordStyle = remember(fontSize) { TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold) }

    // Measured once per word (not per recomposition); TextMeasurer caches the
    // glyph runs internally, so this is two lookups per new word.
    val (prefixWidth, focusWidth) = remember(word, wordStyle) {
        if (word == null) 0 to 0 else {
            val prefix = textMeasurer.measure(word.prefix, wordStyle).size.width
            val withFocus = textMeasurer.measure(word.prefix + word.focusChar, wordStyle).size.width
            prefix to (withFocus - prefix)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = boxHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                val centerX = size.width / 2f
                val tick = GuideTickLength.toPx()
                val stroke = 2.dp.toPx()
                drawLine(guide, Offset(centerX, 0f), Offset(centerX, tick), strokeWidth = stroke)
                drawLine(
                    guide,
                    Offset(centerX, size.height - tick),
                    Offset(centerX, size.height),
                    strokeWidth = stroke
                )
                drawLine(
                    guide.copy(alpha = 0.25f),
                    Offset(centerX, tick + stroke),
                    Offset(centerX, size.height - tick - stroke),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        if (word == null) {
            Text(
                text = placeholderText,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val currentWord = word
            val annotatedWord = remember(currentWord, highlight) {
                buildAnnotatedString {
                    append(currentWord.prefix)
                    withStyle(SpanStyle(color = highlight)) { append(currentWord.focusChar.toString()) }
                    append(currentWord.suffix)
                }
            }
            Layout(content = {
                Text(
                    text = annotatedWord,
                    style = wordStyle,
                    maxLines = 1,
                    softWrap = false,
                    // The word flips up to ~17x/second; announcing each one would
                    // be unusable, so it is kept out of the accessibility tree.
                    // The labelled transport controls carry the interaction.
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }) { measurables, constraints ->
                val text = measurables.first().measure(constraints)
                // Shift the single-line Text so prefixWidth + focusWidth/2 == axis.
                val x = (constraints.maxWidth / 2f - prefixWidth - focusWidth / 2f).roundToInt()
                val y = (constraints.maxHeight - text.height) / 2
                layout(constraints.maxWidth, constraints.maxHeight) {
                    text.placeRelative(x, y)
                }
            }
        }
    }
}

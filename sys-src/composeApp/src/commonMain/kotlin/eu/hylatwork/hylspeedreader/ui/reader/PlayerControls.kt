package eu.hylatwork.hylspeedreader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.hylatwork.hylspeedreader.domain.logic.RsvpTiming
import eu.hylatwork.hylspeedreader.resources.Res
import eu.hylatwork.hylspeedreader.resources.cd_next_chapter
import eu.hylatwork.hylspeedreader.resources.cd_pause
import eu.hylatwork.hylspeedreader.resources.cd_play
import eu.hylatwork.hylspeedreader.resources.cd_previous_chapter
import eu.hylatwork.hylspeedreader.resources.cd_reading_progress
import eu.hylatwork.hylspeedreader.resources.cd_reading_speed
import eu.hylatwork.hylspeedreader.resources.cd_skip_back
import eu.hylatwork.hylspeedreader.resources.cd_skip_forward
import eu.hylatwork.hylspeedreader.resources.chapter_complete
import eu.hylatwork.hylspeedreader.resources.chapter_none_loaded
import eu.hylatwork.hylspeedreader.resources.chapter_of
import eu.hylatwork.hylspeedreader.resources.speed_label
import eu.hylatwork.hylspeedreader.resources.speed_value
import eu.hylatwork.hylspeedreader.resources.words_progress
import eu.hylatwork.hylspeedreader.ui.PlaybackStatus
import eu.hylatwork.hylspeedreader.ui.ReaderUiState
import eu.hylatwork.hylspeedreader.ui.theme.HylIcons
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val WORDS_PER_SKIP = 10

/**
 * Transport dock: chapter context, scrub slider, transport buttons and the
 * 200-1000 WPM speed control, grouped on a single elevated surface so the
 * controls read as one unit distinct from the reading stage above.
 */
@Composable
fun PlayerControls(
    state: ReaderUiState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipWords: (Int) -> Unit,
    onWpmChange: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val canControl = state.hasBook && !state.isLoading && state.chapterWordCount > 0
    val progressLabel = stringResource(Res.string.cd_reading_progress)
    val speedLabel = stringResource(Res.string.cd_reading_speed)
    val progressState = pluralStringResource(
        Res.plurals.words_progress,
        state.chapterWordCount,
        state.currentWordIndex + 1,
        state.chapterWordCount
    )
    // Do not expose a position that changes on every word tick as a live
    // accessibility value while the stream is running; assistive technology must
    // not announce content that updates this fast (NFR-3.1).
    val announcesProgress = state.status != PlaybackStatus.Playing
    val dockPadding = if (compact) 8.dp else 12.dp

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = dockPadding),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            ChapterHeader(state, progressState)

            // Scrub/seek slider. While dragging we display the local value only
            // and commit it once the gesture finishes to avoid feedback loops
            // with the live word ticks.
            var dragProgress by remember(state.currentChapterIndex) { mutableStateOf<Float?>(null) }
            Slider(
                value = (dragProgress ?: state.progress).coerceIn(0f, 1f),
                onValueChange = { dragProgress = it },
                onValueChangeFinished = {
                    dragProgress?.let(onSeek)
                    dragProgress = null
                },
                enabled = canControl && state.chapterWordCount > 1,
                modifier = Modifier
                    .then(
                        if (announcesProgress) {
                            Modifier.semantics { stateDescription = progressState }
                        } else {
                            Modifier
                        }
                    )
                    .semantics { contentDescription = progressLabel }
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousChapter,
                    enabled = state.hasBook && state.currentChapterIndex > 0
                ) {
                    Icon(
                        HylIcons.SkipPrevious,
                        contentDescription = stringResource(Res.string.cd_previous_chapter)
                    )
                }
                IconButton(
                    onClick = { onSkipWords(-WORDS_PER_SKIP) },
                    enabled = canControl
                ) {
                    Icon(
                        HylIcons.FastRewind,
                        contentDescription =
                            pluralStringResource(Res.plurals.cd_skip_back, WORDS_PER_SKIP, WORDS_PER_SKIP)
                    )
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = canControl,
                    modifier = Modifier.size(if (compact) 56.dp else 72.dp)
                ) {
                    Icon(
                        imageVector =
                            if (state.status == PlaybackStatus.Playing) HylIcons.Pause
                            else HylIcons.PlayArrow,
                        contentDescription = stringResource(
                            if (state.status == PlaybackStatus.Playing) Res.string.cd_pause else Res.string.cd_play
                        ),
                        modifier = Modifier.size(if (compact) 28.dp else 34.dp)
                    )
                }
                IconButton(
                    onClick = { onSkipWords(WORDS_PER_SKIP) },
                    enabled = canControl
                ) {
                    Icon(
                        HylIcons.FastForward,
                        contentDescription =
                            pluralStringResource(Res.plurals.cd_skip_forward, WORDS_PER_SKIP, WORDS_PER_SKIP)
                    )
                }
                IconButton(
                    onClick = onNextChapter,
                    enabled = state.hasBook && state.currentChapterIndex < state.chapters.lastIndex
                ) {
                    Icon(
                        HylIcons.SkipNext,
                        contentDescription = stringResource(Res.string.cd_next_chapter)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.speed_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.speed_value, state.wpm),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.widthIn(min = 80.dp)
                )
                Slider(
                    value = state.wpm.toFloat(),
                    onValueChange = { onWpmChange(it.roundToInt()) },
                    valueRange = RsvpTiming.MIN_WPM.toFloat()..RsvpTiming.MAX_WPM.toFloat(),
                    modifier = Modifier
                        .semantics { contentDescription = speedLabel }
                        .weight(1f)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

/** Chapter title, position ("Chapter 3 of 12") and the word-count readout. */
@Composable
private fun ChapterHeader(state: ReaderUiState, progressState: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.chapterTitle.ifEmpty { stringResource(Res.string.chapter_none_loaded) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text =
                    if (state.status == PlaybackStatus.ChapterEnd) stringResource(Res.string.chapter_complete)
                    else stringResource(Res.string.chapter_of, state.currentChapterIndex + 1, state.chapters.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = progressState,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

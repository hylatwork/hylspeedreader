package eu.hylatwork.hylspeedreader.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.hylatwork.hylspeedreader.resources.Res
import eu.hylatwork.hylspeedreader.resources.chapter_label
import eu.hylatwork.hylspeedreader.resources.drawer_empty
import eu.hylatwork.hylspeedreader.resources.drawer_no_book
import eu.hylatwork.hylspeedreader.ui.ReaderUiState
import org.jetbrains.compose.resources.stringResource

/** Inset matching a drawer item's label, so the header aligns with the list. */
private val DrawerContentPadding = 28.dp

/**
 * Chapter overview shown inside the modal navigation drawer. Selecting a
 * chapter loads it (and keeps playing, if the stream was running). Uses the
 * Material 3 [NavigationDrawerItem] so the active chapter gets the standard
 * pill indicator and assistive technology hears the "selected" state.
 */
@Composable
fun ChapterDrawerContent(
    state: ReaderUiState,
    onChapterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val noBookLabel = stringResource(Res.string.drawer_no_book)

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                start = DrawerContentPadding,
                end = DrawerContentPadding,
                top = 16.dp,
                bottom = 8.dp
            )
        ) {
            Text(
                text = state.bookTitle.ifEmpty { noBookLabel },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (state.author.isNotEmpty()) {
                Text(
                    text = state.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        if (state.chapters.isEmpty()) {
            Text(
                text = stringResource(Res.string.drawer_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DrawerContentPadding)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.chapters, key = { it.index }) { chapter ->
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(chapter.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = stringResource(Res.string.chapter_label, chapter.index + 1),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = chapter.index == state.currentChapterIndex,
                        onClick = { onChapterSelected(chapter.index) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}

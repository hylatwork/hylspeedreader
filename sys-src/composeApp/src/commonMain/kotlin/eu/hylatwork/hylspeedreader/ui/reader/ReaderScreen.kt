package eu.hylatwork.hylspeedreader.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.hylatwork.hylspeedreader.resources.Res
import eu.hylatwork.hylspeedreader.resources.cd_chapter_overview
import eu.hylatwork.hylspeedreader.resources.cd_open_epub
import eu.hylatwork.hylspeedreader.resources.error_load_chapter
import eu.hylatwork.hylspeedreader.resources.error_open_epub
import eu.hylatwork.hylspeedreader.resources.reader_placeholder_empty_chapter
import eu.hylatwork.hylspeedreader.resources.reader_placeholder_loading
import eu.hylatwork.hylspeedreader.resources.reader_placeholder_ready
import eu.hylatwork.hylspeedreader.resources.reader_title
import eu.hylatwork.hylspeedreader.ui.ReaderError
import eu.hylatwork.hylspeedreader.ui.ReaderViewModel
import eu.hylatwork.hylspeedreader.ui.rememberEpubPicker
import eu.hylatwork.hylspeedreader.ui.rememberReaderViewModel
import eu.hylatwork.hylspeedreader.ui.rsvp.RsvpWordView
import eu.hylatwork.hylspeedreader.ui.theme.HylIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Root screen: chapter drawer + scaffold with the RSVP canvas and controls.
 * EPUBs are picked through the platform document picker (no permissions on
 * Android; copy-in on iOS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = rememberReaderViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultTitle = stringResource(Res.string.reader_title)
    val openError = stringResource(Res.string.error_open_epub)
    val loadError = stringResource(Res.string.error_load_chapter)

    val openEpub = rememberEpubPicker { handle -> handle?.let(viewModel::openEpub) }
    val openPicker = { openEpub() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(if (error == ReaderError.OPEN_EPUB) openError else loadError)
            viewModel.dismissError()
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = state.hasBook,
        drawerContent = {
            ModalDrawerSheet {
                ChapterDrawerContent(
                    state = state,
                    onChapterSelected = { index ->
                        viewModel.selectChapter(index)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.bookTitle.ifEmpty { defaultTitle },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (state.hasBook) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    HylIcons.Menu,
                                    contentDescription = stringResource(Res.string.cd_chapter_overview)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = openPicker) {
                            Icon(
                                HylIcons.MenuBook,
                                contentDescription = stringResource(Res.string.cd_open_epub)
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                // Constrain the reading column on tablets / landscape so the RSVP
                // stage and transport dock keep a comfortable line length. When the
                // available height is short (phone landscape, small foldable) the
                // stage and dock shrink together so nothing is clipped and the fixed
                // focal axis (the stage's centre) never drifts (NFR-4.2).
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val compact = maxHeight < 560.dp
                    val stagePadding = if (compact) 8.dp else 16.dp
                    val stageHeight = if (compact) 104.dp else 148.dp
                    val stageFontSize = if (compact) 32.sp else 40.sp
                    Column(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        if (state.isLoading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }

                        if (!state.hasBook && !state.isLoading) {
                            EmptyState(onOpenEpub = openPicker, modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            RsvpWordView(
                                word = state.currentWord,
                                fontSize = stageFontSize,
                                boxHeight = stageHeight,
                                placeholder = when {
                                    !state.hasBook -> stringResource(Res.string.reader_placeholder_loading)
                                    state.chapterWordCount == 0 -> stringResource(Res.string.reader_placeholder_empty_chapter)
                                    else -> stringResource(Res.string.reader_placeholder_ready)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = stagePadding)
                                    .pointerInput(state.currentChapterIndex) {
                                        detectTapGestures { viewModel.playPause() }
                                    }
                            )
                        }

                        PlayerControls(
                            state = state,
                            onPlayPause = viewModel::playPause,
                            onSeek = viewModel::seekToFraction,
                            onSkipWords = viewModel::skipWords,
                            onWpmChange = viewModel::changeWpm,
                            onPreviousChapter = viewModel::previousChapter,
                            onNextChapter = viewModel::nextChapter,
                            compact = compact,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = stagePadding)
                        )
                    }
                }
            }
    }
}

# HylSpeedReader — EPUB → RSVP speed reader

A **Kotlin Multiplatform / Compose Multiplatform** reader (Android + iOS) that
ingests local EPUB files through each platform's document picker and presents
their text with Rapid Serial Visual Presentation (RSVP) using a fixed focal
point. All shared logic lives in `composeApp`. Both platforms open books: Android
behind a platform codec (`java.util.zip` + platform XML DOM) and iOS behind a
dependency-free portable codec (pure-Kotlin ZIP + DEFLATE + XML) in
`commonMain`; only the file picker stays platform-specific.

| Requirement | Implementation |
|---|---|
| EPUB ingestion | `ui/rememberEpubPicker` (SAF OpenDocument on Android / `UIDocumentPickerViewController` on iOS) → `data/epub/EpubEngine` → Android `EpubParser` (java.util.zip + platform XML DOM) or iOS `PortableEpubParser` (pure-Kotlin ZIP + DEFLATE + XML) — no EPUB/parser library |
| Entry decoding | `data/epub/EpubCharset` (UTF BOM + XML-prolog encoding) |
| XML/entity layer | `data/epub/XmlDom` + `XmlEntities` — hardened `javax.xml` DOM, DOCTYPE-stripped, embedded entity table, XXE-safe |
| Clean text extraction | `data/epub/EpubTextExtractor` (platform-DOM block extraction, tolerant fallback) + `domain/logic/Tokenizer` |
| focus calculation | `domain/model/RsvpWord.focusIndexFor()` — standard focus-index length buckets |
| Word alignment & highlight | `ui/rsvp/RsvpWordView` — TextMeasurer-driven, pixel-exact focus centering |
| 200–1000 WPM | `domain/logic/RsvpTiming` + WPM slider in `ui/reader/PlayerControls` |
| Punctuation delays | `RsvpTiming.delayMs()` — sentence/clause/numeral/length/paragraph heuristics |
| Playback engine | `domain/logic/RsvpTicker` (pure cold `Flow<Int>`) driven by `ui/ReaderViewModel` on `Dispatchers.Default` |
| Reading position | `data/position/SettingsReadingPositionStore` (multiplatform-settings → SharedPreferences / NSUserDefaults) — restored on launch, flushed on pause/seek/chapter/every 25 words |
| UI (Compose, Material 3) | `ui/reader/ReaderScreen`, `PlayerControls`, `ChapterDrawer` (drawer), `EmptyState` |

## Build & run

- **Toolchain:** Gradle 8.9 / AGP 8.7.2 / Kotlin 2.0.21 / Compose Multiplatform 1.7.3.

### Android
1. Open the `sys-src` project folder in Android Studio (Koala or newer) and let it sync.
2. Run the `composeApp` configuration on a device/emulator with **minSdk 24**.
3. Tap **Open EPUB** and pick any local `.epub` file — no storage permission needed.

### iOS
1. Open `sys-src/iosApp/iosApp.xcodeproj` in Xcode on a Mac (the `composeApp`
   framework is built by the *Compile Kotlin Framework* build phase via
   `embedAndSignAppleFrameworkForXcode`).
2. Run on a simulator. The iOS EPUB codec is the shared pure-Kotlin portable
   codec, so opening a book works the same way as on Android.

### Tests
- JVM unit tests (domain, EPUB codec, reading position, architecture guards):
  `gradlew :composeApp:testDebugUnitTest`
- Instrumented Android tests (RSVP canvas, transport-bar accessibility, the
  `ReaderViewModel` state machine incl. position restore) need a connected
  device/emulator: `gradlew :composeApp:connectedDebugAndroidTest`.

Pre-push verification (from the `sys-src/` Gradle root):
`gradlew :composeApp:testDebugUnitTest :composeApp:lintDebug :composeApp:assembleDebug`.
CI runs those plus `assembleRelease`, and a second job runs
`connectedDebugAndroidTest` on an emulator (`.github/workflows/android.yml`).
Codemagic (`codemagic.yaml`) builds the iOS simulator `.app` and the Android
APKs.

## Architecture

Clean Architecture across a single KMP module (`composeApp`), split into
`commonMain` (shared), `androidMain` and `iosMain` (platform actuals):

```
 commonMain   ui (Compose, MVVM)   ui/…            StateFlow rendering + user events
             ├─ ReaderViewModel    ui/…            playback state machine + tick loop
             data (ports)          data/…          EpubBook/EpubEngine/DocumentHandle/Platform, SettingsReadingPositionStore
             data (portable codec) data/epub/portable/…  pure-Kotlin ZIP + DEFLATE + XML (used by iOS)
             domain (pure Kotlin)  domain/…        RsvpWord (focus), Tokenizer, Typography, RsvpTiming, RsvpTicker
androidMain  data codec            data/epub/…     SAF Uri → ZipFile → EpubSpine/OPF → XML DOM
             platform actuals      …               Platform, DocumentHandle, rememberEpubPicker, dynamic colour
iosMain      data codec            data/epub/…     PortableEpubParser over the shared portable codec
             platform actuals      …               MainViewController, picker, NSUserDefaults
```

The platform seam is `Platform` (expect object), `DocumentHandle` (interface),
`createEpubEngine()`/`persistedStringToDocumentHandle()` (expect funs),
`rememberEpubPicker` and `platformDynamicColorScheme` (composable expect funs).
`ReaderViewModel` is platform-free and takes its `EpubEngine`/`ReadingPositionStore`
injected; platform wiring lives in `ui/ReaderEntryPoint`.

## Tuning knobs

All pacing constants live in `domain/logic/RsvpTiming.kt`
(sentence/clause factors, long-word threshold, numeral factor, paragraph pause).
The punctuation table there is canonical ASCII, and each scanned character is
mapped through `domain/logic/Typography.canonical()` — so smart quotes,
guillemets and CJK/fullwidth marks are paced identically *without* being
rewritten on screen.
The focus bucket table lives in `domain/model/RsvpWord.focusIndexFor()`.

## Known limitations / roadmap

- Reading position, chapter and speed are persisted and restored on launch;
  a remembered book that can no longer be opened (removed file or revoked
  access) silently falls back to the empty state.
- DRM-protected or structurally broken EPUBs (missing container/OPF/spine) are
  rejected with a user-facing error; chapter entries are decoded using a UTF BOM
  or the declared XML encoding when present, and fall back to UTF-8 otherwise.
- **Two EPUB codecs, one shared reader** — Android uses `java.util.zip` +
  platform XML DOM in `androidMain`; iOS uses the dependency-free pure-Kotlin
  portable codec in `commonMain` (`data/epub/portable/`), selected by each
  platform's `createEpubEngine()` actual. Both platforms open books; the iOS
  target is configured but cannot be compiled on Windows/Linux (macOS only).
- The six transport glyphs are hand-authored vectors (`ui/theme/HylIcons.kt`);
  `material-icons-extended` (~35 MB of generated icons in the build) is not
  used.

## Resources & accessibility

- All user-facing strings live in
  `composeApp/src/commonMain/composeResources/values/strings.xml` (shared with
  iOS) and are read via `Res.string.*`; the Android manifest label is
  `@string/app_name` in `androidMain/res/values/strings.xml`.
- The launcher icon is a pure-vector adaptive icon
  (`androidMain/res/mipmap-anydpi-v26/`) with a `mipmap-anydpi/` fallback for API < 26.
- Every interactive control exposes a `contentDescription`; the scrub and speed
  sliders announce "Reading position" and "Reading speed".
- The scrub slider also carries a `stateDescription` (the word-count readout),
  and chapters use the Material 3 `NavigationDrawerItem` so the active chapter is
  announced as selected.
- The rapidly-flipping RSVP word is intentionally excluded from the
  accessibility tree (`clearAndSetSemantics`) — announcing ~17 words/second would
  be unusable — so the labelled transport controls carry the accessible
  interaction.

## Theming & layout

- `HylSpeedReaderTheme` uses wallpaper-derived dynamic color on Android 12+ (via
  the `platformDynamicColorScheme` expect/actual; iOS returns null) and a navy
  brand palette otherwise; `androidMain/res/values/colors.xml` +
  `values-night/colors.xml` keep `windowBackground` in step with the Compose
  surface for a clean edge-to-edge launch.
- The transport controls sit on a single elevated dock; the reading column is
  capped at `640.dp` and centred, and the RSVP stage expands to fill the
  available space (tablets, foldables, landscape).

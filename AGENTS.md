# AGENTS.md

Guidance for AI coding assistants working in the **HylSpeedReader** repository.

> **Verification Notice:** Always inspect the actual repository (`sys-src/settings.gradle.kts`, `sys-src/composeApp/build.gradle.kts`, `sys-src/composeApp/src/androidMain/AndroidManifest.xml`, and `sys-src/composeApp/src/`) before assuming package roots, Gradle module names, targets, or CI scripts. The root package is `eu.hylatwork.hylspeedreader`; there is no Gradle version catalog (dependency versions live in the build scripts).

---

## 1. Project Snapshot

**HylSpeedReader** is an EPUB reader using Rapid Serial Visual Presentation (RSVP) and fixed focal point alignment. It is a **Kotlin Multiplatform / Compose Multiplatform** app targeting **Android and iOS** from one shared codebase.

**Toolchain:** Gradle 8.9 (wrapper), AGP 8.7.2, Kotlin 2.0.21, **Compose Multiplatform 1.7.3** (`org.jetbrains.compose`), JetBrains AndroidX Lifecycle 2.8.4 (`org.jetbrains.androidx.lifecycle`), `multiplatform-settings` 1.1.1, `compileSdk` 35 / `minSdk` 24, iOS deployment target 15, and a JDK 17 Kotlin toolchain (`kotlin { jvmToolchain(17) }`; a local JDK 21 also works). Versions are pinned directly in `settings.gradle.kts` and `composeApp/build.gradle.kts` — there is no version catalog.

**Repository layout:** `sys-doc/` holds the problem-space (`REQUIREMENTS.md`) and solution-space (`ARCHITECTURE.md`) documentation under **CC BY 4.0**; `sys-src/` is the Gradle project under the **MIT** license, containing:

- `composeApp/` — the single KMP module (`androidTarget()` + `iosX64()/iosArm64()/iosSimulatorArm64()`, `isStatic = true`, framework `ComposeApp`).
- `iosApp/` — the Xcode wrapper (`iosApp.xcodeproj`, `iOSApp.swift`, `ContentView.swift`) that links `ComposeApp.framework` via `embedAndSignAppleFrameworkForXcode`.
- `sys-src/dist/` — release-APK drop-point; `codemagic.yaml` (repo root) — Codemagic CI/CD.

- **Data (`composeApp/src/*/kotlin/eu/hylatwork/hylspeedreader/data/`):** platform bridge `Platform` (expect/actual: EPUB engine, settings, logger, IO dispatcher); `data/epub/` = `EpubBook`/`EpubEngine`/`DocumentHandle` seams with the Android codec (SAF → cached copy → `java.util.zip` + platform `javax.xml` DOM, no third-party parser) in `androidMain` and a dependency-free portable codec in `commonMain` (`data/epub/portable/`: pure-Kotlin ZIP + DEFLATE + XML) used by the iOS engine; `data/position/SettingsReadingPositionStore` (multiplatform-settings — SharedPreferences / NSUserDefaults).
- **Domain (`.../domain/`, `commonMain`):** Pure Kotlin `Tokenizer`, `RsvpWord.focusIndexFor()` (standard focus-index length buckets), `RsvpTiming.delayMs()` (200–1000 WPM + punctuation/clause/numeral/length/paragraph heuristics), `RsvpTicker`.
- **UI (`.../ui/`, `commonMain`):** `ReaderViewModel` (cold `Flow<Int>` tick loop on `Dispatchers.Default`), `RsvpWordView` (Compose `TextMeasurer` focus centering), Material 3 screens (`ReaderScreen`, `PlayerControls`, `ChapterDrawer`, `EmptyState`); strings in `composeApp/src/commonMain/composeResources/values/strings.xml`.

---

## 2. Core Agent Behavior Rules

1. **Inspect before acting:** Read build configuration and relevant source files before modifying code.
2. **Never assume unverified artifacts:** Do not assume files, classes, Gradle tasks, or dependencies exist without checking the repository.
3. **Minimal, coherent diffs:** Make the smallest coherent change per task; one concern per commit.
4. **No unrelated refactoring:** Do not refactor outside the current task scope without explicit approval.
5. **Respect existing abstractions:** Do not introduce external EPUB libraries, heavy frameworks, or extra architectural layers unless explicitly requested.
6. **Preserve backward compatibility:** Keep public signatures and user-facing behavior intact unless breaking changes are explicitly required.
7. **Never edit generated files:** Do not modify `build/`, `sys-src/.kotlin/`, `.gradle/`, `.idea/`, `composeApp/build/`, or generated sources (including the Compose-resources `Res` class).
8. **Report verification honestly:** Run Gradle build/test commands when available and report exact failures rather than claiming unverified success. **Note:** iOS targets cannot be compiled on Windows/Linux — only on a macOS host (e.g. Codemagic); report iOS verification as "configured, not compiled" on non-macOS.

---

## 3. Documentation Maintenance & Boundaries

After every significant feature realization or structural change, both `sys-doc/REQUIREMENTS.md` and `sys-doc/ARCHITECTURE.md` must be kept up to date in the same commit/PR.

### Strict Separation of Problem Space and Solution Space

Maintain a strict separation between what the system needs to do and how it is built:

- **`sys-doc/REQUIREMENTS.md` (Problem Space):**
  - Describes **what** the system must achieve and **why**.
  - Contains functional and non-functional requirements, business rules, domain constraints (e.g., document-access boundaries, 200–1000 WPM bounds, deterministic focus rules), and acceptance criteria.
  - **Must remain implementation- and technology-agnostic.** Do not reference Kotlin libraries, specific XML/HTML parsers, Compose widgets, `ViewModel`, or internal packages here.
- **`sys-doc/ARCHITECTURE.md` (Solution Space):**
  - Describes **how** the system achieves those requirements.
  - Contains C4 architectural diagrams (Context, Container, Component), architectural decisions, concrete technologies, integrations, data models, dependency laws, and component tracking.
- **Rule:** Never mix the two. Do not prescribe technical solutions in `REQUIREMENTS.md`, and do not introduce business requirements implicitly through `ARCHITECTURE.md`. Keep both documents consistent and traceable to one another.

### Architecture Guard Test

HylSpeedReader enforces architectural integrity via an automated JVM unit test at `sys-src/composeApp/src/androidUnitTest/kotlin/eu/hylatwork/hylspeedreader/ArchitectureGuardTest.kt` (run from the `sys-src/` Gradle project root). Run the unit test suite — including that guard test — before every push:

```bash
./gradlew :composeApp:testDebugUnitTest
```

This verifies the layer boundaries (`domain/` has zero Android/UI/parser dependencies; `data/` has zero Compose/ViewModel/UI dependencies; `ui/reader`+`ui/rsvp` Composables never recompute focus/RSVP math and never import `data`), component wiring, and structural invariants remain synchronized with `sys-doc/ARCHITECTURE.md`. *(Keep the guard test in sync with new layers, source sets, or boundary changes rather than adding unverified dependencies.)* The guard hard-codes the root package (`eu/hylatwork/hylspeedreader`), the scanned composable roots (`ui/reader`, `ui/rsvp`), and the KMP source roots (`commonMain`/`androidMain`/`iosMain`), so renaming packages, adding source sets, or adding a composable package requires updating it.

---

## 4. Knowledge Graph (`graphify`)

If `graphify` is configured in this repository, a local knowledge graph of the codebase is generated into `graphify-out/` (`graph.json`, `GRAPH_REPORT.md`, `graph.html`) as a fast navigation aid. `graphify-out/` is generated and git-ignored, **except** the curated community labels (`.graphify_labels.json` and its `.graphify_labels.json.sig`), which are deliberately committed; never commit the rest of `graphify-out/`.

- **Strict Scoping:** Run `graphify` scoped **specifically to the source directory** (`sys-src/composeApp/src` in this repository — check `.graphifyignore` and repo layout first). `.graphifyignore` at the repository root excludes Gradle build outputs (`sys-src/composeApp/build/`, `sys-src/build/`, `sys-src/.gradle/`) and graphify's own `graphify-out/`, so the graph stays compact and fast. `GRAPHIFY_OUT` must be an **absolute** path to the git-ignored `graphify-out/` at the repository root — a relative value would write inside the target subdirectory instead:
```bash
# bash/zsh, from the repository root
GRAPHIFY_OUT="$PWD/graphify-out" graphify update sys-src/composeApp/src

# Windows cmd
set "GRAPHIFY_OUT=%CD%\graphify-out" && graphify update sys-src/composeApp/src
```
*(Or via skill: `/graphify sys-src/composeApp/src --update`; exclusions come from `.graphifyignore`, not `--exclude` flags.)*

- **When to Re-Index:** Re-index **only after structural refactors** (adding, removing, renaming, or re-wiring packages, modules, files, public classes/interfaces, or cross-layer call boundaries). Do **not** re-run `graphify` for routine bug fixes, test additions, or purely internal function/behaviour changes. When a refactor **renames or deletes** files/packages, a plain incremental `graphify update` can leave stale path-derived nodes behind, so do a **full rebuild** — but protect the curated labels first (see next bullet).
- **Protect curated labels:** The LLM-curated community names live **inside** `graphify-out/` (`graphify-out/.graphify_labels.json` plus its `.graphify_labels.json.sig`). Back those files up (along with `.graphify_labels.pending.json`) before deleting or rebuilding `graphify-out/`, then restore them afterwards, and regenerate with `graphify label` (needs `GEMINI_API_KEY`/`GOOGLE_API_KEY`) only when the cluster set changed. `___make-clean.cmd` intentionally never removes `graphify-out/`.
- **Querying:** When `graphify-out/graph.json` exists, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` for targeted architectural exploration before broad raw file grepping.

---

## 5. HylSpeedReader Coding & Domain Guardrails

- **EPUB & Text (`data/epub/` [androidMain], `domain/logic/Tokenizer` [commonMain]):** Open SAF URIs via `ContentResolver.openInputStream(uri)` (Android actual). Handle malformed ZIPs, missing OPF/spine entries, non-UTF-8 encodings, and Unicode punctuation (curly quotes, em-dashes, soft hyphens, NBSP) gracefully without crashing. Keep `Tokenizer` pure and deterministic and platform-agnostic.
- **Focus-Index & Timing (`domain/model/RsvpWord`, `domain/logic/RsvpTiming`):** Keep focus bucket math exclusively in `RsvpWord.focusIndexFor()` (`0 <= index < word.length`). Enforce `200..1000` WPM bounds and keep `delayMs()` free of Android/Compose APIs.
- **Playback (`ui/ReaderViewModel`):** Run the cold `Flow<Int>` tick loop on `Dispatchers.Default`; never block `Dispatchers.Main`. Prevent duplicate tick loops or stale coroutine jobs across WPM changes, seeks, and pause/resume transitions. Use the injected `EpubEngine`/`ReadingPositionStore`; never reference a platform type directly here.
- **Compose UI (`ui/rsvp/`, `ui/reader/`):** Use `TextMeasurer` in `RsvpWordView` to center the focus character visually and avoid redundant allocations per tick. Follow Material 3, hoist state to `ReaderViewModel`, collect via `collectAsStateWithLifecycle()`, keep user-facing strings in `composeApp/src/commonMain/composeResources/values/strings.xml`, and provide accessible `contentDescription` labels. Composables in `ui/reader`/`ui/rsvp` must not import `data` — platform wiring lives in `ui/ReaderEntryPoint` (`rememberReaderViewModel()`).
- **expect/actual:** Keep the platform seam small and explicit (`Platform`, `DocumentHandle`, `EpubEngine`, `rememberEpubPicker`, `platformDynamicColorScheme`). Every `expect` declaration needs an `actual` in **both** `androidMain` and `iosMain` or the iOS target won't link.
- **Naming / trademark:** Never (re)introduce the proprietary speed-reading brand name or its acronyms anywhere — code, comments, UI strings, or docs — as it is trademarked in this product space. Use the generic terms **RSVP**, **focus index**, and **focus character**, and keep the `Rsvp*` type names.

---

## 6. TDD, Git & Verification Workflow

- **Mandatory TDD:** Follow **Red $\rightarrow$ Green $\rightarrow$ Refactor**. Write a failing JVM unit test (`composeApp/src/androidUnitTest/`) or Compose UI test (`composeApp/src/androidInstrumentedTest/`) before modifying production code, and commit tests prior to or alongside the minimal implementation.
- **Naming & Style:** Follow official Kotlin conventions (4-space indent, `PascalCase` files/classes/Composables, `camelCase` functions/properties, `UPPER_SNAKE_CASE` constants, `<ClassName>Test.kt`).
- **Git & PRs:** Use Conventional Commits (`<type>(<scope>): <subject>`, $\le$ 72 chars; scopes: `epub`, `parser`, `tokenizer`, `focus`, `timing`, `playback`, `ui`, `reader`, `rsvp`, `kmp`, `ios`, `docs`, `ci`) and `kebab-case` branches (`<type>/<issue>-<desc>`). Never push directly to `main`, never force-push or rebase shared branches, and merge via `--no-ff`.
- **Pre-Push Verification** (run from the `sys-src/` Gradle project root):
  ```bash
  ./gradlew :composeApp:testDebugUnitTest
  ./gradlew :composeApp:lintDebug
  ./gradlew :composeApp:assembleDebug
  ./gradlew :composeApp:assembleRelease
  # macOS only:
  ./gradlew :composeApp:linkReleaseFrameworkIosArm64
  ```
  On Windows invoke the wrapper as `gradlew.bat` (or `.\gradlew.bat`) from `sys-src/`. GitHub Actions (`.github/workflows/android.yml`) runs the Android suite, and a second job runs the instrumented `:composeApp:connectedDebugAndroidTest` on an emulator. Codemagic (`codemagic.yaml` at the repo root) builds the iOS simulator `.app` (`.ipa` once Apple credentials are added) and the Android APKs. Note that `sys-src/gradlew` is committed without the executable bit, so CI restores it with `chmod +x` before invoking the wrapper.
- **Release signing:** There is no `signingConfig`, so `assembleRelease` produces `composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk` (debug: `composeApp-debug.apk`). The `copyReleaseApkToDist` task is finalized by `assembleRelease` and copies the built APK into `sys-src/dist/` (git-ignored except `.gitkeep`). Never commit a keystore or signing password; if signing is introduced, keep the keystore and `keystore.properties` out of version control (add them to `.gitignore`). For iOS, code-signing secrets live in the Codemagic UI, never in the repo.

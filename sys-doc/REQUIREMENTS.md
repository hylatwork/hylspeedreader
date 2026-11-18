# Requirements — HylSpeedReader

## 1. Purpose & Vision

**HylSpeedReader** is a mobile e-book reader designed to help users read local digital books rapidly and comfortably on small screens without eye-movement fatigue. By presenting text one word at a time via **Rapid Serial Visual Presentation (RSVP)** and anchoring each word on its **fixed focal point**, the application minimizes saccadic eye movements while preserving natural reading comprehension through punctuation- and structure-aware pacing.

---

## 2. Scope & Domain Constraints

- **DC-1 — Offline-First Local Reading:** The application operates on user-supplied local EPUB files without requiring cloud accounts, remote servers, or continuous network connectivity.
- **DC-2 — Platform Privacy & Storage Boundaries:** Access to user book files must respect platform-mediated file selection and scoped read permissions rather than broad filesystem access.
- **DC-3 — Single-Focus Reading Ergonomics:** Words must remain visually anchored at a fixed focal point so the reader's gaze does not shift horizontally between consecutive words of varying lengths.
- **DC-4 — Cross-Platform Parity (Android & iOS):** The same reader experience — import, focus-aligned RSVP playback, pacing, chapter navigation, and position persistence — must be offered on both Android and iOS from a single product, each using its own platform-mediated document picker and local storage.

---

## 3. Functional Requirements

### FR-1: Book Import
- **FR-1.1:** Users must be able to browse and select local `.epub` book files from device storage.
- **FR-1.2:** When no book is loaded, the application must present a clear empty state guiding the user to import a book.
- **FR-1.3:** Invalid, corrupted, empty, or unsupported files must be rejected gracefully with clear user-visible error feedback rather than terminating the application.

### FR-2: Book Structure & Content Extraction
- **FR-2.1:** The system must extract chapters in the authoritative reading order defined by the EPUB publication.
- **FR-2.2:** The system must extract readable narrative text while excluding non-content markup, scripts, styling metadata, and navigation boilerplate — including table-of-contents/landmark pages and other auxiliary documents that are not part of the linear reading order.
- **FR-2.3:** Structural paragraph boundaries must be preserved during extraction so reading cadence reflects natural document breaks.
- **FR-2.4:** Text extraction and word segmentation must handle Unicode characters, accented letters, curly quotes, em-dashes, ellipses, non-breaking spaces, and soft hyphens accurately.

### FR-3: Focus-Point Alignment
- **FR-3.1:** Every displayed word must have a deterministically calculated focus character index based on standard focus-index word-length rules.
- **FR-3.2:** The focus character must be visually highlighted and aligned at a fixed horizontal focal point regardless of total word length or prefix/suffix asymmetry.

### FR-4: RSVP Playback & Speed Control
- **FR-4.1:** Users must be able to start, pause, and resume RSVP word playback.
- **FR-4.2:** Users must be able to adjust the baseline reading speed continuously across the range of **200 to 1000 words per minute (WPM)**.
- **FR-4.3:** Speed adjustments made during active or paused playback must take effect smoothly without resetting reading progress or creating overlapping playback streams.

### FR-5: Context-Sensitive Pacing Heuristics
- **FR-5.1:** Word display durations must dynamically scale from the selected WPM baseline using natural reading cadence heuristics:
  - Sentence-ending punctuation pauses.
  - Clause-separating punctuation pauses.
  - Extended dwell time for numerals and long words.
  - Additional pause duration at paragraph boundaries.
  - Typographic/target-language punctuation variants (curly quotes, en/em dashes, ellipses, and equivalent full-width or CJK marks) must be paced equivalently to their ASCII counterparts, without altering the displayed text.

### FR-6: Chapter & Position Navigation
- **FR-6.1:** Users must be able to view the book's chapter list and jump directly to any chapter.
- **FR-6.2:** Users must be able to scrub or step through reading progress within the active book/chapter.
- **FR-6.3:** The application must surface the reader's current progress (chapter position and word position) and the active reading-speed value while a book is open.

### FR-7: Reading Position Persistence
- **FR-7.1:** The application must remember the reader's position (book, chapter, and word) and the selected reading speed across app restarts and process death, and resume there when the same book is reopened.
- **FR-7.2:** When a remembered book can no longer be opened (file removed or access revoked), the application must discard the stale position and present the empty state rather than an error.

---

## 4. Non-Functional Requirements

### NFR-1: Timing Precision & Responsiveness
- **NFR-1.1:** Playback scheduling and book parsing must never block the main user interface thread, even at 1000 WPM (60 ms baseline intervals) or when importing large multi-chapter EPUBs.
- **NFR-1.2:** Word rendering must avoid layout jitter or noticeable frame drops during rapid word transitions.
- **NFR-1.3:** Persisting reading progress must not block the user interface thread or interrupt playback.

### NFR-2: Reliability & Determinism
- **NFR-2.1:** Tokenization, focus calculation, and pacing delay calculations must be strictly deterministic for identical inputs.
- **NFR-2.2:** Malformed EPUB archives (missing manifest/spine entries, broken chapter references, non-UTF-8 encodings) must degrade gracefully without unhandled exceptions.

### NFR-3: Accessibility & Usability
- **NFR-3.1:** All interactive playback, speed, import, and chapter navigation controls must provide accessible labels, sufficient color contrast, and adequate touch target sizes. Content that changes too rapidly to be useful to assistive technology must not be announced on every update.
- **NFR-3.2:** Loading, empty, active-reading, and error states must be clearly distinguishable to the user.

### NFR-4: Visual Presentation & Form-Factor Adaptivity
- **NFR-4.1:** The application must present a consistent, legible reading presentation in both light and dark system themes, with a visual hierarchy in which the currently displayed word remains the single most salient element.
- **NFR-4.2:** The reading layout must adapt gracefully across phone, tablet, foldable, and landscape form factors — without clipped content, cramped controls, or drift of the fixed focal point.
- **NFR-4.3:** The installed application must present a recognisable launcher identity (product name and icon).

---

## 5. Acceptance Criteria

Each requirement is accepted when its externally observable outcome holds. (How these are verified technically belongs to the solution space.)

- **FR-1 (Import):** A user can pick a local `.epub`; a corrupt, empty, or unsupported file produces user-visible error feedback and the application keeps running; with no book loaded, an empty state invites import.
- **FR-2 (Structure & Extraction):** Chapters appear in publication reading order; table-of-contents/landmark and other non-linear documents are excluded; paragraph boundaries survive; Unicode, accented letters, curly quotes, em-dashes, ellipses, NBSP, and soft hyphens read correctly.
- **FR-3 (focus-point):** For any word length the focus index is deterministic and within `0 ≤ index < length`, and the highlighted focus character stays on the fixed focal axis regardless of word length or prefix/suffix asymmetry.
- **FR-4 (Playback & Speed):** Start/pause/resume behave; the speed control spans 200–1000 WPM continuously; changing speed during playback neither resets progress nor yields overlapping word streams.
- **FR-5 (Pacing):** Sentence-ending, clause-separating, numeral, long-word, and paragraph-boundary cases each dwell longer than the baseline; typographic/full-width variants are paced as their ASCII equivalents while the displayed glyphs stay unchanged.
- **FR-6 (Navigation):** The chapter list opens and jumps to any chapter; the reader can scrub/step within a chapter; current chapter/word position and the active reading speed are visible.
- **FR-7 (Persistence):** Book, chapter, word, and speed survive a restart and resume when the same book is reopened; a remembered-but-unopenable book falls back to the empty state with no error.
- **NFR-1 (Responsiveness):** Parsing and playback never block the UI thread (including at 1000 WPM); rendering does not jitter; progress saves never stall the UI.
- **NFR-2 (Reliability):** Identical inputs produce identical tokenization/focus-index/delay results; malformed archives degrade gracefully with no unhandled exception.
- **NFR-3 (Accessibility):** Every control exposes an accessible label, sufficient contrast, and an adequate touch target; rapidly changing content is not announced on every update; loading/empty/reading/error states are visibly distinct.
- **NFR-4 (Presentation):** Light and dark themes both read cleanly with the current word most salient; the layout holds on phone/tablet/foldable/landscape; the launcher shows the product name and icon.
# HylSpeedReader

Read your EPUB books faster, one word at a time, on your phone.

HylSpeedReader is a mobile e-book reader that shows text **one word at a time**
at a fixed spot on the screen, so your eyes stop jumping back and forth across
the page. You pick the pace, from a relaxed 200 to a brisk 1000 words per minute,
and let the book flow.

Everything happens on your device. No account, no sign-up, no uploading your
books anywhere.

> **In plain English:** instead of scanning a page line by line, you see a single
> word at a time — always centred at the same point — and the app decides how long
> each word stays based on punctuation and natural reading rhythm. That removes
> most of the eye movement that makes reading on a small screen tiring.

---

## Why it exists

Reading full pages on a phone means constant horizontal eye movement and
re-finding the start of every line. That is slow and tiring. Presenting words
singly at a **fixed focus point** removes nearly all of that motion, so you can
read longer and faster while still catching the rhythm of sentences.

## What you get

- **One-word-at-a-time reading** with the focus character held at a fixed
  horizontal point, so short and long words never make your gaze drift.
- **Speed you control** — a slider from 200 to 1000 WPM, adjustable any time,
  including mid-sentence, without losing your place.
- **Pacing that respects the text** — longer pauses at sentence ends, commas,
  numbers, long words, and paragraph breaks, so comprehension keeps up with speed.
- **Chapter navigation** — see the book's chapters and jump straight to any of them.
- **Scrub and step** — move forward or back by word, and see where you are
  (chapter and word count) and how fast you're going.
- **Pick up where you left off** — the book, chapter, word, and your chosen speed
  are remembered across app restarts.
- **Reads your files, only your files** — open a local `.epub` with your device's
  normal file picker; the app asks for no broad storage access.
- **Light and dark themes, and any size screen** — phones, tablets, foldables and
  landscape, using system dynamic colour where available.
- **No network required** — fully offline once a book is open.
- **Both Android and iOS** from one product, each using its own native file picker.

## How it works

The app reads the readable narrative text out of your EPUB in the order the book
intends, breaks it into words, and paces each word with simple, predictable
rules (end-of-sentence, commas, numbers, long words, paragraph breaks). Each word
is aligned on a standard focus point so it lands in the same place every time.
See [Requirements](sys-doc/REQUIREMENTS.md) for the full behavioural contract.

## Status

**Early.** This is an initial release of a small, focused reader. Core reading,
pacing, navigation, and position memory work on both platforms, but the project
is young and the API-free product surface may still change. It is not yet
published on Google Play or the App Store.

### Platform support

| Platform | Supported | Notes |
|---|---|---|
| Android | ✅ 7.0+ (API 24+) | Built and tested via Gradle; opens local EPUBs through the system picker |
| iOS | ✅ 15+ | Shares the same reader; builds from Xcode on a Mac |

## Getting started

There is no store download yet. Build and run from source.

### Android

```bash
cd sys-src
./gradlew :composeApp:assembleDebug        # gradlew.bat on Windows
```

Install `composeApp/build/outputs/apk/debug/composeApp-debug.apk` on your device
or emulator, then tap **Open EPUB** and pick any local `.epub` file — no storage
permission is requested.

Prerequisites: JDK 17 (or 21), Android SDK with `compileSdk` 35.

### iOS

```bash
open sys-src/iosApp/iosApp.xcodeproj
```

Run on a simulator or device from Xcode on macOS. Requires Xcode and the
`ComposeApp` Kotlin framework, which the Xcode build phase compiles automatically.

## Limitations and non-goals

Being upfront about what this is **not** saves you time:

- **EPUB only.** Other formats (PDF, MOBI, plain text) are not supported and are
  not planned.
- **DRM-protected books are not supported.** Books must be unencrypted, standard
  EPUB files.
- **Local files only.** There is no store, no cloud library, no syncing between
  devices, and no online accounts — by design.
- **One word at a time.** There is no paginated or side-by-side "normal" reading
  mode; the single-word focus view is the only view.
- **Reading speed is per book, not a global profile.** Your position and speed
  are remembered for the book you were reading.

Malformed or broken EPUB files are rejected with a clear message rather than
crashing the app.

## Privacy

Your books never leave your device. The app opens files through the operating
system's scoped file picker, reads them locally, and stores only your reading
position and speed on-device. There is no telemetry, no remote server, and no
account.

## Documentation

- [Requirements](sys-doc/REQUIREMENTS.md) — what the reader must do, and why.
- [Architecture](sys-doc/ARCHITECTURE.md) — how it is built.
- [Developer guide](sys-src/README.md) — building the shared Kotlin/Compose codebase.

## License

Code is released under the **MIT License** ([`LICENSE.sys-src.md`](LICENSE.sys-src.md));
documentation is licensed under **CC BY 4.0** ([`LICENSE.sys-doc.md`](LICENSE.sys-doc.md)).

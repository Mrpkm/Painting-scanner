# Photo Mosaic Scanner — Stage 1

A guided capture-grid assistant for photographing a large flat painting in
overlapping tiles, for later stitching in Hugin on Windows. This is Stage 1
only: **no** stitching, feature matching, OCR, auto-shutter, or AR tracking.

## ⚠️ Important: this project was not compiled in the environment that built it

I could not actually run Gradle or produce an APK here: this sandbox's
network allow-list does not include Google's Maven repository
(`dl.google.com` / `maven.google.com`), which is where the Android Gradle
Plugin, AndroidX, Compose, and CameraX all come from. Every dependency
resolution step would fail immediately, so I did not pretend to build it —
per the same "never give false precision" principle the app itself follows.

What this means practically:
- All source, Gradle config, and resources below are hand-written to be
  correct against Compose BOM `2024.06.00` / CameraX `1.3.4` / AGP `8.5.2`,
  but **have not been verified by an actual compiler**.
- When you open this in Android Studio (Koala/Ladybug or newer) and let it
  sync, it should build with little or no intervention. The handful of spots
  most likely to need a one-line tweak if your exact library versions differ
  slightly are called out in "If something doesn't compile" below.
- The Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is a binary
  file that couldn't be fetched here either. Android Studio will regenerate
  it automatically on first open/sync. If you're on the command line instead,
  run `gradle wrapper` once (with any local Gradle install) before `./gradlew`.

## What Stage 1 actually does

1. **Setup screen** — enter painting width/height (cm/mm/in), overlap % (with
   a warning outside 20–60%), and rows/columns. A "Suggest a starting grid"
   button offers a rough estimate (see caveat below) you can adjust; a live
   preview shows the numbered serpentine grid before you confirm.
2. **Camera screen** — live rear-camera preview (CameraX) with a translucent
   grid overlay. The current tile is highlighted, completed tiles are marked,
   and a plain-language movement hint ("Move right to the next tile") is
   shown — never a fake precise measurement.
3. **Capture** — a big CAPTURE / MARK COMPLETE button takes a full-resolution
   photo via CameraX (`ResolutionSelector.HIGHEST_AVAILABLE_STRATEGY`,
   `CAPTURE_MODE_MAXIMIZE_QUALITY`) and saves it with a deterministic name
   (`r01_c01.jpg`), then auto-advances to the next tile in serpentine order.
4. **Retake** — tap any tile (on the camera overlay or the overview grid) to
   jump the cursor to it. Recapturing writes a new versioned file
   (`r01_c01_v2.jpg`) rather than overwriting the original.
5. **Overview screen** — progress bar, completed/remaining counts, a tappable
   grid for jumping to any tile, and Continue / Finish / Start Over.
6. **Persistence** — every change is written to `session.json` next to the
   photos, and the active session's path is remembered in SharedPreferences,
   so closing and reopening the app resumes exactly where you left off (on
   the Overview screen).

## Where photos are saved — a deliberate trade-off

Photos and `session.json` are written to app-specific external storage:

```
Android/data/com.example.photomosaicscanner/files/Pictures/PhotoMosaicScanner/
    Painting_2026-09-25_001/
        session.json
        r01_c01.jpg
        r01_c02.jpg
        ...
```

This was chosen over the public `Pictures/` folder on purpose: writing an
arbitrary file like `session.json` into public storage on modern Android
requires either the deprecated `WRITE_EXTERNAL_STORAGE` permission or the
Storage Access Framework, both of which add real complexity for Stage 1.
App-specific storage needs **no runtime storage permission** and behaves
identically across Android versions, at the cost of the folder not showing
up in the Gallery app.

To get files onto your Windows PC for Hugin: connect the phone by USB and
browse to the path above with a file manager / Android File Transfer, or use
`adb pull <path> .`. If you'd rather have photos land directly in the public
Pictures folder, that's a reasonable Stage 2 change (MediaStore + a
one-time SAF folder grant) — flagged here rather than silently promised.

## What's deferred to Stage 2+ (per the brief)

- **Automatic grid sizing from real camera geometry.** `GridCalculator.suggestGrid()`
  exists, but it's an estimate based on an assumed capture footprint (35×25 cm),
  not a measurement of your phone's actual field of view at your working
  distance. It's clearly labeled as such in the UI. Manual entry is the
  trusted path in Stage 1.
- **Precise phone position / overlap measurement.** The app knows which tile
  you should shoot next and shows a qualitative direction; it does not know,
  and does not claim to know, your phone's physical position or the true
  overlap achieved. That requires the visual tracking work in Stage 2.
- **RAW/DNG capture.** Not implemented. Stable CameraX doesn't expose RAW
  through the standard `ImageCapture` API; adding it would need
  `Camera2Interop` and per-device verification.
- **Guaranteed vendor "108 MP mode."** The app requests the highest
  resolution CameraX itself can expose (`ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY`).
  Whether that reaches the Honor X8c's vendor-specific ultra-high-res sensor
  mode depends on what that mode exposes through Camera2/CameraX on this
  device — not something Stage 1 can guarantee.
- Stitching, OpenCV feature matching, auto-capture, blur/coverage detection,
  and Hugin project export are all explicitly Stage 2–4 work per the brief
  and are not present here.

## Architecture, for Stage 2 continuity

- `model/` — plain data classes (`ScanSession`, `TileCoordinate`, `TileRecord`,
  `PaintingUnits`), no Android or UI dependencies.
- `grid/GridCalculator` — pure geometry (serpentine order, grid-size estimate).
- `capture/` — `CaptureFileNaming` (deterministic, retake-safe filenames) and
  `PhotoCaptureController` (CameraX preview + still capture only). Stage 2
  can add an `ImageAnalysis` use case here, next to `ImageCapture`, without
  touching the capture path.
- `session/` — `SessionRepository` (JSON persistence via `org.json`, no
  extra dependency) and `SessionViewModel` (all state mutation + the
  qualitative `MovementHint` logic that Stage 2's real tracking will replace).
- `ui/` — Compose screens (`setup`, `camera`, `overview`) plus a shared
  `GridDiagram` component reused by all three, and a minimal Material3 theme.
- Navigation is a plain two-state `enum` switch in `MainActivity`, not a nav
  library — intentionally simple for a 3-screen app; swap in
  `navigation-compose` later if Stage 2 adds more screens.

## Building without Android Studio (recommended if your device can't run it)

This repo includes `.github/workflows/build-apk.yml`, which builds the debug
APK on GitHub's free cloud servers — your own device only needs a web
browser to trigger it and download the result.

1. Create a free account at [github.com](https://github.com) if you don't have one.
2. Create a new repository (either Public or Private both work; Public gets
   unlimited free Actions minutes, Private gets 2,000 free minutes/month,
   plenty for this).
3. On the new repo's page, use **Add file → Upload files** and upload the
   contents of this project (everything that was inside the
   `PhotoMosaicScanner` folder — `app/`, `gradle/`, `.github/`,
   `build.gradle.kts`, etc. — at the **root** of the repo, not nested one
   level deeper). Most desktop browsers let you drag the whole unzipped
   folder in at once; on mobile, or if drag-and-drop of a folder doesn't
   work for you, let me know what device you're using and I'll give you a
   more specific set of steps (e.g. using GitHub's `git` command line, the
   GitHub mobile app, or a browser-based Codespace instead).
4. Commit the upload.
5. Go to the **Actions** tab of your repo. A workflow run should already be
   in progress (it triggers on push); if not, click **Build Debug APK →
   Run workflow**.
6. Wait for it to finish (a few minutes), then open the completed run and
   download the **PhotoMosaicScanner-debug-apk** artifact — it's a zip
   containing `app-debug.apk`.
7. Transfer that APK to your Android phone (email it to yourself, use a
   cloud drive, or download directly if you're already on the phone's
   browser), then tap it to install. You'll need to allow "install unknown
   apps" for whichever app you used to open the file — Android will prompt
   you for this the first time.

If a build fails, open the failed step in the Actions log and paste me the
error — I can adjust the workflow or the Gradle config from there without
needing a working local build environment on your end at all.

## Building with Android Studio (alternative, if you get access to it later)

1. Install **Android Studio** (Koala 2024.1 or newer recommended).
2. `File → Open`, select the `PhotoMosaicScanner` folder, let it sync (this
   downloads the Gradle wrapper jar and all dependencies from Google's/Maven
   Central's servers — needs normal internet access).
3. Connect an Android device (API 26+, e.g. the Honor X8c) with USB
   debugging enabled, or use `Run ▶`.
4. Grant the camera permission when prompted on first launch of the camera
   screen.

### Command line

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Install
it with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### If something doesn't compile

Compose/Material3's API surface shifts slightly between versions. The parts
of this code most sensitive to that, in case your synced versions differ
from the ones pinned in `app/build.gradle.kts`:

- `Modifier.menuAnchor()` in `SetupScreen.kt` — some newer Material3
  releases require `menuAnchor(MenuAnchorType.PrimaryNotEditable)` instead
  of the no-arg version.
- `LinearProgressIndicator(progress = { progress }, ...)` in
  `OverviewScreen.kt` — older Material3 releases only have the direct
  `progress: Float` parameter (no lambda).
- `drawText(textLayoutResult, ...)` in `GridDiagram.kt` — requires
  `androidx.compose.ui.text.drawText`, part of Compose UI 1.4+.

Each is a one-line fix if Android Studio flags it.

## Manual test checklist

- [ ] Enter dimensions/overlap/grid, confirm, land on camera screen with grid overlay.
- [ ] Capture a tile, see it turn "completed" and the cursor auto-advance.
- [ ] Tap a completed tile to jump back, capture again, confirm a `_v2` file
      appears in the session folder rather than overwriting the original.
- [ ] Close the app mid-scan, reopen it, confirm it resumes on Overview with
      the same progress and current tile.
- [ ] Finish a session, confirm Setup screen appears for a new scan.

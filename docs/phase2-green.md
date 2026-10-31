# :tgui Compiles — Verified

**Date**: 2026-10-31
**Branch**: feat/phase2-vendor-tgui
**Donor**: DrKLO/Telegram @ `3de37c8cfb039452be7398909461da6288babf15`

## Result

```
:tgui:compileDebugJavaWithJavac   BUILD SUCCESSFUL
:tgui:assembleDebug               BUILD SUCCESSFUL
```

- **2,232 vendored Java files** compile.
- **14,263 classes** in `classes.jar`.
- AAR: `45,471,582` bytes (45 MB) at
  `tgui/build/outputs/aar/tgui-debug.aar`.

## Path to green (all measured)

| Step | Errors |
|------|--------|
| Vendored tree, no dependencies | 6,180 |
| + 20 `compileOnly` libraries | 1,971 |
| + zxing/gson/wallet/markwon/biometric/… | 519 |
| + stripe/wearable/strikethrough/media3-extractor | 387 |
| + MLKit stubs, minSdk 23 | 345 |
| + compileSdk 36 (the 37.0 jar is a trimmed stub) | 258 |
| + donor-exact library versions | 126 |
| + fork-API edits (media3, mp4parser, BuildConfig) | **0** |

The single biggest lever was **using the donor's own library versions** rather
than latest. `stripe-android:2.0.2` (not 20.x), `billing:8.0.0`,
`firebase-messaging:22.0.0`, `exifinterface:1.3.6`, and the
`play-services-mlkit-*` coordinates instead of `com.google.mlkit:*`.

## APK-leakage check (the rule #2 question)

Unpacked the AAR and listed `classes.jar`. Counts are for the forbidden
libraries the brief names:

| Library | Classes in AAR |
|---------|----------------|
| `com.google.android.gms` | **0** |
| `com.google.android.play.core` (Integrity) | **0** |
| `com.android.billingclient` | **0** |
| `com.stripe` | **0** |
| `com.google.firebase` | **0** |
| `com.google.mlkit` | **0** |
| `com.googlecode.mp4parser` | **0** |
| `com.google.android.recaptcha` | **0** |
| `com.google.zxing` | **0** |
| `io.noties` (markwon) | **0** |
| `org.commonmark` | **0** |
| `org.aspectj` | **0** |
| `androidx.media3` | **0** |
| `androidx.car` | **0** |

`compileOnly` behaves as intended: those libraries satisfy javac but contribute
nothing to the artifact.

### One violation remains

| Library | Classes in AAR |
|---------|----------------|
| `org.webrtc` | **396** |

Telegram **vendors its own `org/webrtc` Java sources** (173 files, 1.5 MB) in
`TMessagesProj`, rather than depending on a library. Because they are ordinary
Java sources in the copied tree, they compile into the AAR like any other code.

Related: `org/telegram/messenger/voip` (18 files, 424 KB). Together these are
**467 classes**.

### Why they were not simply deleted

Attempted and reverted. Removing both packages produced **2,379 errors** across
`LaunchActivity` (162), `LivePlayer` (129), `GroupCallPipAlertView` (96),
`RTMPStreamPipOverlay` (93), `FragmentContextView` (80), `ChatObject` (72) and
others. 45 files import them, including core classes (`MediaController`,
`ChatActivity`, `MessagesController`, `ApplicationLoader`). The tree was restored
from backup and rebuilds green in 34 s.

Removing VoIP therefore requires the same treatment as the earlier Stories/Stars
analysis: editing the 45 importing files, not deleting two directories.

## Fork-API edits made (donor files touched)

Kept deliberately small and each marked `TGDELTA:`. These are the only edits to
donor code:

| File | Change | Why |
|------|--------|-----|
| `tgnet/ConnectionsManager.java` | dropped `DefaultBandwidthMeter.onTransfer` call | fork-only API |
| `ui/Components/VideoPlayer.java` | removed `VideoListener` (iface + 3 overrides + 1 call) | removed upstream |
| `ui/Components/VideoPlayer.java` | `setWorkerQueue` no longer forwards | fork-only |
| `ui/Components/VideoPlayer.java` | `SurfaceNotValidException` branch dropped | fork-only type |
| `ui/Components/VideoPlayer.java` | `MediaCodecUtil.isHardwareAccelerated` → name heuristic | package-private upstream |
| `ui/Components/VideoPlayer.java` | HDR static info path disabled | `getCodecOutputMediaFormat` protected |
| `ui/Components/VideoPlayer.java` | `Format.documentId` → match on `format.id` | fork-only field |
| `messenger/MediaController.java` | live-photo XMP detection disabled | `MotionPhotoDescription` package-private |
| `messenger/video/HevcDecoderConfigurationRecord.java` | `NalUnitTypes`→`H265NalUnitTypes`, dropped shadowed import | renamed upstream |

Plus two generated-symbol replacements: `BuildConfig` and `LocalizationUtils`.

## Status

- **`:tgui` is green and produces an AAR.** This is build-verified, not claimed.
- **Not** integrated into the app: the root `DeltaCat` project still cannot
  configure on this machine because `google-services:4.5.0` JAR 502s on every
  reachable mirror. `:tgui` has never been linked into the Delta Chat APK.
- No APK exists, so no device run, no screenshots.
- `org.webrtc` (396 classes) is still a rule #2 violation in the artifact.

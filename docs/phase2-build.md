# Plan A — First Build-Verified Results

**Date**: 2026-10-30
**Branch**: feat/phase2-vendor-tgui

First time this work has been compiled. Supersedes the "not verified" caveats in
`phase2-closure.md` and `phase2-dependencies.md`.

## Toolchain (actual, on this machine)

| Component | Value |
|-----------|-------|
| JDK | Temurin **21.0.12.1+1** (`AppData/Local/Temp/tools/jdk-21.0.12.1+1`) |
| Android SDK | `Desktop/VibeCodes/android-sdk` (installed for this task) |
| Platform | `android-37.0` |
| Build Tools | `36.0.0` (37.0.0 was insufficient — AGP requests 36.0.0) |
| NDK | **not installed** — not needed for a pure-Java library |
| Gradle | 9.5.0 (wrapper) |
| AGP | 9.3.2 |

### Environment obstacle found: `dl.google.com` is sinkholed

`dl.google.com` resolves to **198.18.0.81** (a reserved benchmarking address) on
this machine, so every Google Maven and Android repository URL returns 404 —
including ones that definitely exist. This is a DNS-level interception, not a
missing artifact.

Workarounds used:
- Gradle repos → `https://maven.aliyun.com/repository/google`
- `sdkmanager` → `SDK_TEST_BASE_URL=https://mirrors.cloud.tencent.com/AndroidSDK/`
- `google()` is kept as a fallback in `settings.gradle` / `build.gradle` so the
  build still works on machines where the real host resolves.

### Second obstacle: `google-services` plugin is unobtainable

`com.google.gms.google-services:4.5.0` POM is on the mirror but the **JAR returns
502**, and no other reachable mirror has it. This blocks the root `DeltaCat`
project (which applies the plugin for the `gplay` flavor). It does **not** affect
`:tgui`, so a standalone verification project is used to compile the library:

```
Desktop/VibeCodes/tgui-verify/    # settings.gradle + build.gradle, includes ../DeltaCat/tgui
Desktop/VibeCodes/build-tgui.ps1  # sets JAVA_HOME/ANDROID_HOME, runs the build, logs
```

## Result: javac compiles the vendored tree

`./gradlew :tgui:compileDebugJavaWithJavac` now runs javac over the 2,230 vendored
files. It does not succeed, and stops at javac's 100-error default cap.

**Every one of the 100 reported errors is a missing third-party package** — not a
syntax error, not a missing Telegram class, not a resource problem. The vendored
code itself parses. That validates the closure approach: the code tree is
internally consistent; what is missing is the external dependency surface.

### Missing packages reported

| Package | Errors | Files importing it |
|---------|--------|--------------------|
| `com.coremedia.iso.*` | 13 | `messenger/video/{MP4Builder,Track,HevcDecoderConfigurationRecord}` |
| `androidx.mediarouter.app` | 8 | Chromecast UI |
| `com.google.zxing.common.detector` | 5 | `TelegramQRCodeWriter`, `CameraScanActivity` |
| `com.android.billingclient.api` | 5 | `BillingController`, `BuildVars`, `MediaDataController` |
| `com.google.mlkit.*` | 8 | `Components/Paint/Views/{PhotoView,StickerMakerView}` |
| `com.google.android.play.core.integrity` | 4 | `tgnet/ConnectionsManager`, `LoginActivity` |
| `androidx.webkit` | 4 | — |
| `com.google.android.gms.*` (cast/vision/tasks/auth) | 10 | `AndroidUtilities`, `ApplicationLoader`, `CaptchaController`, `chromecast/*` |
| `androidx.media3.*` (session, hls, dash) | 3 | — |
| `org.checkerframework.*` | 2 | `SlideIntChooseView`, bots, Business |
| `org.aspectj.lang.annotation` | 1 | `messenger/video/VideoAds` |

## What this means for the brief's rules

Hard rule #2 forbids Telegram network code, Firebase/GCM, and GMS/analytics SDKs.
The compile errors are concentrated in exactly those subsystems:

- **Billing** (`android.billingclient`) — out of scope, must be removed.
- **Play Integrity** (`play.core.integrity`) — GMS, forbidden; sits inside
  `ConnectionsManager`, which is also the MTProto network layer.
- **GMS** (cast, vision, tasks, auth) — forbidden; cast/vision are out of scope.
- **MLKit** — subject segmentation in the photo editor, out of scope.
- **Firebase** — forbidden (GCM push).
- **aspectj** — used only by `VideoAds`, an ad class that must not ship.

The practical path is **not** to add these dependencies. It is to delete the
owning subsystems, which is step 2 of the A1 plan. The compiler is now telling us
exactly which ones to remove, which is more precise than the earlier static
estimate.

## Status

- **Not green.** 100+ errors, truncated at the cap.
- No APK produced, nothing run on a device.
- Not yet measured: how many error groups appear once the cap is lifted and the
  out-of-scope packages are excised.
- The Android app module (`DeltaCat` root project) still cannot configure on this
  machine because of the unobtainable `google-services` plugin JAR.

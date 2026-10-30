# A3 Surgery — Progress Log

**Branch**: feat/phase2-vendor-tgui
**Donor**: DrKLO/Telegram @ `3de37c8cfb039452be7398909461da6288babf15`

Goal: make the vendored tree compile **without shipping** any of the libraries
the brief forbids (GMS, Play Integrity, Firebase, billing, MLKit, Stripe,
WebRTC, MP4Parser, cast, ads).

## Approach chosen

The brief says "prefer stubbing at singleton boundaries over editing donor files
line by line; keep donor edits minimal". Two mechanisms are used, in this order:

1. **`compileOnly` dependencies** for third-party libraries. They go on the javac
   classpath but contribute **no classes and no native code to the APK**. This is
   the cheapest way to satisfy rule #2 (nothing forbidden ships) without touching
   29,615-line donor files.
2. **Hand-written compile-only stubs** for the two artifact coordinates that are
   not published on any reachable mirror (MLKit subject segmentation and image
   labelling). These are tiny and marked `TGDELTA:`.

A third mechanism was **tried and rejected**: deleting out-of-scope packages and
letting the cascade remove dependents. Measured result — 470 files deleted, error
count went 6,180 → 21,261, because 289 surviving files import the deleted
packages and `ChatMessageCell`/`DialogCell` themselves reference Stories, Stars
and Premium. See `phase2-delete-cascade.md`. The tree was restored.

## Measured progress (all from real Gradle runs)

| Step | Errors |
|------|--------|
| Vendored tree, no deps | 6,180 |
| + 20 `compileOnly` libraries | 1,971 |
| + zxing/gson/wallet/recaptcha/biometric/markwon/com… | 519 |
| + stripe/wearable/strikethrough/media3-extractor | 387 |
| + MLKit stubs, minSdk 23 | 345 |
| + compileSdk 36 (android-37.0 jar is a trimmed stub) | **258** |

### Environment findings worth keeping

- **`dl.google.com` is DNS-sinkholed** on this machine (`198.18.0.81`). All
  Google Maven and Android repository URLs 404. Worked around with the Aliyun
  Google mirror and the Tencent SDK mirror; `google()` kept as fallback.
- **`google-services:4.5.0` JAR returns 502** everywhere reachable, so the root
  `DeltaCat` app module cannot configure here. `:tgui` is compiled through
  `VibeCodes/tgui-verify/`, a standalone project that includes the module.
- **`platforms/android-37.0/android.jar` is a trimmed stub** (~15k classes, no
  `android.hardware.fingerprint`). `android-36` is complete enough. The module
  now builds against 36.
- **Build Tools 36.0.0** is what AGP wants; 37.0.0 alone is not enough.
- NDK is **not** installed and **not** needed to compile a pure-Java library.

## Remaining errors (258)

Concentrated in a handful of files that are all out of scope:

- `com.stripe.android.net/model/exception` — `PaymentFormActivity`
- `androidx.media3.extractor.jpeg.MotionPhotoDescription` — media metadata
- `com.google.mlkit.nl.languageid` — text recognition (a third MLKit surface)
- `Action` / `static import only from classes and interfaces` — a handful of
  static imports that resolve to missing members

## Status

- **Not green.** 258 errors.
- No AAR, no APK, nothing run on a device.
- The `compileOnly` list is currently **unverified for APK leakage** — the check
  (`apkanalyzer files list`) needs an APK that does not exist yet.

## Next steps

1. Stub `languageid` (same pattern as the other MLKit stubs).
2. Resolve `MotionPhotoDescription` — either add
   `androidx.media3:media3-extractor` at the right version or stub it.
3. Strip the Stripe path from `PaymentFormActivity`/`PaymentInfoCell`, or stub
   the three `com.stripe.android.net/*` types (payments are out of scope).
4. Chase the `static import only from classes and interfaces` group — these are
   usually a stub that declares a field where donor code expects a static method.
5. Once green: build the AAR, then prove `compileOnly` stays out of the artifact.

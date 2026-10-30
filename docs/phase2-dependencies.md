# Plan A — Third-Party Dependency Surface of the Vendored Tree

**Date**: 2026-10-30
**Branch**: feat/phase2-vendor-tgui
**Donor**: DrKLO/Telegram @ `3de37c8cfb039452be7398909461da6288babf15`

Method: copied the donor's entire `TMessagesProj/src/main/java` (2,230 files) and
`TMessagesProj/src/main/res` (5,266 files, 33 MB) into `tgui/`, then extracted
every non-`android*` / non-`java*` import across the tree.

## External libraries referenced by the vendored code

| Library | Import sites | Status under brief rules |
|---------|--------------|--------------------------|
| `com.google.android.*` (GMS, Play Core, Play Integrity) | 124 | **Forbidden** — Play Integrity/GMS pulled in by `ConnectionsManager` |
| `com.google.firebase.*` | 8 | **Forbidden** — "no Firebase/GCM from Telegram" |
| `com.google.mlkit.*` | 13 | Out of scope (text recognition in photo editor) |
| `com.stripe.android.*` | 8 | Out of scope (payments) |
| `org.webrtc.*` | 35+ | Out of scope (calls) |
| `com.google.zxing.*` | 49 | Tolerable, but Delta already ships `zxing-android-embedded` |
| `com.coremedia.iso.*`, `com.googlecode.mp4parser.*` | 50 | Out of scope (video transcoding) |
| `org.commonmark.*` | 31 | Out of scope (Instant View / article rendering) |
| `com.google.gson.*` | 23 | Tolerable |
| `org.checkerframework.*` | 11 | Compile-time annotations only |
| `org.json.*` | 73 | Platform class, fine |

## Why this matters

The brief's hard rule #2 says the final app must have **no** Telegram network
code, no Firebase/GCM from Telegram, and no analytics/crash SDKs from Telegram's
tree.

A full copy of the donor source set brings all of the above in as **compile-time
dependencies**, even if the code paths are never executed. To get `:tgui` to
compile as-is, `build.gradle` would have to declare:

```gradle
implementation 'com.google.android.gms:play-services-base:...'
implementation 'com.google.android.play:integrity:...'
implementation 'com.google.firebase:firebase-messaging:...'
implementation 'com.google.mlkit:text-recognition:...'
implementation 'com.stripe:stripe-android:...'
implementation 'org.webrtc:google-webrtc:...'
implementation 'com.googlecode.mp4parser:isoparser:...'
implementation 'com.atlassian.commonmark:commonmark:...'
```

Declaring them compiles, but it also ships them in the APK. That is a direct
conflict with the brief.

## Consequence for the A1 plan

A1 ("copy everything, get green, then delete") is not one step but three:

1. **Copy + declare deps → green.** Establishes a working baseline, but the
   module currently violates hard rule #2.
2. **Delete out-of-scope packages.** `Stories`, `Stars`, `Wallet`, `Business`,
   `bots`, `iv`, `web`, `voip`, `Paint`, `Premium` — ~206k lines.
3. **Re-green and drop the now-unused dependencies.** Verify with a
   `gradle :tgui:dependencies` diff and an APK contents check.

Only after step 3 does the module satisfy the brief. Step 2 is where the real
labour is, and it is invisible in a file count.

## Note on `ConnectionsManager`

The stub I wrote earlier was premature. Under A1 the unmodified donor file is
what establishes the baseline; replacing it with a no-op is a *step 3* activity,
and doing it early hides compile errors rather than fixing them.

## Status

- `:tgui` contains 2,230 Java files + 33 MB of resources.
- **Does not compile.** No JDK or Android SDK on this machine
  (`java: command not found`), so the dependency list above is derived
  statically from imports, not from a failed Gradle run.
- No APK produced. No runtime behaviour observed.

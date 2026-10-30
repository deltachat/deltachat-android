# Third-Party Code and Assets

## Vendored Telegram UI (`tgui/`)

| Field | Value |
|-------|-------|
| Source repo | https://github.com/DrKLO/Telegram |
| Upstream commit | `3de37c8cfb039452be7398909461da6288babf15` |
| Upstream commit subject | `update to 13.1.1 (7237)` (2026-10-25) |
| Module vendored from | `TMessagesProj` |
| Destination | `tgui/src/main/java/`, `tgui/src/main/res/` |
| Files copied | 2,230 Java files; 5,266 resource files (33 MB) |
| Changes made | None at time of copy — files are byte-identical to upstream |

### License of vendored code

Telegram for Android is **GPL v2 or later**. Original license headers have been
kept intact in every vendored file. Do not strip them.

The vendored tree is **not** license-uniform. Header survey over the copied files:

| Header | Files |
|--------|-------|
| `It is licensed under GNU GPL v. 2 or later.` | 427 |
| `Copyright Nikolai Kudashov, 2013-2018.` | 409 |
| `This is the source code of Telegram for Android v. 5.x.x.` | 349 |
| `Copyright 2018 The Android Open Source Project` | 38 |
| `This is the source code of Telegram for Android v. 5.x.x` (no trailing period) | 32 |
| `Copyright © Vyacheslav Krylov 2014` | 22 |
| `Copyright 2013-2014 Odysseus Software GmbH` | 21 |
| `This is the source code of Telegram for Android v. 1.3.x.` | 18 |
| `This is the source code of Telegram for Android v. 2.0.x.` | 15 |
| `Copyright (C) 2015 The Android Open Source Project` | 14 |
| `Copyright Nikolai Kudashov, 2013-2020.` | 9 |
| `This is the source code of Telegram for Android v. 6.x.x.` | 6 |

Two distinct upstream licenses are present:

- **GPL v2 or later** — Telegram's own code (the majority).
- **Apache License 2.0** — files derived from AOSP (the `androidx.recyclerview`
  fork, and several `Components` classes) and from Odysseus Software GmbH
  (`org.telegram/messenger/audioinfo/**`).

The remaining ~1,800 files have no recognizable license header. These need an
individual audit before any distribution; they are not cleared by this document.

### ⚠️ This build must not be published

The vendored tree references Apple-style emoji bitmaps whose licensing does not
permit redistribution. Additionally, the license audit above is incomplete.
**Treat every artifact built from this repository as private and non-distributable.**

## Dependencies introduced by the vendored tree

The full donor source set references libraries that the project brief forbids
shipping (GMS, Play Integrity, Firebase, MLKit, Stripe, WebRTC, MP4Parser,
commonmark). See `docs/phase2-dependencies.md` for the measured list and for the
plan to remove them. **As of this commit the `:tgui` module does not yet satisfy
the brief's network/SDK constraints.**

## Delta Chat Android (fork base)

| Field | Value |
|-------|-------|
| Source repo | https://github.com/deltachat/deltachat-android |
| License | GPL v3 or later |
| Core submodule | `jni/deltachat-core-rust` → https://github.com/chatmail/core |
| Core commit | `706ee7a48ca509229bb75c836c00a4268e095a02` |

## License compatibility note

Delta Chat Android is **GPL v3+**. Telegram for Android is **GPL v2 or later**.
GPLv2-or-later code can be combined with GPLv3 under the "or later" option. The
Apache-2.0 files are one-way compatible into GPLv3. This is believed compatible,
but it has not been reviewed by a lawyer and should be before this code is
distributed in any form.

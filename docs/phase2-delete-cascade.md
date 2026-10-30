# Plan A — The Delete Cascade (build-verified)

**Date**: 2026-10-30
**Branch**: feat/phase2-vendor-tgui

Measuring what actually happens when the out-of-scope Telegram subsystems are
removed. All numbers below are from real Gradle/javac runs, not estimates.

## Experiment

1. Compile the full 2,230-file tree, javac error cap lifted to 20,000.
   → **6,180 errors.**
2. Delete the 11 out-of-scope packages (~470 files, ~287k lines):
   `Stories`, `Stars`, `Wallet`, `iv`, `web`, `bots`, `Business`,
   `Components/voip`, `messenger/voip`, `Components/Paint`, `Components/Premium`.
   Tree drops to **1,760 files**.
3. Recompile.
   → **21,261 errors** — 3.4× *worse*.

## Why errors increased

Deleting a package breaks every caller. Measured importers of the deleted packages:

| Deleted package | Files importing it |
|-----------------|--------------------|
| `ui.Stories` | 168 |
| `ui.Components.Premium` | 97 |
| `ui.Stars` | 61 |
| `ui.Components.voip` | 32 |
| `messenger.voip` | 29 |
| `ui.bots` | 24 |
| `ui.Business` | 23 |
| `ui.Wallet` | 19 |
| `ui.Components.Paint` | 13 |
| `ui.iv` | 12 |
| `ui.web` | 11 |
| **Union** | **289 files** |

## The decisive finding: the entry points depend on the out-of-scope features

`ChatMessageCell.java` and `DialogCell.java` — the two files this whole plan
exists to vendor — **directly reference** the subsystems the brief says to hide:

`ChatMessageCell` references:
- `ui.Stars.{StarGiftPatterns, StarGiftSheet, StarsController, StarsIntroActivity, StarsReactionsSheet}`
- `ui.Stories.{StoriesUtilities, StoryViewer}`
- `ui.Stories.recorder.{CaptionContainerView, DominantColors}`
- `ui.Components.Premium.boosts.{BoostCounterSpan, cells.msg.GiveawayMessageCell, cells.msg.GiveawayResultsMessageCell}`

`DialogCell` references:
- `ui.Stars.StarsIntroActivity`
- `ui.Stories.{StoriesListPlaceProvider, StoriesUtilities, StoryViewer}`
- `ui.Components.Premium.PremiumGradient`

These are not incidental — they are in the import list of the vendored UI cells
themselves. `ChatMessageCell` literally renders giveaway/star-gift bubbles and
story viewers inline.

## What this invalidates

The brief assumes a clean split: vendor the chat UI, delete the Telegram features
that have no Delta Chat equivalent. The measurement shows **there is no such
seam**. "Telegram's chat bubble" as shipped in 13.1.1 includes premium gift
bubbles, star reactions, story viewers, and boost counters. Removing them means
editing `ChatMessageCell.java` (29,615 lines) and `DialogCell.java` (6,550 lines)
directly — which is exactly Plan A3, the option previously rejected as
"painful upstream syncing".

So Plan A1 ("copy everything, green it, then delete") does not terminate in the
shape the brief wants. It terminates in A3 by necessity.

## Options, restated with data

**A1-extended**: keep Stories/Stars/Premium/etc. in the tree purely so the cells
compile, and never invoke them at runtime. Cheapest to build, but ships ~287k
lines of Telegram premium/story code plus their dependency surface (GMS, billing,
MLKit, WebRTC…) — a direct violation of the brief's hard rule #2.

**A3 (now apparently mandatory for a compliant build)**: edit `ChatMessageCell`
and `DialogCell` to strip the references, then re-close. Large, ongoing edits
inside two huge files; every upstream sync re-opens the conflict.

**B**: stop vendoring. Restyle Delta Chat's own `ConversationItem` /
`ConversationListItem`. The dependency surface measured here does not exist,
because none of these classes are involved.

## Status

- Tree restored to 2,232 files after the experiment.
- Build is **not** green under any option.
- No APK, no device run.
- Root `DeltaCat` project still cannot configure here (google-services JAR 502).

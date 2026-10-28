# Phase 0: Recon and Feasibility Report

**Date**: 2026-10-28  
**Branch**: docs/phase0-recon  
**Author**: AI Agent

---

## 1. Build Environment Verification

### Delta Chat Android (base repo)
- **Repository**: https://github.com/deltachat/deltachat-android (cloned with `--recursive`)
- **Core submodule**: `jni/deltachat-core-rust` (chatmail/core@706ee7a)
- **Package structure**:
  - UI: `org.thoughtcrime.securesms.*` (descended from Signal's UI)
  - JNI bindings: `com.b44t.messenger.*` (`DcContext`, `DcChat`, `DcMsg`, `DcContact`, `DcEventEmitter`, `DcLot`)
  - RPC types: `chat.delta.rpc.types.*`

### Build Toolchain (from Dockerfile / BUILDING.md)
| Component | Version |
|-----------|---------|
| JDK | 17 (openjdk-17-jdk) |
| Android SDK | cmdline-tools (latest) |
| NDK | r27 LTS (27.0.11902837) |
| Gradle | 9.3.2 (via wrapper) |
| compileSdk | 37 |
| minSdk | 21 |
| targetSdk | 37 |
| Rust | via rustup (toolchain in `scripts/rust-toolchain`) |

### Build Commands
```bash
# Native build (all architectures)
scripts/ndk-make.sh

# Java build
./gradlew assembleDebug
```

**Status**: Repository cloned successfully. Submodule present. Build not yet attempted (no Android SDK/NDK on this machine).

---

## 2. Delta Chat UI Mapping

### Key UI Classes to Replace

| Area | Delta Chat Class | Lines | Responsibility |
|------|-----------------|-------|----------------|
| Chat List (row) | `ConversationListItem.java` | 432 | Single row in conversation list |
| Chat List (adapter) | `ConversationListAdapter.java` | 193 | RecyclerView adapter for list |
| Chat List (fragment) | `ConversationListFragment.java` | 375 | List fragment, search, account switcher |
| Chat List (activity) | `ConversationListActivity.java` | 772 | Top-level activity, FAB, toolbar |
| Chat Screen (item) | `ConversationItem.java` | 1142 | Individual message bubble |
| Chat Screen (adapter) | `ConversationAdapter.java` | 483 | RecyclerView adapter for messages |
| Chat Screen (fragment) | `ConversationFragment.java` | 1300 | Message list, input bar, events |
| Chat Screen (activity) | `ConversationActivity.java` | 2214 | Activity wrapper, media viewer, webxdc |

**Total Delta Chat UI to replace: ~6,911 lines**

### Data Objects (JNI layer - DO NOT MODIFY)
- `DcMsg` - message with getId(), getText(), getTimestamp(), getType(), getState(), getChatId(), getFromId(), getFile(), getQuotedMsg(), isForwarded(), isEdited(), isOutgoing(), isSecure(), showPadlock()
- `DcChat` - chat with isMultiUser(), isSelfTalk(), isContactRequest(), getVisibility() (pinned/archived), isMuted()
- `DcContact` - contact with getDisplayName(), wasSeenRecently()
- `DcContext` - main entry point, event emitter
- `DcLot` - summary for list items (state, snippet)

### Theming
- Uses `ThemeUtil.getThemedColor()` with attrs like `conversation_list_item_unread_color`, `conversation_list_item_background`, `pinned_list_item_background`
- Material Components + AppCompat
- Light/dark theme support

---

## 3. Telegram Donor Analysis

### Repository
- **Source**: https://github.com/DrKLO/Telegram (cloned shallow, default branch)
- **Module**: `TMessagesProj`
- **License**: GPL v2 or later (verify per file)

### Key UI Files (Lines of Code)

| File | Lines | Purpose |
|------|-------|---------|
| `org/telegram/ui/Cells/DialogCell.java` | 6,550 | Chat list row (avatar, name, snippet, date, badges, mute, archive, pins) |
| `org/telegram/ui/Cells/ChatMessageCell.java` | 29,615 | Message bubble (text, media, replies, forwards, reactions, ticks, date chips, grouping) |
| `org/telegram/ui/ActionBar/Theme.java` | 10,159 | Complete theming system (colors, dark/light, accents) |
| `org/telegram/messenger/Emoji.java` | 1,047 | Emoji rendering, Apple-style bitmaps |
| `org/telegram/ui/Components/*` | ~50+ files | AnimatedFloat, AvatarDrawable, MessageBackgroundDrawable, ReactionsLayoutInBubble, etc. |

**Total Telegram UI to vendor (core): ~47,371 lines**

### Dependency Report: ChatMessageCell.java Imports

#### Network/Backend Dependencies (MUST STUB OR REMOVE)
| Singleton | Purpose | Action |
|-----------|---------|--------|
| `MessagesController` | Chat/user data, dialogs | Stub with Delta Chat adapter |
| `ConnectionsManager` | MTProto network layer | **REMOVE** - no Telegram network |
| `FileLoader` | Media download from Telegram CDN | Stub with local file loader |
| `MediaDataController` | Media metadata | Stub |
| `DownloadController` | Download management | Stub |
| `MediaController` | Media playback/gallery | Keep for local media |
| `NotificationCenter` | In-process event bus | Keep as-is (pure Java) |
| `UserConfig` | Current user config | Stub with Delta Chat account |
| `AccountInstance` | Multi-account | Map to Delta Chat accounts |
| `ContactsController` | Contact list | Stub |
| `ChatObject` | Chat utilities | Stub |
| `DialogObject` | Dialog utilities | Stub |

#### Native Libraries (MUST REMOVE)
- `tgnet` (MTProto) - **REMOVE entirely**
- Any `.so` files in `TMessagesProj/jni/` and `TMessagesProj/libs/`

#### Telegram-Specific Features (REMOVE/STUB)
- Stories (`org.telegram.ui.Stories.*`, `TL_stories`)
- Premium/Stars (`org.telegram.ui.Stars.*`, `TL_stars`)
- Bots/Inline keyboards (`BotInlineKeyboard`, `TL_keyboard`)
- Video calls/VoIP (`org.telegram.messenger.voip.*`, `OldVideoPlayerRewinder`)
- Instant View (`ArticleViewer`, `CodeHighlighting`)
- Payments, Ads, Sponsored messages

#### Usable Without Network (KEEP WITH MINIMAL STUBS)
- `Theme` - complete theming system
- `AnimatedFloat`, `AnimatedEmojiDrawable`, `AnimatedEmojiSpan` - animations
- `AvatarDrawable`, `AvatarsDrawable` - avatar rendering
- `MessageBackgroundDrawable` - bubble shapes with tails
- `ReactionsLayoutInBubble` - reaction rendering
- `RLottieDrawable` - Lottie animations
- `Text`, `StaticLayoutEx` - text layout
- `ImageReceiver` - image loading (stub FileLoader)
- `Emoji` / `AnimatedEmojiSpan` - emoji rendering
- `LocaleController` - string formatting
- `AndroidUtilities` - dp/px, device info

---

## 4. Field Mapping: Delta Chat → Telegram UI

### Chat List: DcMsg/DcChat/DcLot → TLRPC.Dialog + TLRPC.Message + TLRPC.User/Chat

| Telegram UI Field | Delta Chat Source | Notes |
|------------------|-------------------|-------|
| `dialog.id` | `DcChat.getId()` | int → long |
| `dialog.unread_count` | `ThreadRecord.getUnreadCount()` | |
| `dialog.unread_mark` | `DcChat.getVisibility() == ARCHIVED` | Archived = true |
| `dialog.pinned` | `DcChat.getVisibility() == PINNED` | |
| `dialog.draft` | Not directly available | Skip or stub |
| `message.text` | `DcMsg.getText()` / `getSummaryText()` | |
| `message.date` | `DcMsg.getTimestamp()` | seconds vs millis - verify |
| `message.out` | `DcMsg.isOutgoing()` | |
| `message.media` | `DcMsg.getType()` → map to TLRPC.MessageMedia | See media table |
| `message.reply_to` | `DcMsg.getQuotedMsg()` → getId() | |
| `message.fwd_from` | `DcMsg.isForwarded()` + `getOverrideSenderName()` | |
| `message.views` | N/A (Telegram channel feature) | Always 0 |
| `message.edit_date` | `DcMsg.isEdited()` ? timestamp : 0 | |
| `user.id` / `chat.id` | `DcMsg.getFromId()` / `DcChat.getId()` | |
| `user.first_name` / `chat.title` | `DcContact.getDisplayName()` / `DcChat` name | |
| `user.photo` / `chat.photo` | Avatar via `DcContext` blob | Load via FileLoader stub |
| `message.send_state` | `DcMsg.getState()` → map to ticks | See ticks table |
| `message.reactions` | `DcMsg.getReactions()` (RPC) | Map to TLRPC.Reaction |

### Message State → Ticks Mapping

| Delta State | Telegram Visual | Description |
|-------------|-----------------|-------------|
| `DC_STATE_OUT_PREPARING` (18) | Clock/spinner | Preparing to send |
| `DC_STATE_OUT_PENDING` (20) | Single tick (✓) | Sent to server |
| `DC_STATE_OUT_FAILED` (24) | Red exclamation | Failed |
| `DC_STATE_OUT_DELIVERED` (26) | Double tick (✓✓) | Delivered |
| `DC_STATE_OUT_MDN_RCVD` (28) | Blue double tick | Read |
| `DC_STATE_IN_FRESH` (10) | Bold in list | Unread incoming |
| `DC_STATE_IN_SEEN` (16) | - | Read incoming |

### Media Type Mapping

| Delta Type | TLRPC.MessageMedia | Notes |
|------------|-------------------|-------|
| `DC_MSG_TEXT` (10) | `messageMediaEmpty` / web page | Link preview if URL |
| `DC_MSG_IMAGE` (20) | `messageMediaPhoto` | |
| `DC_MSG_GIF` (21) | `messageMediaDocument` (gif) | |
| `DC_MSG_STICKER` (23) | `messageMediaDocument` (sticker) | |
| `DC_MSG_AUDIO` (40) | `messageMediaDocument` (audio) | |
| `DC_MSG_VOICE` (41) | `messageMediaDocument` (voice) | Round waveform |
| `DC_MSG_VIDEO` (50) | `messageMediaDocument` (video) | |
| `DC_MSG_FILE` (60) | `messageMediaDocument` (file) | Generic file |
| `DC_MSG_WEBXDC` (80) | `messageMediaDocument` (webxdc) | Custom attachment card |
| `DC_MSG_VCARD` (90) | `messageMediaContact` | vCard |
| `DC_MSG_CALL` (71) | N/A | Calls not in scope |

---

## 5. Plan A vs Plan B Assessment

### Plan A: Vendor Telegram UI (`:tgui` module)

**Pros:**
- Pixel-perfect Telegram look by construction
- Reuses battle-tested bubble grouping, tail logic, RTL, animations
- Emoji rendering, reactions, animated stickers already work
- Theming system is comprehensive

**Cons:**
- **Massive dependency surface**: ChatMessageCell imports 100+ Telegram classes
- Must stub ~15 singletons touching network, contacts, media, accounts
- Need to vendor ~50+ Components classes transitively
- TLRPC data classes needed as plain holders (hundreds of fields)
- Build complexity: remove `tgnet`, native libs, Firebase, Premium, Stories
- APK size impact unknown
- Licensing: every file needs header audit, THIRD_PARTY.md tracking

**Estimated Effort:**
- Phase 2 (vendor + stub): 3-4 weeks
- Phase 3 (sandbox): 1 week
- Phase 4 (adapter): 2 weeks
- Phase 5-6 (integration): 3-4 weeks
- **Total: 9-12 weeks**

**Top 5 Risks:**
1. **Stub surface too large** - ChatMessageCell reaches into MessagesController, FileLoader, MediaController, DownloadController, ContactsController, UserConfig, AccountInstance, ChatObject, DialogObject. Each needs a Delta-backed implementation.
2. **TLRPC bloat** - Need to copy hundreds of TLRPC classes as data holders; maintenance burden.
3. **Native build breakage** - Removing `tgnet` may break Gradle config; need to verify ndk-make.sh still works.
4. **Licensing drift** - Must track every copied file's upstream commit and license header.
5. **RTL/bidi bugs** - Telegram's RTL is complex; Delta Chat has Persian/Arabic users. Integration testing needed.

---

### Plan B: Restyle Delta Chat's Own RecyclerView Items

**Pros:**
- Zero new dependencies
- No stubbing, no TLRPC, no native lib removal
- Keeps existing Delta Chat logic (events, selection, webxdc, media viewer)
- Smaller diff, easier to review
- Build stays exactly as-is

**Cons:**
- Must re-implement: bubble tails, grouping corners, tick animations, reaction layout, emoji-only enlargement, date chips, forward header, reply preview, RTL tail flip
- Telegram's `MessageBackgroundDrawable` (bubble shape logic) is ~500 lines of Path math - must port or reimplement
- Emoji rendering: Delta uses `androidx.emoji2` + system; Telegram uses Apple bitmaps + custom spans
- More visual QA needed to match "pixel-close"

**Estimated Effort:**
- Phase 2 (bubble drawable + tail logic): 2 weeks
- Phase 3 (chat list row restyle): 1 week
- Phase 4 (chat screen items): 3 weeks
- Phase 5 (emoji): 1 week
- Phase 6 (theming/polish): 2 weeks
- **Total: 9 weeks**

**Top 5 Risks:**
1. **Bubble tail math** - Getting corner radii + tail exactly right across grouping states (first/middle/last, incoming/outgoing, media/text) is finicky.
2. **Emoji parity** - Telegram's emoji-only enlargement + Apple-style rendering requires porting `AnimatedEmojiSpan` + bitmap assets.
3. **Reactions layout** - Telegram's in-bubble reaction strip with animated counters is non-trivial.
4. **RTL** - Must mirror tails, tick positions, text alignment correctly.
5. **Performance** - Custom drawables must match Telegram's 60fps scroll on 5k messages.

---

## 6. Recommendation

**Recommendation: PLAN B (Restyle Delta Chat's own UI)**

### Rationale:
1. **Lower architectural risk** - No massive stub layer, no TLRPC vendoring, no native build surgery.
2. **Comparable effort** - Plan A's "vendor" work (stubs + TLRPC + build cleanup) ≈ Plan B's "reimplement visuals" work.
3. **Maintainability** - Plan B keeps one codebase; Plan A creates a permanent fork of 50+ Telegram files that will diverge upstream.
4. **Delta Chat features preserved** - WebXDC, encryption indicators, contact requests, archive/pin/mute, multi-account all work natively without adapter translation layer.
5. **Team velocity** - Two humans working in parallel: one on chat list, one on chat screen, no merge conflicts with vendored code.

### Mitigation for Plan B's Risks:
- **Bubble drawable**: Copy Telegram's `MessageBackgroundDrawable.java` as reference (MIT-compatible algorithm), adapt to Delta's `ConversationItem` view hierarchy.
- **Emoji**: Implement `EmojiProvider` abstraction (per spec), default to NotoColorEmoji (bundled), Telegram Apple set as opt-in dev-only.
- **Reactions**: Port only `ReactionsLayoutInBubble` logic (measurement + layout), not the full Telegram component tree.
- **RTL**: Test with Persian locale from Day 1; use `ViewCompat.setLayoutDirection()`.

### Phase 0 Gate Decision Required:
**Humans: Choose A or B.** If A, I proceed with `:tgui` module creation and stubbing. If B, I proceed with restyling `ConversationItem` and `ConversationListItem` in place.

---

## 7. Next Steps (After Gate)

### If Plan B Chosen:
1. Create branch `feat/phase1-baseline` - rename appId, app name, icon
2. Add `README.md`, `THIRD_PARTY.md`, `docs/PROGRESS.md`
3. Add GitHub Actions CI (assembleDebug)
4. Create `EmojiProvider` abstraction + Noto emoji bundling
5. Begin `MessageBackgroundDrawable` port for bubble tails

### If Plan A Chosen:
1. Create branch `feat/phase2-vendor-tgui`
2. Create `:tgui` Gradle module
3. Copy `DialogCell`, `ChatMessageCell`, transitive `Components` deps
4. Create `:tgui-stubs` module with singleton stubs
5. Remove `tgnet`, Firebase, Premium, Stories from build

---

## Appendix: Verification Checklist

- [x] Delta Chat repo cloned with submodule
- [x] Telegram donor repo cloned (shallow)
- [x] Delta Chat UI classes identified and measured
- [x] Telegram key UI classes identified and measured
- [x] ChatMessageCell imports analyzed (100+ Telegram deps)
- [x] DialogCell imports analyzed (80+ Telegram deps)
- [x] Field mapping table created (Delta → Telegram)
- [x] Media type mapping table created
- [x] Message state → ticks mapping created
- [x] Plan A vs B effort estimated
- [x] Top 5 risks identified for each plan
- [ ] **HUMAN DECISION: A or B**

---

*End of Phase 0 Recon Report*
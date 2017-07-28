Delta Chat Android Client
================================================================================

This is the android client for Delta Chat.  For the core library and other common
information, please refer to [Delta Chat Core Library](https://github.com/deltachat/deltachat-core).
For ready-to-use binaries, please go to https://delta.chat .

![Logo](https://delta.chat/assets/features/start-img4.png)


Build
--------------------------------------------------------------------------------

After checking out the deltachat-android repository, it may be needed to checkout the submodule deltachat-core explicitly;
type `git submodule update --init --recursive` for this purpose.

Beside a build in Android Studio, you have to call `ndk-build` in the
`MessengerProj` directory.

The core library (https://github.com/deltachat/deltachat-core), is checked out
automatically; there is no need to build the core library itself, the android
client just references the needed files.

---

Copyright © 2017 Delta Chat contributors

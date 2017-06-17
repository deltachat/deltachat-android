Delta Chat Android Client
================================================================================

This is the android client for Delta Chat.  For the core library and other common
information, please refer to https://github.com/deltachat/deltachat-core and to
https://delta.chat .


Build
--------------------------------------------------------------------------------

Beside a build in Android Studio, you have to call `ndk-build` in the
`MessengerProj` directory.  Moreover, place a copy of your keyfile eg. to
`MessengerProj/config/debug.keystore`.

![Logo](https://delta.chat/public/images/features/start-img4.png)

The core library (https://github.com/deltachat/deltachat-core), is checked out
automatically; there is no need to build the core library itself, the android
client just references the needed files.

The Delta Chat Android Frontend is based upon
[Telegram FOSS](https://github.com/slp/Telegram-FOSS).

---

Copyright © 2017 Delta Chat contributors

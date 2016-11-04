Delta Chat Android Frontend
================================================================================

This is the android frontend for Delta Chat.  For the backend and other common
information, please refer to https://github.com/r10s/messenger-backend .


Build
--------------------------------------------------------------------------------

Beside a build in Android Studio, you have to call `ndk-build` in the
`TMessagesProj` directory.  Moreover, place a copy of your keyfile eg. to
`TMessagesProj/config/debug.keystore`.

![Logo](https://messenger.b44t.com/start-img4.png)

You'll also need the backend (https://github.com/r10s/messenger-backend), that
must be placed at `../messenger-backend`, however, there is not need to build
the backend itself, the android frontend just references the needed files.

The Delta Chat Android Frontend is based upon
[Telegram FOSS](https://github.com/slp/Telegram-FOSS).

---

Copyright (c) Bjoern Petersen Software Design and Development,
http://b44t.com and contributors.

Delta Chat Android Frontend
================================================================================

This is the android frontend for Delta Chat.  For the backend and other common
information, please refer to https://github.com/r10s/messenger-backend and to
https://getdelta.org .


Build
--------------------------------------------------------------------------------

Beside a build in Android Studio, you have to call `ndk-build` in the
`MessengerProj` directory.  Moreover, place a copy of your keyfile eg. to
`MessengerProj/config/debug.keystore`.

![Logo](https://getdelta.org/start-img4.png)

The backend (https://github.com/r10s/messenger-backend), is checked out 
automatically; there is not need to build the backend itself, the android 
frontend just references the needed files.

The Delta Chat Android Frontend is based upon
[Telegram FOSS](https://github.com/slp/Telegram-FOSS).

---

Copyright (C) 2017 Delta Chat contributors

Delta Chat Android Client
================================================================================

This is the android client for Delta Chat.  For the core library and other common
information, please refer to [Delta Chat Core Library](https://github.com/deltachat/deltachat-core).
For ready-to-use binaries, please go to https://delta.chat .

![Logo](https://delta.chat/assets/features/start-img4.png)

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="75"/>](https://f-droid.org/packages/com.b44t.messenger)


Build
--------------------------------------------------------------------------------

When checking out _deltachat-android_, make sure also to check out the
subproject _deltachat-core_:

- When using Git, you can can do this initially by
  `$ git clone --recursive https://github.com/deltachat/deltachat-android.git`
  or later by `git submodule update --init --recursive`.

- Alternatively, you can download the [deltachat-android zip-file](https://github.com/deltachat/deltachat-android/archive/master.zip); in this case, also download the [deltachat-core zip-file](https://github.com/deltachat/deltachat-core/archive/master.zip) and place its contents to `MessengerProj/jni/messenger-backend` 

There is no need to build the core library itself, deltachat-android just 
references them.

Then, call `ndk-build` in the `MessengerProj` directory to build the C-part
and run the project in Android Studio.  The project required API 25.

With chance, that's it :) - if not, read on how to setup a proper development
environment.


Install Development Environment
--------------------------------------------------------------------------------

1. Some libs required for Android Studio may be missing on 64 bit linux machines 
   [[Source](https://developer.android.com/studio/install.html)], so for Ubuntu execute  
   `$ sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386`  
   and for Fedora execute  
   `$ sudo yum install zlib.i686 ncurses-libs.i686 bzip2-libs.i686`
  
2. Download Android Studio from <https://developer.android.com> (android-studio-ide-...-linux.zip)
   and unzip the archive which contains a single folder called `android-studio`; 
   move this folder eg. to `~/android-studio` 

3. To launch Android Studio for the first time, open a terminal, navigate to 
   `~/android-studio/bin` and execute `studio.sh` and use all standard values
   from the wizard
   
4. Android Studio now ask you if you want to download an existing project; 
   choose `~/deltachat-android` as created in step 1 (Android Studio starts to
   build the project, however, there are some steps missing before this will
   succeed)
   
5. As Delta Chat uses API 25 for some reasons, go to "Tools / Android / 
   SDK Manager / SDK Platforms" and enable "Nougat 7.1.1 (API 25)" -
   now the build should succeed - but the app still misses the native part

6. Download Android NDK from <https://developer.android.com/ndk/downloads/> (eg. android-ndk-r15c-linux-x86_64.zip)
   and unzip the archive which contains a single folder called eg.
   `android-ndk-r15c`; move this folder eg. to `~/android-ndk-r15c`
   
7. Export the folder path to your environment as ANDROID_NDK and add it to PATH.
   You can archive this eg. by adding the following lines to `.bashrc`  
   `export ANDROID_NDK=/home/bpetersen/android-ndk-r15c`  
   `export PATH=$PATH:$ANDROID_NDK`
   
The last two steps may be replaced by using the new Android Studio NDK options, however, up to now, we did not found any time to do so.

---

Copyright © 2017 Delta Chat contributors

#!/bin/sh

# If you want to speed up compilation, you can run this script with your
# architecture as an argument:
#
#     ./ndk-make.sh arm64-v8a
#
# Possible values are armeabi-v7a, arm64-v8a, x86 and x86_64.
# You should be able to find out your architecture by running:
#
#     adb shell uname -m
#
# or:
#
#     adb shell cat /proc/cpuinfo
#
# The values in the following lines mean the same:
#
# armeabi-v7a, armv7 and arm
# arm64-v8a, aarch64 and arm64
# x86 and i686
# (there are no synonyms for x86_64)
#
#
# If you put this in your .bashrc, then you can directly build and
# deploy DeltaChat from the jni/deltachat-core-rust directory by
# typing `nmake`:
#
# nmake() {(cd ../..; ./ndk-make.sh arm64-v8a && ./gradlew installFatDebug; notify-send "install finished")}

set -e
echo "starting time: `date`"

: "${ANDROID_NDK_ROOT:=$ANDROID_NDK_HOME}"
: "${ANDROID_NDK_ROOT:=$ANDROID_NDK}"
if test ! -z "$ANDROID_NDK_ROOT"; then
    echo Setting CARGO_TARGET environment variables.

    if test -z "$NDK_HOST_TAG"; then
        KERNEL="$(uname -s | tr '[:upper:]' '[:lower:]')"
        ARCH="$(uname -m)"
        NDK_HOST_TAG="$KERNEL-$ARCH"
    fi

    TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/$NDK_HOST_TAG"
    export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$TOOLCHAIN/bin/armv7a-linux-androideabi16-clang"
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/aarch64-linux-android21-clang"
    export CARGO_TARGET_I686_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/i686-linux-android16-clang"
    export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/x86_64-linux-android21-clang"
fi

# Check if the argument is a correct architecture:
if test $1 && echo "armeabi-v7a arm64-v8a x86 x86_64" | grep -vwq $1; then
    echo "Architecture '$1' not known, possible values are armeabi-v7a, arm64-v8a, x86 and x86_64."
    exit
fi

cd jni
rm -f armeabi-v7a/*
rm -f arm64-v8a/*
rm -f x86/*
rm -f x86_64/*
mkdir -p armeabi-v7a
mkdir -p arm64-v8a
mkdir -p x86
mkdir -p x86_64

cd deltachat-core-rust

# fix build on MacOS Catalina
unset CPATH

if test -z $1; then
  echo Full build

  export RUSTFLAGS="-C lto=on -C embed-bitcode=yes"
  RELEASE="release"
  RELEASEFLAG="--release"
else
  echo Fast, partial, slow debug build. DO NOT UPLOAD THE APK ANYWHERE.

  RELEASE="debug"
  RELEASEFLAG=
fi

if test -z $1 || test $1 = armeabi-v7a; then
    echo "-- cross compiling to armv7-linux-androideabi (arm) --"
    export CFLAGS=-D__ANDROID_API__=16
    TARGET_CC=armv7a-linux-androideabi16-clang \
    cargo +`cat rust-toolchain` build $RELEASEFLAG --target armv7-linux-androideabi -p deltachat_ffi
    cp target/armv7-linux-androideabi/$RELEASE/libdeltachat.a ../armeabi-v7a
fi

if test -z $1 || test $1 = arm64-v8a; then
    echo "-- cross compiling to aarch64-linux-android (arm64) --"
    export CFLAGS=-D__ANDROID_API__=21
    TARGET_CC=aarch64-linux-android21-clang \
    cargo +`cat rust-toolchain` build $RELEASEFLAG --target aarch64-linux-android -p deltachat_ffi
    cp target/aarch64-linux-android/$RELEASE/libdeltachat.a ../arm64-v8a
fi

# if test -z $1 || test $1 = x86; then
#     echo "-- cross compiling to i686-linux-android (x86) --"
#     export CFLAGS=-D__ANDROID_API__=16
#     TARGET_CC=i686-linux-android16-clang \
#     cargo +`cat rust-toolchain` build $RELEASEFLAG --target i686-linux-android -p deltachat_ffi
#     cp target/i686-linux-android/$RELEASE/libdeltachat.a ../x86
# fi

# if test -z $1 || test $1 = x86_64; then
#     echo "-- cross compiling to x86_64-linux-android (x86_64) --"
#     export CFLAGS=-D__ANDROID_API__=21
#     TARGET_CC=x86_64-linux-android21-clang \
#     cargo +`cat rust-toolchain` build $RELEASEFLAG --target x86_64-linux-android -p deltachat_ffi
#     cp target/x86_64-linux-android/$RELEASE/libdeltachat.a ../x86_64
# fi

echo -- ndk-build --

cd ..
# Set the right arch in Application.mk:
oldDotMk="$(cat Application.mk)"

if test $1; then
    # Using temporary file because `sed -i` is not portable
    TMP=$(mktemp)
    sed "s/APP_ABI.*/APP_ABI := $1/g" Application.mk >"$TMP"
    mv "$TMP" Application.mk
else
    # We are compiling for all architectures:
    TMP=$(mktemp)
    sed "s/APP_ABI.*/APP_ABI := armeabi-v7a arm64-v8a/g" Application.mk >"$TMP"
    mv "$TMP" Application.mk
fi

cd ..
ndk-build

cd jni
# Restore old Application.mk:
echo "$oldDotMk" > Application.mk

echo "ending time: `date`"

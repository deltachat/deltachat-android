#!/bin/sh

# If you want to speed up compilation, you can run this script with your
# architecture as an argument:
#
#     scripts/ndk-make.sh arm64-v8a
#
# Possible values are armeabi-v7a, arm64-v8a, x86 and x86_64.
#
# To build the core in debug mode, run with "--debug" argument in the beginning:
#
#     scripts/ndk-make.sh --debug arm64-v8a
#
# You should be able to find out your architecture by running:
#
#     adb shell uname -m
#
# Use an app like: https://f-droid.org/packages/com.kgurgul.cpuinfo/
#
# Or you just guess your phone's architecture to be arm64-v8a and if you
# guessed wrongly, a warning message will pop up inside the app, telling
# you what the correct one is.
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
# nmake() {(cd ../..; scripts/ndk-make.sh arm64-v8a && ./gradlew installFossDebug; notify-send "install finished")}
#
#
# If anything doesn't work, please open an issue!!

set -e
echo "starting time: `date`"

export CFLAGS="-fno-unwind-tables -fno-exceptions -fno-asynchronous-unwind-tables -fomit-frame-pointer -fvisibility=hidden"

: "${ANDROID_NDK_ROOT:=$ANDROID_NDK_HOME}"
: "${ANDROID_NDK_ROOT:=$ANDROID_NDK}"
if test -z "$ANDROID_NDK_ROOT"; then
    echo "ANDROID_NDK_ROOT is not set"
    exit 1
fi

# for reproducible build:
export RUSTFLAGS="-C link-args=-Wl,--build-id=none --remap-path-prefix=$HOME/.cargo= --remap-path-prefix=$(realpath $(dirname $(dirname "$0")))="
export SOURCE_DATE_EPOCH=1
# always use the same path to NDK:
rm -f /tmp/android-ndk-root
ln -s "$ANDROID_NDK_ROOT" /tmp/android-ndk-root
ANDROID_NDK_ROOT=/tmp/android-ndk-root

echo Setting CARGO_TARGET environment variables.

if test -z "$NDK_HOST_TAG"; then
    KERNEL="$(uname -s | tr '[:upper:]' '[:lower:]')"
    ARCH="$(uname -m)"

    if test "$ARCH" = "arm64" && test ! -f "$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/$KERNEL-$ARCH/bin/aarch64-linux-android21-clang"; then
        echo "arm64 host is not supported by $ANDROID_NDK_ROOT; trying to use x86_64, in case the host has a binary translation such as Rosetta or QEMU installed."
        echo "(Newer NDK may support arm64 host but may lack support for Android4/ABI16)"
        ARCH="x86_64"
    fi

    NDK_HOST_TAG="$KERNEL-$ARCH"
fi

if test -z "$CARGO_TARGET_DIR"; then
    export CARGO_TARGET_DIR=/tmp/deltachat-build
fi

TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/$NDK_HOST_TAG"
export CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$TOOLCHAIN/bin/armv7a-linux-androideabi21-clang"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/aarch64-linux-android21-clang"
export CARGO_TARGET_I686_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/i686-linux-android21-clang"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/x86_64-linux-android21-clang"

export RUSTUP_TOOLCHAIN=$(cat "$(dirname "$0")/rust-toolchain")

if test "$1" = "--debug"; then
  echo Quick debug build that will produce a slower app. DO NOT UPLOAD THE APK ANYWHERE.

  RELEASE="debug"
  RELEASEFLAG=""

  shift
else
  echo Full build

  # According to 1.45.0 changelog in https://github.com/rust-lang/rust/blob/master/RELEASES.md,
  # "The recommended way to control LTO is with Cargo profiles, either in Cargo.toml or .cargo/config, or by setting CARGO_PROFILE_<name>_LTO in the environment."
  export CARGO_PROFILE_RELEASE_LTO=on
  RELEASE="release"
  RELEASEFLAG="--release"
fi

# Check if the argument is a correct architecture:
if test $1 && echo "armeabi-v7a arm64-v8a x86 x86_64" | grep -vwq -- $1; then
    echo "Architecture '$1' not known, possible values are armeabi-v7a, arm64-v8a, x86 and x86_64."
    exit
fi

cd jni
jnidir=$PWD
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

if test -z $1 || test $1 = armeabi-v7a; then
    echo "-- cross compiling to armv7-linux-androideabi (arm) --"
    TARGET_CC="$TOOLCHAIN/bin/armv7a-linux-androideabi21-clang" \
    TARGET_AR="$TOOLCHAIN/bin/llvm-ar" \
    TARGET_RANLIB="$TOOLCHAIN/bin/llvm-ranlib" \
    cargo build $RELEASEFLAG --target armv7-linux-androideabi -p deltachat_ffi
    cp "$CARGO_TARGET_DIR/armv7-linux-androideabi/$RELEASE/libdeltachat.a" "$jnidir/armeabi-v7a"
fi

if test -z $1 || test $1 = arm64-v8a; then
    echo "-- cross compiling to aarch64-linux-android (arm64) --"
    TARGET_CC="$TOOLCHAIN/bin/aarch64-linux-android21-clang" \
    TARGET_AR="$TOOLCHAIN/bin/llvm-ar" \
    TARGET_RANLIB="$TOOLCHAIN/bin/llvm-ranlib" \
    cargo build $RELEASEFLAG --target aarch64-linux-android -p deltachat_ffi
    cp "$CARGO_TARGET_DIR/aarch64-linux-android/$RELEASE/libdeltachat.a" "$jnidir/arm64-v8a"
fi

if test -z $1 || test $1 = x86; then
    echo "-- cross compiling to i686-linux-android (x86) --"
    TARGET_CC="$TOOLCHAIN/bin/i686-linux-android21-clang" \
    TARGET_AR="$TOOLCHAIN/bin/llvm-ar" \
    TARGET_RANLIB="$TOOLCHAIN/bin/llvm-ranlib" \
    cargo build $RELEASEFLAG --target i686-linux-android -p deltachat_ffi
    cp "$CARGO_TARGET_DIR/i686-linux-android/$RELEASE/libdeltachat.a" "$jnidir/x86"
fi

if test -z $1 || test $1 = x86_64; then
    echo "-- cross compiling to x86_64-linux-android (x86_64) --"
    TARGET_CC="$TOOLCHAIN/bin/x86_64-linux-android21-clang" \
    TARGET_AR="$TOOLCHAIN/bin/llvm-ar" \
    TARGET_RANLIB="$TOOLCHAIN/bin/llvm-ranlib" \
    cargo build $RELEASEFLAG --target x86_64-linux-android -p deltachat_ffi
    cp "$CARGO_TARGET_DIR/x86_64-linux-android/$RELEASE/libdeltachat.a" "$jnidir/x86_64"
fi

echo -- ndk-build --

cd ../..

if test $1; then
    "$ANDROID_NDK_ROOT/ndk-build" APP_ABI="$1"
else
    # We are compiling for all architectures defined in Application.mk
    "$ANDROID_NDK_ROOT/ndk-build"
fi

if test $1; then
    echo "NDK_ARCH=$1" >ndkArch
else
    rm -f ndkArch # Remove ndkArch, ignore if it doesn't exist
fi

echo "ending time: `date`"

#!/bin/sh

# to setup the toolchains (from https://medium.com/visly/rust-on-android-19f34a2fb43 )
# run the following in `jni/deltachat-core-rust`:
#
#    rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android --toolchain `cat rust-toolchain`
# 
# Currently ndk20b is minimum required version
# (newer versions will likely work, however, are not tested and not used in offial releases,
# in general, changes on the ndk-version should be done with care)
#
# after that, add PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin to your $PATH
# and add the correct clang-linkers to `~/.cargo/config`:
# ```
# [target.armv7-linux-androideabi]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/armv7a-linux-androideabi16-clang"
# [target.aarch64-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/aarch64-linux-android21-clang"
# [target.i686-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/i686-linux-android16-clang"
# [target.x86_64-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/x86_64-linux-android21-clang"
# ```
#
#
# If you want to, you can run the script with your architecture as an argument
# to speed up compilation by factor 4:
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


set -e
echo "starting time: `date`"

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

if test -z $1 || test $1 = armeabi-v7a; then
    echo "-- cross compiling to armv7-linux-androideabi (arm) --"
    export CFLAGS=-D__ANDROID_API__=16
    TARGET_CC=armv7a-linux-androideabi16-clang \
    cargo +`cat rust-toolchain` build --release --target armv7-linux-androideabi -p deltachat_ffi
    cp target/armv7-linux-androideabi/release/libdeltachat.a ../armeabi-v7a
fi

if test -z $1 || test $1 = arm64-v8a; then
    echo "-- cross compiling to aarch64-linux-android (arm64) --"
    export CFLAGS=-D__ANDROID_API__=21
    TARGET_CC=aarch64-linux-android21-clang \
    cargo +`cat rust-toolchain` build --release --target aarch64-linux-android -p deltachat_ffi
    cp target/aarch64-linux-android/release/libdeltachat.a ../arm64-v8a
fi

if test -z $1 || test $1 = x86; then
    echo "-- cross compiling to i686-linux-android (x86) --"
    export CFLAGS=-D__ANDROID_API__=16
    TARGET_CC=i686-linux-android16-clang \
    cargo +`cat rust-toolchain` build --release --target i686-linux-android -p deltachat_ffi
    cp target/i686-linux-android/release/libdeltachat.a ../x86
fi

if test -z $1 || test $1 = x86_64; then
    echo "-- cross compiling to x86_64-linux-android (x86_64) --"
    export CFLAGS=-D__ANDROID_API__=21
    TARGET_CC=x86_64-linux-android21-clang \
    cargo +`cat rust-toolchain` build --release --target x86_64-linux-android -p deltachat_ffi
    cp target/x86_64-linux-android/release/libdeltachat.a ../x86_64
fi

echo -- ndk-build --

cd ..
# Set the right arch in Application.mk:
oldDotMk="$(cat Application.mk)"

if test $1; then
    sed -i "s/APP_ABI.*/APP_ABI := $1/g" Application.mk
else
    # We are compiling for all architectures:
    sed -i "s/APP_ABI.*/APP_ABI := armeabi-v7a arm64-v8a x86 x86_64/g" Application.mk
fi

cd ..
ndk-build

cd jni
# Restore old Application.mk:
echo "$oldDotMk" > Application.mk

echo "ending time: `date`"

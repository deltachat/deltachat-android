#!/bin/sh
set -e
echo "starting time: `date`"

cd jni/deltachat-core-rust

# to setup the toolchains (from https://medium.com/visly/rust-on-android-19f34a2fb43 )
# run the following in `jni/deltachat-core-rust`:
# $ rustup target add armv7-linux-androideabi aarch64-linux-android  i686-linux-android x86_64-linux-android
# after that, add PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin to your $PATH
# and add the correct clang-linkers to `~/.cargo/config`:
# ```
# [target.armv7-linux-androideabi]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/armv7a-linux-androideabi18-clang"
# [target.aarch64-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/aarch64-linux-android21-clang"
# [target.i686-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/i686-linux-android18-clang"
# [target.x86_64-linux-android]
# linker = "PATH_TO_NDK/toolchains/llvm/prebuilt/HOST/bin/x86_64-linux-android21-clang"
# ```
# then, the following should work:

echo "-- cross compiling to armv7-linux-androideabi (arm) --"
export CFLAGS=-D__ANDROID_API__=18
TARGET_CC=armv7a-linux-androideabi18-clang \
cargo build --release --target armv7-linux-androideabi -p deltachat_ffi

echo "-- cross compiling to aarch64-linux-android (arm64) --"
export CFLAGS=-D__ANDROID_API__=21
TARGET_CC=aarch64-linux-android21-clang \
cargo build --release --target aarch64-linux-android -p deltachat_ffi

echo "-- cross compiling to i686-linux-android (x86) --"
export CFLAGS=-D__ANDROID_API__=18
TARGET_CC=i686-linux-android18-clang \
cargo build --release --target i686-linux-android -p deltachat_ffi

echo "-- cross compiling to x86_64-linux-android (x86_64) --"
export CFLAGS=-D__ANDROID_API__=21
TARGET_CC=x86_64-linux-android21-clang \
cargo build --release --target x86_64-linux-android -p deltachat_ffi

echo -- copy generated .a files --
cd ..
rm armeabi-v7a/*
rm arm64-v8a/*
rm x86/*
rm x86_64/*
mkdir -p armeabi-v7a
mkdir -p arm64-v8a
mkdir -p x86
mkdir -p x86_64
cp deltachat-core-rust/target/armv7-linux-androideabi/release/libdeltachat.a armeabi-v7a
cp deltachat-core-rust/target/aarch64-linux-android/release/libdeltachat.a arm64-v8a
cp deltachat-core-rust/target/i686-linux-android/release/libdeltachat.a x86
cp deltachat-core-rust/target/x86_64-linux-android/release/libdeltachat.a x86_64

echo -- ndk-build --
cd ..
ndk-build
echo "ending time: `date`"

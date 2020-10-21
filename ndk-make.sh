#!/bin/sh
set -e
echo "starting time: `date`"

cd jni/deltachat-core-rust

# to setup the toolchains (from https://medium.com/visly/rust-on-android-19f34a2fb43 )
# run `scripts/install-toolchains.sh`.
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
# then, the following should work:
# (If you want to speed up compilation, have look at the comment in ndk-make-fast.sh.)

# fix build on MacOS Catalina
unset CPATH

echo "-- cross compiling to armv7-linux-androideabi (arm) --"
export CFLAGS=-D__ANDROID_API__=16
RUSTFLAGS="-C lto=on -C embed-bitcode=yes" \
TARGET_CC=armv7a-linux-androideabi16-clang \
cargo +`cat rust-toolchain` build --release --target armv7-linux-androideabi -p deltachat_ffi

echo "-- cross compiling to aarch64-linux-android (arm64) --"
export CFLAGS=-D__ANDROID_API__=21
RUSTFLAGS="-C lto=on -C embed-bitcode=yes" \
TARGET_CC=aarch64-linux-android21-clang \
cargo +`cat rust-toolchain` build --release --target aarch64-linux-android -p deltachat_ffi

# echo "-- cross compiling to i686-linux-android (x86) --"
# export CFLAGS=-D__ANDROID_API__=16
# RUSTFLAGS="-C lto=on -C embed-bitcode=yes" \
# TARGET_CC=i686-linux-android16-clang \
# cargo +`cat rust-toolchain` build --release --target i686-linux-android -p deltachat_ffi

# echo "-- cross compiling to x86_64-linux-android (x86_64) --"
# export CFLAGS=-D__ANDROID_API__=21
# RUSTFLAGS="-C lto=on -C embed-bitcode=yes" \
# TARGET_CC=x86_64-linux-android21-clang \
# cargo +`cat rust-toolchain` build --release --target x86_64-linux-android -p deltachat_ffi

echo -- copy generated .a files --
cd ..
rm -f armeabi-v7a/*
rm -f arm64-v8a/*
rm -f x86/*
rm -f x86_64/*
mkdir -p armeabi-v7a
mkdir -p arm64-v8a
mkdir -p x86
mkdir -p x86_64
cp deltachat-core-rust/target/armv7-linux-androideabi/release/libdeltachat.a armeabi-v7a
cp deltachat-core-rust/target/aarch64-linux-android/release/libdeltachat.a arm64-v8a
# cp deltachat-core-rust/target/i686-linux-android/release/libdeltachat.a x86
# cp deltachat-core-rust/target/x86_64-linux-android/release/libdeltachat.a x86_64

echo -- ndk-build --
cd ..
ndk-build
echo "ending time: `date`"

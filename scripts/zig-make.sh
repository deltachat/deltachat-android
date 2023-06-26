#!/bin/sh

set -x
set -e

cd jni

: "${JAVA_HOME:=/usr/lib/jvm/java-17-openjdk/}"

zigbuild() {
    mkdir -p ../libs/$1
    zig cc -static -o ../libs/$1/libnative-utils-debug.so -target $2 $3 -Oz -flto=thin -fno-unwind-tables -fno-exceptions -fno-asynchronous-unwind-tables -fomit-frame-pointer -I $JAVA_HOME/include/ -I $JAVA_HOME/include/linux/ -shared -fPIC dc_wrapper.c deltachat-core-rust/target/$4/release/libdeltachat.a 

    # Strip symbols.
    zig objcopy -S ../libs/$1/libnative-utils-debug.so ../libs/$1/libnative-utils.so
    rm -f ../libs/$1/libnative-utils-debug.so
}

zigbuild x86_64      x86_64-linux-musl    ""                                       x86_64-unknown-linux-musl
zigbuild x86         x86-linux-musl       ""                                       i686-unknown-linux-musl
zigbuild armeabi-v7a arm-linux-musleabihf "-mcpu=generic+v7a+vfp3-d32+thumb2-neon" armv7-unknown-linux-musleabihf
zigbuild arm64-v8a   aarch64-linux-musl   ""                                       aarch64-unknown-linux-musl

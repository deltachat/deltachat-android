#!/bin/sh
#
# Installs Rust cross-compilation toolchains for all supported architectures.
#
set -e
TARGETS="armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android"
TOOLCHAIN="$(cat jni/deltachat-core-rust/rust-toolchain)"
rustup install "$TOOLCHAIN"
rustup target add $TARGETS --toolchain "$TOOLCHAIN"

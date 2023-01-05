#!/bin/sh
#
# Installs Rust cross-compilation toolchains for all supported architectures.
#
set -e
TARGETS="armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android"
RUSTUP_TOOLCHAIN=$(cat "$(dirname "$0")/rust-toolchain")
rustup install "$RUSTUP_TOOLCHAIN"
rustup target add $TARGETS --toolchain "$RUSTUP_TOOLCHAIN"

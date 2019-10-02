echo -- cross compiling --
cd jni/deltachat-core-rust

# to setup the toolchains (from https://medium.com/visly/rust-on-android-19f34a2fb43 )
# $ rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android
# $ mkdir ~/.NDK
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 21 --arch arm64 --install-dir ~/.NDK/arm64
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 14 --arch arm --install-dir ~/.NDK/arm
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 14 --arch x86 --install-dir ~/.NDK/x86
# (--api should be aligned with APP_PLATFORM from Application.mk and with minSdkVersion)
# this installs toolchains in `~/.NDK`
# then, the following should work:

export RUSTFLAGS="-C linker=$HOME/.NDK/arm64/bin/clang";
export PATH=~/.NDK/arm64/bin:$PATH;
cargo build --release --target aarch64-linux-android -p deltachat_ffi

export RUSTFLAGS="-C linker=$HOME/.NDK/arm/bin/clang";
export PATH=~/.NDK/arm/bin:$PATH;
cargo build --release --target armv7-linux-androideabi -p deltachat_ffi

export RUSTFLAGS="-C linker=$HOME/.NDK/x86/bin/clang";
export PATH=~/.NDK/x86/bin:$PATH;
cargo build --release --target i686-linux-android -p deltachat_ffi

# an alternative might be:
# $ cross build --release --target <target> -p deltachat_ffi

echo -- copy generated .a files --
cd ..
rm arm64-v8a/*
rm armeabi-v7a/*
rm x86/*
mkdir -p arm64-v8a
mkdir -p armeabi-v7a
mkdir -p x86
cp deltachat-core-rust/target/aarch64-linux-android/release/libdeltachat.a arm64-v8a
cp deltachat-core-rust/target/armv7-linux-androideabi/release/libdeltachat.a armeabi-v7a
cp deltachat-core-rust/target/i686-linux-android/release/libdeltachat.a x86

echo -- ndk-build --
cd ..
ndk-build

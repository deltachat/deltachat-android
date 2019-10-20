cd jni/deltachat-core-rust

# to setup the toolchains (from https://medium.com/visly/rust-on-android-19f34a2fb43 )
# run the following in `jni/deltachat-core-rust`:
# $ rustup target add armv7-linux-androideabi aarch64-linux-android  i686-linux-android x86_64-linux-android
#
# then:
# $ mkdir ~/.NDK
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 14 --arch arm --install-dir ~/.NDK/arm
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 21 --arch arm64 --install-dir ~/.NDK/arm64
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 14 --arch x86 --install-dir ~/.NDK/x86
# $ $ANDROID_NDK/build/tools/make_standalone_toolchain.py --api 21 --arch x86_64 --install-dir ~/.NDK/x86_64
# (--api should be aligned with APP_PLATFORM from Application.mk and with minSdkVersion)
# this installs toolchains in `~/.NDK`
# then, the following should work:

echo "-- cross compiling to armv7-linux-androideabi (arm) --"
export RUSTFLAGS="-C linker=$HOME/.NDK/arm/bin/clang";
export PATH=~/.NDK/arm/bin:$PATH;
cargo build --release --target armv7-linux-androideabi -p deltachat_ffi

echo "-- cross compiling to aarch64-linux-android (arm64) --"
export RUSTFLAGS="-C linker=$HOME/.NDK/arm64/bin/clang";
export PATH=~/.NDK/arm64/bin:$PATH;
cargo build --release --target aarch64-linux-android -p deltachat_ffi

echo "-- cross compiling to i686-linux-android (x86) --"
export RUSTFLAGS="-C linker=$HOME/.NDK/x86/bin/clang";
export PATH=~/.NDK/x86/bin:$PATH;
cargo build --release --target i686-linux-android -p deltachat_ffi

echo "-- cross compiling to x86_64-linux-android (x86_64) --"
export RUSTFLAGS="-C linker=$HOME/.NDK/x86_64/bin/clang";
export PATH=~/.NDK/x86_64/bin:$PATH;
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

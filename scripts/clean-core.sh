cd jni/deltachat-core-rust
cargo clean
cd -

rm -rf jni/deltachat-core-rust/target
rm jni/armeabi-v7a/libdeltachat.a
rm jni/x86/libdeltachat.a
rm jni/arm64-v8a/libdeltachat.a
rm jni/x86_64/libdeltachat.a

echo "now, in Android Studio, run 'Build / Clean Project'"

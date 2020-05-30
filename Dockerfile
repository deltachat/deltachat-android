FROM ubuntu:18.04

# Install Android Studio requirements
# See https://developer.android.com/studio/install#linux
RUN apt-get update -y \
&& apt-get install -y --no-install-recommends \
wget \
curl \
unzip \
openjdk-11-jre \
file \
build-essential \
&& rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME /android-sdk

WORKDIR /android-sdk
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-6200805_latest.zip && \
  unzip commandlinetools-linux-6200805_latest.zip && \
  rm commandlinetools-linux-6200805_latest.zip

RUN yes | ${ANDROID_HOME}/tools/bin/sdkmanager --sdk_root=${ANDROID_HOME} --licenses

ENV PATH ${PATH}:/android-sdk/tools/bin

# Install NDK manually. Other SDK parts are installed automatically by gradle.
RUN sdkmanager --sdk_root=${ANDROID_HOME} ndk-bundle

ENV PATH ${PATH}:/android-sdk/ndk-bundle/toolchains/llvm/prebuilt/linux-x86_64/bin/:/android-sdk/ndk-bundle/

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain none
ENV PATH ${PATH}:/root/.cargo/bin
COPY jni/deltachat-core-rust/rust-toolchain ${ANDROID_HOME}
RUN rustup default `cat ${ANDROID_HOME}/rust-toolchain` \
&& rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android

COPY docker/cargo-config /root/.cargo/config

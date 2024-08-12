FROM docker.io/debian:12.4

# Install Android Studio requirements
# See https://developer.android.com/studio/install#linux
RUN apt-get update -y \
&& apt-get install -y --no-install-recommends \
wget \
curl \
unzip \
openjdk-17-jre \
file \
build-essential \
&& rm -rf /var/lib/apt/lists/*

ARG USER=deltachat
ARG UID=1000
ARG GID=1000

RUN groupadd -g $GID -o $USER
RUN useradd -m -u $UID -g $GID -o $USER
USER $USER

ENV ANDROID_SDK_ROOT /home/${USER}/android-sdk
RUN mkdir ${ANDROID_SDK_ROOT}
WORKDIR $ANDROID_SDK_ROOT
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip && \
  unzip commandlinetools-linux-8512546_latest.zip && \
  rm commandlinetools-linux-8512546_latest.zip

RUN yes | ${ANDROID_SDK_ROOT}/cmdline-tools/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses

ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/bin

# Install NDK manually. Other SDK parts are installed automatically by gradle.
#
# If you change the NDK version here, also change it in `flake.nix`.
# NDK version r27 LTS aka 27.0.11902837
RUN sdkmanager --sdk_root=${ANDROID_SDK_ROOT} 'ndk;27.0.11902837'

ENV ANDROID_NDK_ROOT ${ANDROID_SDK_ROOT}/ndk/27.0.11902837
ENV PATH ${PATH}:${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain none
ENV PATH ${PATH}:/home/${USER}/.cargo/bin

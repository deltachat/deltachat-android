FROM docker.io/debian:bullseye

RUN sed -i -e's/ main/ main contrib/g' /etc/apt/sources.list \
&& apt-get update -y \
&& DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
curl \
android-sdk \
google-android-ndk-installer \
build-essential \
&& rm -rf /var/lib/apt/lists/*

ARG USER=deltachat
ARG UID=1000
ARG GID=1000

RUN groupadd -g $GID -o $USER
RUN useradd -m -u $UID -g $GID -o $USER
USER $USER

ENV ANDROID_SDK_ROOT /usr/lib/android-sdk
ENV ANDROID_NDK_ROOT ${ANDROID_SDK_ROOT}/ndk-bundle
ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/tools/bin
ENV PATH ${PATH}:${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin/:${ANDROID_NDK_ROOT}

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain none
ENV PATH ${PATH}:/home/${USER}/.cargo/bin

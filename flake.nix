{
  description = "Delta Chat for Android";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    rust-overlay.url = "github:oxalica/rust-overlay";
    flake-utils.url = "github:numtide/flake-utils";
    android.url = "github:tadfisher/android-nixpkgs";
  };

  outputs = { self, nixpkgs, rust-overlay, flake-utils, android }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        overlays = [ (import rust-overlay) ];
        pkgs = import nixpkgs { inherit system overlays; };
        android-sdk = android.sdk.${system} (sdkPkgs:
          with sdkPkgs; [
            build-tools-35-0-0
            cmdline-tools-latest
            platform-tools
            platforms-android-36
            ndk-27-2-12479018
          ]);
        rust-version = pkgs.lib.removeSuffix "\n"
          (builtins.readFile ./scripts/rust-toolchain);
      in
      {
        formatter = pkgs.nixpkgs-fmt;

        devShells.default = pkgs.mkShell rec {
          ANDROID_SDK_ROOT = "${android-sdk}/share/android-sdk";
          ANDROID_NDK_ROOT =
            "${android-sdk}/share/android-sdk/ndk/27.2.12479018";
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/35.0.0/aapt2";
          buildInputs = [
            android-sdk
            pkgs.openjdk17
            (pkgs.buildPackages.rust-bin.stable."${rust-version}".minimal.override {
              targets = [
                "armv7-linux-androideabi"
                "aarch64-linux-android"
                "i686-linux-android"
                "x86_64-linux-android"
              ];
            })
          ];
        };
      });
}

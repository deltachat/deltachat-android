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
            build-tools-30-0-3
            cmdline-tools-latest
            ndk-bundle
            platform-tools
            platforms-android-32
            ndk-23-2-8568313
          ]);
      in {
        devShells.default = pkgs.mkShell {
          ANDROID_SDK_ROOT = "${android-sdk}/share/android-sdk";
          ANDROID_NDK_ROOT =
            "${android-sdk}/share/android-sdk/ndk/23.2.8568313";
	  shellHook = ''
            export PATH="$ANDROID_SDK_ROOT/build-tools/30.0.3/:$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/:$PATH"
	  '';
          buildInputs = [
            android-sdk
            pkgs.openjdk17
            (pkgs.buildPackages.rust-bin.stable."1.70.0".minimal.override {
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

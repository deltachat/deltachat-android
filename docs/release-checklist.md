# Release new F-Droid version

Release core, if needed, then:

1. $ ./tools/update-core.sh
2. $ tx pull # test and commit changes
3. bump version, adapt changelog, ommit
4. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`

... some days later, F-Droid should be updated.


# Release new Play Store version

Release core, f-droid, then:

5. In Android Studio, select "Build / Generate signed APK"
   (not: App Bundle as this would require uploading the signing key to Google)
6. Select flavor `gplayRelease` with V1 signature enabled
   (needed for easy APK verificarion), V2 is optional
7. Test the generated APK from `gplay/release`
8. upload the APK to https://play.google.com/apps/publish/


# For an additional APK

9.  in Android Studio make sure, the target is fatDebug
    and select "Build / Build APK"
    (TODO: or also use the release signing key)
10. upload the generated APK to the created Github release


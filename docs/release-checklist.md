# Release new F-Droid version

Release core, if needed, then:

1. $ ./tools/update-core.sh
2. $ tx pull # test and commit changes
3. bump version, commit
4. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`
5. update the high-level changelog deltachat-pages/en/changelog.md

... some days later, F-Droid should be updated.


# Release new Play Store version

Release core, f-droid, then:

1. In Android Studio, select "Build / Generate signed APK"
   (not: App Bundle as this would require uploading the signing key to Google)
2. Select flavor `gplayRelease` with V1 signature enabled
   (needed for easy APK verificarion), V2 is optional
3. Test the generated APK from `gplay/release`


# For an additional APK

6. in Android Studio select "Build / Build APK"
7. rename the generated APK to `deltachat-v1.2.3.apk`
8. upload the APK to the created Github release


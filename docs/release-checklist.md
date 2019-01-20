# Release new F-Droid version

Release core, if needed, then:

1. $ ./tools/update-core.sh
2. $ tx pull # test and commit changes
3. bump version, commit
4. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`
5. update the high-level changelog deltachat-pages/en/changelog.md

... some days later, F-Droid should be updated.


# For an additional APK

6. in Android Studio select "Build / Build APK"
7. rename the generated APK to `deltachat-v1.2.3.apk`
8. upload the APK to the created Github release


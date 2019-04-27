# Release new F-Droid version

Release core, if needed, then:

1. $ ./tools/update-core.sh OR
    git checkout master
    cd jni/messenger-backend/
    git checkout master
    git pull
    cd ../..
    git add jni/messenger-backend/
    git commit -m "update messenger-backend submodule"

2. $ tx pull # test and commit changes
3. bump version, adapt changelog, commit, push
4. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`

... some days later, F-Droid should be updated.


# Release new APK and Play Store version

Release core, f-droid, then:

5. make sure latest core is used: ndk-build
6. In Android Studio, select "Build / Generate signed APK"
   (not: App Bundle as this would require uploading the signing key to Google)
7. Select flavor `gplayRelease` with V1 signature enabled
   (needed for easy APK verificarion), V2 is optional
8. Upload the generated APK from `gplay/release` to the Github release created at 4
9. Test the APK
10. Upload the APK to https://play.google.com/apps/publish/


# Testing checklist

Only some rough ideas, ideally, this should result into a simple checklist
that can be checked before releasing.
However, although it would be nice to test "everything", we should keep in mind
that the test should be doable in, say, 10~15 minutes.
- create new account with (one of?): gmail, yandex, other
  or (?) test an existing account
- send and receive a message
- create a group
- do a contact verification
- join a group via a qr scan


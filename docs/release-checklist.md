# Release new F-Droid version

Release core, if needed, then:

1. $ ./tools/update-core.sh
2. $ tx pull # test and commit changes
3. bump version, adapt changelog, commit
4. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`

... some days later, F-Droid should be updated.


# Release new Play Store version

Release core, f-droid, then:

5. In Android Studio, select "Build / Generate signed APK"
   (not: App Bundle as this would require uploading the signing key to Google)
6. Select flavor `gplayRelease` with V1 signature enabled
   (needed for easy APK verificarion), V2 is optional
7. Test the generated APK from `gplay/release`
8. Upload the APK to https://play.google.com/apps/publish/


# For an additional APK

9.  Upload the APK from 8. to the Github release created at 4.


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


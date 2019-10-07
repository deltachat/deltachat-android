# Release new F-Droid version

Release core, if needed, then:

1. $ git checkout master
2. $ ./tools/update-core.sh
3. $ ./tools/tx-pull-translations.sh  # test and commit changes
4. bump version, adapt changelog, commit, push
5. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`

... some days later, F-Droid should be updated.


# Release new APK and Play Store version

Release core, f-droid, then:

1. make sure latest core is used: ndk-build
2. In Android Studio, select "Build / Generate signed APK"
   (not: App Bundle as this would require uploading the signing key to Google)
3. Select flavor `gplayRelease` with V1 signature enabled
   (needed for easy APK verification), V2 is optional
4. Upload the generated APK from `gplay/release` to the Github release created at 4
5. Test the APK
6. Upload the APK as _Beta_ (_not:_ Production) to https://play.google.com/apps/publish/
   (Release management/App releases/Open track/Manage/Create Release/Browse files ->
   select APK from above, add changelog -> Review button, then "ok" or so)

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

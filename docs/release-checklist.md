# Release checklist - generate APK

on the command-line:

1. update core rust submodule, if needed:
   $ ./scripts/update-core.sh
   depending on how much you trust in rust, you might want to do a
   ./scripts/clean-core.sh before building

2. $ ./ndk-make.sh

this will take some time - meanwhile we're doing some housekeeping:

3. update translations and local help:
   $ ./scripts/tx-pull-translations.sh
   $ cd ../deltachat-pages; ./tools/create-local-help.py; cd ../deltachat-android

4. add a device message to ConversationListActivity::onCreate()
   and update CHANGELOG.md
   (the core-changelog at
   https://github.com/deltachat/deltachat-core-rust/blob/master/CHANGELOG.md
   and the "N commits to master since last release" on
   https://github.com/deltachat/deltachat-android/releases gives some hints)

in Android Studio:

5. bump version in build.gradle,
   update _both_, versionCode and versionName

6. if `./ndk-make.sh` from step 2. is finished successfully:
   a) select "Build / Generate signed APK"
      (not: App Bundle as this would require uploading the signing key)
   b) select flavor `gplayRelease` with V1 signature enabled
      (needed for easy APK verification), V2 is optional

on success, the generated APK is at
`gplay/release/deltachat-gplay-release-VERSION.apk`


# Upload APK to get.delta.chat

7. $ ./scripts/upload-release.sh VERSION

8. a) Test the APK yourself.
      The "Testing checklist" gives some hints.
   b) Give the APK to testing groups.


# Release on Play Store

on https://play.google.com/apps/publish/ :

9. a) open "Delta Chat/Release/Production"
      then "Create new release" and upload APK from above
   b) fill out "Release details/Release notes" (500 characters max),
      release name should be default ("123 (1.2.3)")
   c) click "Save" and then "Review release"
   d) rollout to 20% by default is fine


# Release new F-Droid version

10. make sure, everything is pushed, then:
    $ git tag v1.2.1; git push --tags
    
F-Droid picks on the tags starting with "v" and builds the version.
This may take some days.


# Release new Amazon Appstore version

on https://developer.amazon.com/dashboard :

11. a) for "Delta Chat", select tab "Add upcoming version"
    b) at "Description/Edit" add "Release notes" from CHANGELOG.md, "Save"
	c) at "APK Files" hit "Edit" abottom and then "Replace APK" atop,
       upload the APK from above, "Save"
	d) hit "Submit app" at the upper right corner


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

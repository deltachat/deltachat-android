# Release checklist - generate APK

on the command-line:

1. update core rust submodule, if needed:
   $ ./scripts/update-core.sh
   depending on how much you trust in rust, you might want to do a
   ./scripts/clean-core.sh before building

2. $ ./scripts/ndk-make.sh

this will take some time - meanwhile we're doing some housekeeping:

3. update translations and local help:
   $ ./scripts/tx-pull-translations.sh
   $ ./scripts/create-local-help.sh

4. a) update CHANGELOG.md
      from https://github.com/deltachat/deltachat-core-rust/blob/master/CHANGELOG.md
      and https://github.com/deltachat/deltachat-android/pulls?q=is%3Apr+is%3Aclosed+sort%3Aupdated-desc
   b) add used core version to CHANGELOG.md
   c) add a device message to ConversationListActivity::onCreate()
      or remove the old one

in Android Studio:

5. bump version in build.gradle,
   update _both_, versionCode and versionName

6. Add "4" at the end of versionCode to calculate F-Droid version code number.
   E.g. for versionCode 456 you get number 4564.
   This version conversion is due to `VercodeOperation` in
   <https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.b44t.messenger.yml>
   metadata file, see <https://f-droid.org/docs/Build_Metadata_Reference/#VercodeOperation> for
   documentation.
   Add `metadata/en-US/changelogs/4564.txt` file with a changelog for F-Droid.

7. if `./scripts/ndk-make.sh` from step 2. is finished successfully:
   a) select "Build / Generate Signed Bundle or APK" and then "APK"
      (not: App Bundle as this would require uploading the signing key)
   b) select flavor `gplayRelease` with V1 signature enabled
      (needed for easy APK verification), V2 is optional
   c) if you want to use upload-beta.sh, generate a debug apk additionally at
      "Build / Build Bundle(s)/APK / Build APK(s)"

on success, the generated APK is at
`gplay/release/deltachat-gplay-release-VERSION.apk`
and can be uploading for testing using:
$ ./scripts/upload-beta.sh VERSION
The "Testing checklist" gives some hints about what should be always tested.


# Upload APK to get.delta.chat

7. $ ./scripts/upload-release.sh VERSION

8. bump `VERSION_ANDROID` (without leading `v`) on
   `https://github.com/deltachat/deltachat-pages/blob/master/_includes/download-boxes.html`


# Release on Play Store

on https://play.google.com/apps/publish/ :

9. a) open "Delta Chat/Release/Production"
      then "Create new release" and upload APK from above
   b) fill out "Release details/Release notes" (500 characters max),
      release name should be default ("123 (1.2.3)")
   c) click "Save" and then "Review release"
   d) set "Rollout Percentage" to 1% and then 2%, 5%, 10%, 20%, 50%, 100% the next days


# Release new F-Droid version

10. make sure, everything is pushed, then:
    $ git tag v1.2.1; git push --tags
    
F-Droid picks on the tags starting with "v" and builds the version.
This may take some days.


# Release new Amazon Appstore version

on https://developer.amazon.com/dashboard :

11. a) for "Delta Chat", select tab "Add upcoming version"
    b) at "App Information" hit "Edit" abottom and then "Replace APK" atop,
       upload the APK from above, "Save"
    c) on the same tab, add "Release notes" from CHANGELOG.md, "Save"
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

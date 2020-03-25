# Release checklist - generate APK

on the command-line:

1. update core rust submodule, if needed:
   $ ./tools/update-core.sh
   $ ./tools/clean-core.sh  # to be sure nothing gets mixed up

2. $ ./ndk-make.sh

this will take some time - meanwhile we're doing some housekeeping:

3. update translations and local help:
   $ ./tools/tx-pull-translations.sh
   $ cd ../deltachat-pages; ./tools/create-local-help.py; cd ../deltachat-android

4. update CHANGELOG.md
   (the core-changelog at
   https://github.com/deltachat/deltachat-core-rust/blob/master/CHANGELOG.md
   and the "N commits to master since last release" on
   https://github.com/deltachat/deltachat-android/releases gives some hints)

in Android Studio:

5. bump version in gradle.build,
   update _both_, versionCode and versionName

6. if `./ndk-make.sh` from step 2. is finished successfully:
   a) select "Build / Generate signed APK"
      (not: App Bundle as this would require uploading the signing key)
   b) select flavor `gplayRelease` with V1 signature enabled
      (needed for easy APK verification), V2 is optional

on success, the generated APK is at
`gplay/release/deltachat-gplay-release-VERSION.apk`


# Upload APK to get.delta.chat

7. $ cd gplay/release
   $ rsync deltachat-gplay-release-VERSION.apk jekyll@download.delta.chat:/var/www/html/download/android/
   (you need the private SSH key of the jekyll user; you can find it in this file:
   https://github.com/hpk42/otf-deltachat/blob/master/secrets/delta.chat
   It is protected with [git-crypt](https://www.agwa.name/projects/git-crypt/) -
   after installing it, you can decrypt it with `git crypt unlock`. 
   If your key isn't added to the secrets, you can ask on irc add you.
   Add the key to your `~/.ssh/config` for the host, or to your ssh-agent, so rsync is able to use it)

8. a) Test the APK yourself.
      The "Testing checklist" gives some hints.
   b) Give the APK to testing groups.


# Release on Play Store

on https://play.google.com/apps/publish/ :

9. a) open "Delta Chat/Release management/App releases/Open track/Manage"
      then "Create release/Browse files" and select APK from above
   b) fill out "What's new in this release?" (500 characters max)
   c) click "Save" and then "Review"
   d) rollout to 20% by default is fine


# Release new F-Droid version

10. make sure, everything is pushed, then:
    $ git tag v1.2.1; git push --tags
    
F-Droid picks on the tags starting with "v" and builds the version.
This may take some days.


# Release new Amazon Appstore version

on https://developer.amazon.com/dashboard :

11. upload the APK from above


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

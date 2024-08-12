# Android Release Checklist


## Generate APKs

on the command-line, in a PR called "update-core-and-stuff-DATE":

1. update core:
   ```
   ./scripts/update-core.sh               # shows used branch
   ./scripts/update-core.sh BRANCH_OR_TAG # update to tag or latest commit of branch
   ./scripts/clean-core.sh                # helps on weird issues, do also "Build / Clean"
   ./scripts/ndk-make.sh
   ```

2. update translations and local help:
   ```
   ./scripts/tx-pull-translations.sh
   ./scripts/create-local-help.sh  # requires deltachat-pages checked out at ../deltachat-pages
   ```

the "update-core-and-stuff-DATE" PR can be merged without review
(as everyrhing was already reviewed in their repos).

the following steps are done in a PR called `prep-VERSION` (no leading "v"):

3. update `CHANGELOG.md`
   from <https://github.com/deltachat/deltachat-core-rust/blob/main/CHANGELOG.md>
   and <https://github.com/deltachat/deltachat-android/pulls?q=is%3Apr+is%3Aclosed+sort%3Aupdated-desc>.
   avoid technical terms, library versions etc. the changelog is for the end user.
   do not forget to update/mention used core version and release month.  
   in case previous entries of the changelog refer to betas or to not officially released versions,
   the entries can be summarized.
   this makes it easier for the end user to follow changes by showing major changes atop

4. add a device message to `ConversationListActivity::onCreate()` or remove the old one.
   do not repeat the CHANGELOG here: write what really is the ux outcome
   in a few lines of easy speak without technical terms.
   if there is time for a translation round, do `./scripts/tx-push-source.sh`
   **ping tangible translators** and start over at step 2.

5. bump `versionCode` _and_ `versionName` (no leading "v") in `build.gradle`

6. build APKs:
   a) generate debug APK at "Build / Build Bundle(s)/APK / Build APK(s)"  
   b) generate release APK at "Build / Generate Signed Bundle or APK",
      select "APK", add keys, flavor `gplayRelease`


## Push Test Releases

7. a) `./scripts/upload-beta.sh VERSION` uploads both APKs to testrun.org and drafts a message.  
   b) add things critically to be tested to the message (this is not the changelog nor the device message)  
   c) post the message to relevant testing channels, **ping testers**  
   d) make sure, the `prep-VERSION` PR **gets merged**

On serious deteriorations, **ping devs**, make sure they get fixed, and start over at step 1.


## Release on get.delta.chat

Take care the APK used here and in the following steps
are binary-wise the same as pushed to testers and not overwritten by subsequent builds.

8. a) `./scripts/upload-release.sh VERSION`  
   b) do a PR to bump `VERSION_ANDROID` (without leading `v`) on
      `https://github.com/deltachat/deltachat-pages/blob/master/_includes/download-boxes.html`  
   c) make sure, **the PR gets merged**
      and the correct APK is finally available on get.delta.chat

only afterwards, push the APK to stores. **consider a blog post.**


## Release on Play Store

on <https://play.google.com/apps/publish/>:

9. a) open "Delta Chat/Release/Production"
      then "Create new release" and upload APK from above  
   b) fill out "Release details/Release notes" (500 chars), add the line
      "These features will roll out over the coming days. Thanks for using Delta Chat!";
      release name should be default ("123 (1.2.3)")  
   c) click "Next", set "Rollout Percentage" to 1% (later 2%, 5%, 10%, 20%, 50%, 100%),
      click "Save"
   d) Go to "Publishing Overview", "Managed publishing" is usually off;
      click "Send change for review", confirm


## Tag for F-Droid and create Github release

10. make sure, everything is pushed, then:  
    $ git tag v1.2.1 COMMIT; git push --tags
    
F-Droid picks on the tags starting with "v" and builds the version.
This may take some days.

11. a) on <https://github.com/deltachat/deltachat-android/releases>,
       tap "Draft a new Release", choose just created tag, fill changelog
	b) add APK from above using "Attach binary".
	c) tap "Publish release"


## Release on Amazon Appstore

on <https://developer.amazon.com/dashboard>:

12. a) for "Delta Chat", select "Add upcoming version" on the left
    b) at "Step 1 / Existing file(s)" hit "Replace", upload the APK from above
    c) on the "Step 1" page, add "Release notes" from CHANGELOG.md, hit "Next"
    d) on "Step 2" page: "Does your app collect or transfer user data to third parties?" -> No, then "Next"
    e) on "Step 3" page: "Next"
    f) on "Step 4" page: "Submit app"


## Release on Huawei AppGallery

on <https://developer.huawei.com/consumer/en/appgallery>:

13. a) go to "Upload your app / Android / Delta Chat / Update", again "Update" upper right
    b) "Manage Packages / Upload", upload the APK from above, hit "Save"
    c) Update "App Information / New Features", hit "Save", then "Next"
    d) Hit "Submit"; on the next page, confirm version and language


## Releases on Apklis, Passkoocheh

These stores are not under our control.
On important updates **ping store maintainers** and ask to update.


## Testing checklist

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


The following checklists are there not to forget things ... to be continued.


# Release new Core version

1. deltachat-core: bump version, commit, push
2. deltachat-core on Github: "Draft a new release" with the version form `v1.2.3`


# Release new F-Droid version

Release core, if needed, then:

2. deltachat-android/tools: `$ ./update-core.sh`, push
3. deltachat-android/tools: `$ ./txpull`, if there are changes: commit, push
4. deltachat-android: bump version, commit, push
5. deltachat-android on Github: "Draft a new release" with the version form `v1.2.3`
6. update the high-lebel changelog deltachat-pages/en/changelog.md

... some days later, F-Droid should be updated.

For an additional APK:

6. in Android Studio select "Build / Build APK"
7. rename the generated APK to `deltachat-v1.2.3.apk`
8. upload the APK to the created Github release


# Add language

1. add `MessengerProj/src/main/res/values-<LANG>/strings.xml`
2. add copy command to `tools/txpull.sh` and `tools/txpush.sh`


# Update source languges on Transifex

1. call `./txpull` to overwrite local translation files with the ones from Transifex, the source is not updated
2. modify source and translations as needed
3. call `./txpush` to push source and translations back to Transifex.

(the tx-tool uses on the _name-attribute_ to identify strings while the Transifex-UI uses the source _string_.
So, without pushing back, the translation of modified source would be lost.  
For the same reason, we do not use the "Auto update resource" function in the Transifex-UI:
A change to a source string in values/strings.xml would break all translations)

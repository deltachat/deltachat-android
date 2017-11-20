
The following checklists are there not to forget things ... to be continued.

# Add Release on F-Droid

1. deltachat-core: "draft a new release", if required
2. deltachat-android: check CHANGES.md against core and android commit list from last release
3. deltachat-android/tools: `$ ./update-core.sh` followed by a `git push`
4. deltachat-android/tools: `$ ./txpull` followed by a `git commit` and a `git push` if there are changes
5. deltachat-android: "Draft a new release" using a version number in for Form v1.2.3

... some days later, F-Droid should be updated.

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

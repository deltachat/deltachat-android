
The following checklists are there not to forget things ... to be continued.

# Add Release on F-Droid

1. deltachat-core: "draft a new release", if required
2. deltachat-android: check CHANGES.md against core and android commit list from last release
3. deltachat-android: `$ ./update-core.sh` followed by a `git push`
4. deltachat-android: "Draft a new release" using a version number in for Form v1.2.3

... some days later, F-Droid should be updated.

# Add language

1. add `MessengerProj/src/main/res/values-<LANG>/strings.xml`
2. add copy command to `tools/txpull.sh` and `tools/txpush.sh`

# Update source languges

1. modify `MessengerProj/src/main/res/values/strings.xml` as needed, commit them, but do not push them yet
2. call `./txpull` from tools-directory
3. `git push` (after the pull to avoid a source-pull from transifex removing the translations due to unknown source strings)
4. call `./txpush` from tools-directory


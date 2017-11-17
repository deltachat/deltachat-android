
The following checklists are there not to forget things ... to be continued.

# Add Release on F-Droid

1. deltachat-core: "draft a new release", if required
2. deltachat-android: check CHANGES.md against core and android commit list from last release
3. deltachat-android: `$ ./update-core.sh` followed by a `git push`
4. deltachat-android: "Draft a new release" using a version number in for Form v1.2.3

... some days later, F-Droid should be updated.

# Add language

- add `MessengerProj/src/main/res/values-<LANG>/strings.xml`
- add copy routine to `tools/txpull.sh` and `tools/txpush.sh`



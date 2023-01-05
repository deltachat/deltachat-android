# F-Droid - Overview

- <https://f-droid.org/en/packages/com.b44t.messenger/>
  is the Delta Chat page on F-Droid.org,
  the F-Droid-app will show similar information.

- <https://github.com/deltachat/deltachat-android/tree/master/metadata>
  contains the description, icon, screenshots and all meta data shown for Delta Chat on F-Droid
  in the [fastlane format](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/#fastlane-structure).

- <https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.b44t.messenger.yml> and
  <https://gitlab.com/fdroid/fdroiddata/-/tree/master/metadata/com.b44t.messenger>
  contain [additional F-Droid-specific metadata](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/#in-the-f-droid-repo)
  and build instructions that do not fit the fastlane format.
  F-Droid adds new versions automatically to the end of `.yml` file.

- new versions are recognized by tags in the form `v1.2.3` -
  before adding tags like that, have a look at `docs/release-checklist.md`
  the build and distribution is expected to take
  [up to 5 days](https://gitlab.com/fdroid/wiki/-/wikis/FAQ#how-long-does-it-take-for-my-app-to-show-up-on-website-and-client).


# F-Droid Build status

- <https://monitor.f-droid.org/builds>
  shows F-Droid's overall build status,
  if Delta Chat shows up at "Need updating" or "Running",
  things are working as expected :)

- <https://f-droid.org/repo/com.b44t.messenger_VERSIONCODE.apk>  
  (with VERSIONCODE = 537 or so) links to successfully built apk
  even if it is not yet in the index (which may take some more time).
  F-Droid keeps the last 3 successful builds in the main repo,
  while the rest will be moved to the Archive repo:
  <https://f-droid.org/archive/com.b44t.messenger_VERSIONCODE.apk>


# Use F-Droid-tools locally

$ git clone https://gitlab.com/fdroid/fdroiddata  
$ git clone https://gitlab.com/fdroid/fdroidserver  
$ cd fdroiddata  

now, metadata/com.b44t.messenger.yml can be modified.
for testing, one can change the repo to a branch
by adding the line `Update Check Mode:RepoManifest/BRANCH` to the file.

set some path to ndk etc:  
$ cp ../fdroidserver/examples/config.py .  # adapt file as needed

checkout repo as f-droid would do:  
$ ../fdroidserver/fdroid checkupdates -v com.b44t.messenger  
(for testing with uncommited changes, add --allow-dirty)

build repo as f-droid would do:  
$ ../froidserver/fdroid build -v com.b44t.messenger:<versionCode>

(via https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools/ 
and https://f-droid.org/docs/Building_Applications/ -
might require `pip install pyasn1 pyasn1_modules pyaml requests`)


# Changing the description

- Change the files `metadata/en-US/short_description.txt`
  and `metadata/en-US/full_description.txt`
  in <https://github.com/deltachat/deltachat-android/> repository.

- make sure there is a "newline" at the end of the description
  (see <https://gitlab.com/fdroid/fdroiddata/merge_requests/3580>)


# Changing F-Droid metadata

- the file `com.b44t.messenger.yml` can be changed via a PR to the <https://gitlab.com/fdroid/fdroiddata/> repository

- reformat the metadata using  
  $ ../fdroidserver/fdroid rewritemeta com.b44t.messenger  # called from fdroiddata dir
 

# F-Droid - Overview

- <https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.b44t.messenger.yml>
  contains the description and all meta data shown for Delta Chat on F-Droid;
  you can also check if F-Droid recognizes a new version here (they show up at the end)

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

now, metadata/com.b44t.messenger.txt can be modified.
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


# Changing the Description on F-Droid

- the description can be changed via a PR to the file above

- make sure there is a "newline" at the end of the description
  (see https://gitlab.com/fdroid/fdroiddata/merge_requests/3580)

- reformat the metadata using  
  $ ../fdroidserver/fdroid rewritemeta com.b44t.messenger  # called from fdroiddata dir
 

# F-Droid - Overview

- https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.b44t.messenger.txt
  contains the descripion and all meta data shown for Delta Chat on F-Droid

- when a new tag in the form `v1.2.3` is added to the deltachat-android repo,
  F-Droid will pick up this version and distribute it - 
  this may take some days or even weeks.
  
  before creating a new version, please have a look at docs/release-checklist.md


# Use F-Droid-tools locally

$ git clone https://gitlab.com/fdroid/fdroiddata
$ git clone https://gitlab.com/fdroid/fdroidserver
$ cd fdroiddata
$ ../fdroidserver/fdroid checkupdates -v com.b44t.messenger

(via https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools/ 
and https://f-droid.org/docs/Building_Applications/ -
might require `pip install pyasn1 pyasn1_modules pyaml`)


# Changing the Description on F-Droid

- the description can be changed via a PR to the file above

- make sure there is a "newline" at the end of the description
  (see https://gitlab.com/fdroid/fdroiddata/merge_requests/3580)

- reformat the metadata using
  $ ../fdroidserver/fdroid rewritemeta com.b44t.messenger  # called from fdroiddata dir
 

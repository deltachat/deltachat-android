# F-Droid - Overview

- <https://f-droid.org/en/packages/com.b44t.messenger/>
  is the Delta Chat page on F-Droid.org,
  the F-Droid app will show similar information.

- <https://github.com/deltachat/deltachat-android/tree/main/metadata>
  contains the description, icon, screenshots and all meta data shown for Delta Chat on F-Droid
  in the [fastlane format](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/#fastlane-structure).

- <https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/com.b44t.messenger.yml>
  contains [additional F-Droid-specific metadata](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/#in-the-f-droid-repo)
  and build instructions that do not fit the fastlane format.
  F-Droid adds new versions automatically to the end of `.yml` file.

- New versions are recognized by tags in the form `v1.2.3` -
  before adding tags like that, have a look at
  <https://github.com/deltachat/deltachat-android/blob/main/RELEASE.md#release-on-f-droid>.
  The build and distribution is expected to take
  [up to 7 days](https://gitlab.com/fdroid/wiki/-/wikis/FAQ#how-long-does-it-take-for-my-app-to-show-up-on-website-and-client).


# F-Droid Build status

- <https://monitor.f-droid.org/builds>
  shows F-Droid's overall build status,
  if Delta Chat shows up at "Need updating" or "Running",
  things are working as expected. :)

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

Now, metadata/com.b44t.messenger.yml can be modified.
For testing, one can change the repo to a branch
by adding the line `Update Check Mode:RepoManifest/BRANCH` to the file.

Set some path to ndk etc:  
$ cp ../fdroidserver/examples/config.py .  # adapt file as needed

Checkout repo as F-Droid would do:  
$ ../fdroidserver/fdroid checkupdates -v com.b44t.messenger  
(for testing with uncommitted changes, add --allow-dirty)

Build repo as F-Droid would do:  
$ ../froidserver/fdroid build -v com.b44t.messenger:<versionCode>

(via <https://f-droid.org/docs/Installing_the_Server_and_Repo_Tools/> 
and <https://f-droid.org/docs/Building_Applications/> -
might require `pip install pyasn1 pyasn1_modules pyaml requests`)


# Changing the description

- Change the files `metadata/en-US/short_description.txt`
  and `metadata/en-US/full_description.txt`
  in <https://github.com/deltachat/deltachat-android/> repository.

- Make sure there is a "newline" at the end of the description
  (see <https://gitlab.com/fdroid/fdroiddata/merge_requests/3580>).


# Changing F-Droid metadata

- The file `com.b44t.messenger.yml` can be changed via a PR to the <https://gitlab.com/fdroid/fdroiddata/> repository.

- Reformat the metadata using  
  $ ../fdroidserver/fdroid rewritemeta com.b44t.messenger  # called from fdroiddata dir
 

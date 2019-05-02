
# this script updates the deltachat-core sub-repository from github.
# must be executed from the repo root.
#
# - make sure, the deltachat-android directory is clean
# - make sure, deltachat-core is committed successfull before calling this script
# - the script assumes, deltachat-core is placed in the same directory as deltachat-android
#
# to simplify core-development, files in the submodule folder are replaced by 
# symbolic links to ../deltachat-core afterwards

# remove links to the files
rm -r jni/messenger-backend/*
rm -r jni/messenger-backend/.??*

# check out submodules as present in the repository
git submodule update --init --recursive

# update submodule
cd jni/messenger-backend
git checkout master
git pull
cd ../..

# commit changes
git add jni/messenger-backend/
git commit -m "Update messenger-backend submodule."

# remove files downloaded just  for committing
rm -r jni/messenger-backend/*
rm -r jni/messenger-backend/.??*

# re-link all files (symbolic links may contain arbitrary text, so no relative paths, please)
cd ..
ln --symbolic `pwd`/deltachat-core/src     deltachat-android/jni/messenger-backend/src
ln --symbolic `pwd`/deltachat-core/libs    deltachat-android/jni/messenger-backend/libs
cd deltachat-android


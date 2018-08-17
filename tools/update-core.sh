
# this script updates the deltachat-core sub-repository from github.
#
# - make sure, the deltachat-android directory is clean
# - make sure, deltachat-core is committed successfull before calling this script
# - the script assumes, deltachat-core is placed in the same directory as deltachat-android
#
# to simplify core-development, files in the submodule folder are replaced by 
# symbolic links to ../deltachat-core afterwards

# get to main directory
cd ..

# remove links to the files
rm -r jni/messenger-backend/*
rm -r jni/messenger-backend/.??*

# check out submodules as present in the repository
git submodule update --init --recursive

# update submodule
cd jni/messenger-backend
git checkout master
git pull

# commit changes
cd ../..
git commit -am "Update messenger-backend submodule."

# remove files downloaded just  for committing
rm -r jni/messenger-backend/*
rm -r jni/messenger-backend/.??*

# re-link all files (symbolic links may contain arbitrary text, so no relative paths, please)
cd ..
ln --symbolic `pwd`/deltachat-core/src     deltachat-android-ii/jni/messenger-backend/src
ln --symbolic `pwd`/deltachat-core/libs    deltachat-android-ii/jni/messenger-backend/libs
ln --symbolic `pwd`/deltachat-core/cmdline deltachat-android-ii/jni/messenger-backend/cmdline
cd deltachat-android-ii

# back to tools directory
cd tools

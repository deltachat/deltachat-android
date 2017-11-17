
# this script updates the deltachat-core sub-repository from github
# - make sure, deltachat-core is committed successfull before calling this script
# - the script assumes, deltachat-core is placed in the same directory as deltachat-android

# get to main directory
cd ..

# remove links to the files
rm -r MessengerProj/jni/messenger-backend/*
rm -r MessengerProj/jni/messenger-backend/.??*

# check out submodules as present in the repository
git submodule update --init --recursive

# update submodule
cd MessengerProj/jni/messenger-backend
git checkout master
git pull

# commit changes
cd ../../..
git commit -am "Update messenger-backend submodule."

# remove files downloaded just  for committing
rm -r MessengerProj/jni/messenger-backend/*
rm -r MessengerProj/jni/messenger-backend/.??*

# re-link all files (symbolic links may contain arbitrary text, so no relative paths, please)
cd ..
ln --symbolic `pwd`/deltachat-core/src     deltachat-android/MessengerProj/jni/messenger-backend/src
ln --symbolic `pwd`/deltachat-core/libs    deltachat-android/MessengerProj/jni/messenger-backend/libs
ln --symbolic `pwd`/deltachat-core/cmdline deltachat-android/MessengerProj/jni/messenger-backend/cmdline
cd deltachat-android

# back to tools directory
cd tools

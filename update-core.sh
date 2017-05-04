
# this script updates the messenger-backend subfile from github;
# make sure, messenger-backend is committed successfull before calling this script

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

# re-link all files
rm -r MessengerProj/jni/messenger-backend/*
rm -r MessengerProj/jni/messenger-backend/.??*
mkdir MessengerProj/jni/messenger-backend/src
cp -al ../deltachat-core/src MessengerProj/jni/messenger-backend/ 
mkdir MessengerProj/jni/messenger-backend/libs
cp -al ../deltachat-core/libs MessengerProj/jni/messenger-backend/ 

